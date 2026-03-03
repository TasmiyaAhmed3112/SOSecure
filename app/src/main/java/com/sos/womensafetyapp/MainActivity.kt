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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)



        fun startRecording() {
            outputFilePath =
                "${cacheDir.absolutePath}/recording${System.currentTimeMillis()}.mp4"
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


}