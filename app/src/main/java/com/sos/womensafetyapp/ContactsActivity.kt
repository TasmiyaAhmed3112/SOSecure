package com.sos.womensafetyapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton




class ContactsActivity : AppCompatActivity() {
    lateinit var dbHelper: DatabaseHelper
    lateinit var recyclerView: RecyclerView
    lateinit var contactsList: ArrayList<Contact>
    lateinit var adapter: ContactsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contacts)
         dbHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        contactsList = ArrayList()
        adapter = ContactsAdapter(contactsList)
        adapter.onItemLongClick = { contact ->
            showDeleteDialog(contact)
        }

        recyclerView.adapter = adapter

        loadContacts()




        val fab = findViewById<FloatingActionButton>(R.id.addContactFab)

        fab.setOnClickListener {
            showAddContactDialog()

        }

        val toolbar = findViewById<Toolbar>(R.id.contactsToolbar)
        setSupportActionBar(toolbar)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_add_contact, null)

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    dbHelper.insertContact(name, phone)
                    Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()

                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadContacts() {
        contactsList.clear()
        val cursor = dbHelper.getAllContacts()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                contactsList.add(Contact(id, name, phone))
            } while (cursor.moveToNext())
        }

        cursor.close()   // ✅ VERY IMPORTANT
        adapter.notifyDataSetChanged()
    }



    private fun showDeleteDialog(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteContact(contact.id)
                loadContacts()
                Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()  // go back to MainActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

}



