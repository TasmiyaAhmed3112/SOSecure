package com.sos.womensafetyapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

import android.media.MediaRecorder
import android.os.Bundle

import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.gms.location.LocationRequest





class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    private var isRecording = false
    lateinit var dbHelper: DatabaseHelper
    //private var model: Model? = null

    //private var recognizer: Recognizer? = null
    //private var speechService: SpeechService? = null

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val sosbtn = findViewById<Button>(R.id.btnSOS)   // Initialize SOS button from layout
        sosbtn.setOnClickListener {
            triggerSOS()                                    // Set click listener for SOS button
            // When pressed it triggers emergency action
        }

        dbHelper = DatabaseHelper(this)          // Initialized the DBhelper
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val intent = Intent(this, VoiceForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)

        //implementing vosk model foe offline wake word detection
       /* val modelPath = "${filesDir.absolutePath}/vosk-model"

        copyAssetFolder(
            this,
            "vosk-model-small-en-us-0.15",
            modelPath
        )*/


//copyModelAndInit()



        fun startRecording() {
            outputFilePath =
                "${filesDir.absolutePath}/recording_${System.currentTimeMillis()}.mp4"
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)


                prepare()
                start()
            }.also { mediaRecorder = it }
        }

        fun stopRecording() {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                Toast.makeText(this, "Recording saved at $outputFilePath", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Stop failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }


        val micFab = findViewById<FloatingActionButton>(R.id.mic)
        micFab.setOnClickListener {

            val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            val isRecordingEnabled = prefs.getBoolean("recording_enabled", false)

            if (isRecordingEnabled) {
                if (!isRecording) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {

                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            101
                        )
                        return@setOnClickListener
                    }

                    startRecording()
                    isRecording = true
                    Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
                } else {
                    stopRecording()
                    isRecording = false
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
    fun triggerSOS() {
        getLocationAndSendSOS()
        /*val intent = Intent(this, VoiceForegroundService::class.java)
        intent.action = "TRIGGER_SOS"
        ContextCompat.startForegroundService(this, intent)*/
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
    fun sendSMS(latitude: Double, longitude: Double) {

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
                    val accuracy = location.accuracy


                    android.util.Log.d("LOCATION", "Lat: $lat, Lon: $lon, Acc: $accuracy")


                    fusedLocationProviderClient.removeLocationUpdates(this)


                    if (accuracy <= 100) {

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

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.contacts -> {
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


    /*private fun copyModelAndInit() {
        Thread {
            val modelPath = "${filesDir.absolutePath}/vosk-model"

            copyAssetFolder(
                this,
                "vosk-model-small-en-us-0.15",
                modelPath
            )

            model = Model(modelPath)

            runOnUiThread {
                Toast.makeText(this, "Vosk Ready - Listening started", Toast.LENGTH_SHORT).show()
                startListening()
            }

        }.start()
    }

    private fun startListening() {

        val sampleRate = 16000.0f

        recognizer = Recognizer(model, sampleRate)

        speechService = SpeechService(recognizer, sampleRate)

        speechService?.startListening(object : RecognitionListener {

            override fun onPartialResult(hypothesis: String?) {
                // optional live text
            }

            override fun onResult(hypothesis: String?) {
                if (hypothesis != null) {
                    if (hypothesis.contains("help", true) ||
                        hypothesis.contains("sos", true)
                    ) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "SOS DETECTED!", Toast.LENGTH_LONG).show()

                        }
                    }
                }
            }

            override fun onFinalResult(hypothesis: String?) {}
            override fun onError(e: Exception?) {}
            override fun onTimeout() {}
        })
    }*/


}