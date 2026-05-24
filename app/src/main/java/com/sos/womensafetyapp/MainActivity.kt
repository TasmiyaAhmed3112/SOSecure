package com.sos.womensafetyapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder?=null
    private var outputFilePath: String?=null
    private var isRecording=false
    lateinit var dbHelper: DatabaseHelper
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val sosbtn=findViewById<Button>(R.id.btnSOS)   // Initialize SOS button from layout
        sosbtn.setOnClickListener {
            triggerSOS()                                    // Set click listener for SOS button
                                                           // When pressed it triggers emergency action
        }

        dbHelper = DatabaseHelper(this)          // Initialized the DBhelper
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)

        val voicePrefs = getSharedPreferences("app_settings", MODE_PRIVATE)oi
        val voiceEnabled = voicePrefs.getBoolean("voice_enabled", false)
        Toast.makeText(this, "Voice Enabled: $voiceEnabled", Toast.LENGTH_SHORT).show()

        if (voiceEnabled) {
            startVoiceActivation()
        }




        fun startRecording() {
            outputFilePath =
                "${filesDir.absolutePath}/recording${System.currentTimeMillis()}.mp4"
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)

                prepare()
                start()
            }.also { mediaRecorder = it }
        }

            fun stopRecording(){
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder=null
                    Toast.makeText(this, "Recording saved at $outputFilePath", Toast.LENGTH_SHORT).show()
                }
                catch (e: Exception){
                    e.printStackTrace()
                    Toast.makeText(this, "Stop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }



        val micFab = findViewById<FloatingActionButton>(R.id.mic)
        micFab.setOnClickListener {

            val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            val isRecordingEnabled = prefs.getBoolean("recording_enabled", false)

            if (isRecordingEnabled) {
                if(!isRecording){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            101
                        )
                        return@setOnClickListener
                    }

                    startRecording()
                    isRecording=true
                    Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
                }
                else{
                    stopRecording()
                    isRecording=false
                    Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enable Recording in Settings", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets


        }
    }
    // Function to handle SOS trigger
    fun triggerSOS(){
        sendSMS()
    }
    // Function to fetch emergency contacts from DB
    fun getEmergencyContacts(): List<String> {
        val list = mutableListOf<String>()

        val cursor = dbHelper.getAllContacts()

        if (cursor.moveToFirst()) {
            do {
                val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                list.add(phone)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }
    //CheckingSMS feature
    fun sendSMS() {

        val contacts = getEmergencyContacts()

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show()
            return
        }

        val message = " SOS triggered! I need help. My location:" +
                " https://www.google.com/maps/search/?api=1&query=$latitude,$longitude".trimIndent()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                1
            )
            return
        }

        val smsManager = SmsManager.getDefault()

        for (number in contacts) {
            smsManager.sendTextMessage(number, null, message, null, null)
        }

        Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()
    }
    fun getLocationAndSendSOS() {

        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500

        }


        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation

                if (location != null) {


                    val lat = location.latitude
                    val lon = location.longitude
                    val accuracy=location.accuracy


                   android.util.Log.d("LOCATION", "Lat: $lat, Lon: $lon, Acc: $accuracy")


                    fusedLocationProviderClient.removeLocationUpdates(this)


                    if (accuracy<=100 ) {

                        fusedLocationProviderClient.removeLocationUpdates(this)

                        sendSMS(lat, lon)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                2
            )
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location!= null){
                val lat= location.latitude
                val longi=location.longitude

                android.util.Log.d("Location","Lat:$lat ,Lon:$longi")
                sendSMS(lat, longi)
            }
            else{
                Toast.makeText(this, "Location not availaible", Toast.LENGTH_SHORT).show()
            }

        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.contacts-> {
                val intent = Intent(this, ContactsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.rec -> {
                val intent = Intent(this, RecordingActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun startVoiceActivation() {

        Toast.makeText(this, "Voice Started", Toast.LENGTH_SHORT).show()
        Log.d("VOICE", "startVoiceActivation called")

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {

                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                matches?.forEach { text ->
                    val input = text.lowercase()

                    if (input.contains("help") || input.contains("emergency")) {
                        triggerSOS()
                    }
                }

                speechRecognizer.startListening(speechIntent)
            }

            override fun onError(error: Int) {
                speechRecognizer.startListening(speechIntent)
            }

            override fun onEndOfSpeech() {
                speechRecognizer.startListening(speechIntent)
            }

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE", "READY FOR SPEECH")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(speechIntent)
    }


}