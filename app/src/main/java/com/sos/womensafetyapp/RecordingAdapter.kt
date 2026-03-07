package com.sos.womensafetyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale






class RecordingAdapter(
    private val recordings: List<File>
) : RecyclerView.Adapter<RecordingAdapter.ViewHolder>(){
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val nameText= itemView.findViewById<TextInputEditText>(R.id.text_rec)
        val duration=itemView.findViewById<TextView>(R.id.Duration)
        val dateText=itemView.findViewById<TextView>(R.id.date)
        val playButton=itemView.findViewById<ImageView>(R.id.play_img)

 }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view) as ViewHolder

    }

    override fun onBindViewHolder(holder:ViewHolder, position: Int) {
        val file =recordings[position]

        val cleanName = file.nameWithoutExtension
        holder.nameText.setText(cleanName)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(file.lastModified()))
        holder.dateText.text = formattedDate

        holder.duration.text = "Audio file"
        var isPlaying=false

        holder.playButton.setOnClickListener {
                val filePath = file.absolutePath // get file path
             // check if any recording is playing

                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {

                    mediaPlayer?.release()

                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(file.absolutePath)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()

                    isPlaying = true
                }



        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording?")
                .setPositiveButton("Delete") { _, _ ->

                    // deletion logic
                    val file = recordings[position]

                    file.delete()

                    recordings.removeAt(position)

                    notifyItemRemoved(position)

                }
                .setNegativeButton("Cancel", null)
                .show()
            true


        }
    }

    override fun getItemCount(): Int {
        return recordings.size
    }
}


