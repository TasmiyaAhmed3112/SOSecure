package com.sos.womensafetyapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.Model
import com.sos.womensafetyapp.copyAssetFolder
import org.vosk.android.RecognitionListener
import java.io.File
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import androidx.core.app.ActivityCompat
import android.telephony.SmsManager



class VoiceForegroundService : Service() {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var isVoiceRunning = false

    private val channelId = "voice_service_channel"
    private lateinit var dbHelper: DatabaseHelper

    private var isSOSActive = false
    private var lastSOSTime = 0L
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        Log.d("VOICE_SERVICE", "Service Created")
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbHelper = DatabaseHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, createNotification())

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val voiceEnabled = prefs.getBoolean("voice_enabled", false)

        Log.d("VOICE_PREF", "Service check: $voiceEnabled")

        if (voiceEnabled) {
            if (!isVoiceRunning) {
                initVosk()
                isVoiceRunning = true
            }
        } else {
            stopVoiceDetection()
            isVoiceRunning = false
        }

        return START_STICKY
    }

    private fun stopVoiceDetection() {
        try {

            speechService?.stop()
            speechService?.shutdown()
            speechService = null

            recognizer = null

            isVoiceRunning = false

            Log.d("VOICE_SERVICE", "Vosk stopped successfully")

        } catch (e: Exception) {
            Log.e("VOICE_SERVICE", "Error stopping Vosk: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, VoiceForegroundService::class.java)

        val restartServicePendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            restartServicePendingIntent
        )

        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Voice Background Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SOS Listening Active")
            .setContentText("Background voice service running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun initVosk() {
        Thread {

            try {
                val modelPath = File(filesDir, "vosk-model")

                // Copy model only once if not exists
                if (!modelPath.exists()) {
                    copyAssetFolder(
                        this@VoiceForegroundService,
                        "vosk-model-small-en-us-0.15",
                        modelPath.absolutePath
                    )
                }

                model = Model(modelPath.absolutePath)

                if (model != null) {
                    startListening()
                }

            } catch (e: Exception) {
                Log.e("VOICE_SERVICE", "Vosk init failed: ${e.message}")
            }

        }.start()
    }

    // ---------------- LISTENING ----------------

    private fun startListening() {

        val sampleRate = 16000.0f

        recognizer = Recognizer(model, sampleRate)
        speechService = SpeechService(recognizer, sampleRate)

        speechService?.startListening(object : RecognitionListener {

            override fun onPartialResult(hypothesis: String?) {
                Log.d("VOSK_PARTIAL", hypothesis ?: "null")
            }

            override fun onResult(hypothesis: String?) {

                Log.d("VOSK_RESULT", hypothesis ?: "null")

                if (hypothesis != null) {
                    val text = hypothesis.lowercase()

                    if (text.contains("help") || text.contains("sos")) {
                        Log.d("SOS_DETECTED", "Emergency keyword detected!")

                        triggerSOS()
                    }
                }
            }

            override fun onFinalResult(hypothesis: String?) {}
            override fun onError(e: Exception?) {
                Log.e("VOSK_ERROR", e?.message ?: "error")

                //restart listening
                speechService?.stop()
                startListening()
            }

            override fun onTimeout() {}
        })
    }

    // ---------------- SOS TRIGGER ----------------

    private fun triggerSOS() {

        val currentTime = System.currentTimeMillis()

        // 🛑 Block repeated triggers
        if (isSOSActive && (currentTime - lastSOSTime < 60000)) {
            Log.d("SOS_FLOW", "SOS ignored (cooldown active)")
            return
        }

        isSOSActive = true
        lastSOSTime = currentTime

        Log.d("SOS_FLOW", "SOS TRIGGERED")


        triggerSOSInternal()
    }

    private fun getLocation() {

        val request = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation

                if (location != null) {

                    val lat = location.latitude
                    val lon = location.longitude

                    Log.d("SOS_LOCATION", "Lat=$lat Lon=$lon")

                    fusedLocationClient.removeLocationUpdates(this)

                    sendSOSMessage(lat, lon)
                }
            }
        }


        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SOS_ERROR", "Location permission missing")
            return
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            mainLooper
        )
    }

    private fun sendSOSMessage(lat: Double, lon: Double) {

        val contacts = getContacts()

        if (contacts.isEmpty()) {
            Log.e("SOS_SMS", "No contacts found")
            return
        }

        val message =
            "SOS ALERT! I need help.\n" +
                    "Location: https://www.google.com/maps/search/?api=1&query=$lat,$lon"

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SOS_SMS", "SMS permission missing")
            return
        }

        val smsManager = SmsManager.getDefault()

        for (number in contacts) {
            smsManager.sendTextMessage(number, null, message, null, null)
        }

        Log.d("SOS_SMS", "SOS SMS sent")
    }

    private fun getContacts(): List<String> {

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

    private fun triggerSOSInternal() {
        Log.d("SOS_FLOW", "Internal SOS placeholder called")
        getLocation()

        // We will add location + SMS here in next step
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            recognizer = null
            isVoiceRunning = false

            Log.d("VOICE_SERVICE", "Service fully stopped")

        } catch (e: Exception) {
            Log.e("VOICE_SERVICE", "Error stopping service: ${e.message}")
        }
    }
}
