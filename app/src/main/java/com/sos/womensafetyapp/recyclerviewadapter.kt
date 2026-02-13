package com.sos.womensafetyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class ContactsAdapter(private val contactsList: List<Contact>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {
    var onItemLongClick: (Contact) -> Unit = {}


    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.etContactName)
        val number: TextView = itemView.findViewById(R.id.etContactNumber)
        val image: ImageView = itemView.findViewById(R.id.imgContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false) // your display CardView layout
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactsList[position]
        holder.name.text = contact.name
        holder.number.text = contact.phone
        
        holder.image.setImageResource(R.drawable.id_contact)
        holder.itemView.setOnLongClickListener {
            onItemLongClick(contact)
            true
        }
    }

    override fun getItemCount(): Int = contactsList.size
}

private fun ContactsAdapter.onItemLongClick(contact: Contact) {}
