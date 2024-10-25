package com.example.forzafit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FriendActivity : AppCompatActivity() {

    private lateinit var friendListView: ListView
    private lateinit var searchFriend: EditText
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var friendList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)

        searchFriend = findViewById(R.id.searchFriend)
        friendListView = findViewById(R.id.friendListView)

        // Sample friend list
        friendList = arrayListOf("Aldo", "Beldo", "Celdo", "Ildo", "Eldo", "Feldo")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, friendList)
        friendListView.adapter = adapter

        // Handle friend selection
        friendListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedFriend = friendList[position]
            Toast.makeText(this@FriendActivity, "View Profile: $selectedFriend", Toast.LENGTH_SHORT).show()
        }

        // Search functionality
        searchFriend.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
