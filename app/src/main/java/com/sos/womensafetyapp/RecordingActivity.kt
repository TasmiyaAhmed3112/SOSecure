package com.sos.womensafetyapp


import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class RecordingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // connecting our recordings with recycler view
        val recyclerView= findViewById<RecyclerView>(R.id.contactRecyclerView)
        recyclerView.layoutManager= LinearLayoutManager(this)
        adapter = RecordingAdapter(recordingList)
        recyclerView.adapter = adapter

        val directory = filesDir
        val allFiles = directory.listFiles()

        if (allFiles != null) {
            for (file in allFiles) {
                if (file.isFile &&
                    file.name.startsWith("recording_") &&
                    file.name.endsWith(".mp4")
                ) {
                    recordingList.add(file)
                }
            }
        }

        recordingList.sortByDescending { it.lastModified() }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // go back to MainActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
