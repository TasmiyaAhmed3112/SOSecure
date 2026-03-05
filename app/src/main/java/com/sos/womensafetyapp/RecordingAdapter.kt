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
    }

    override fun getItemCount(): Int {
        return recordings.size
    }
}


