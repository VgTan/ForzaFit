package com.example.forzafit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment

class FriendFragment : Fragment() {

    private lateinit var friendListView: ListView
    private lateinit var searchFriend: EditText
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var friendList: ArrayList<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_friend, container, false)

        // Handle Back Button Click
        val imgBackButton: ImageView = rootView.findViewById(R.id.imgBackButton)
        imgBackButton.setOnClickListener {
            parentFragmentManager.popBackStack() // Navigasi ke fragment sebelumnya
        }

        // Initialize views
        searchFriend = rootView.findViewById(R.id.searchFriend)
        friendListView = rootView.findViewById(R.id.friendListView)

        // Sample friend list
        friendList = arrayListOf("Aldo", "Beldo", "Celdo", "Ildo", "Eldo", "Feldo")

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, friendList)
        friendListView.adapter = adapter

        // Handle friend selection
        friendListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedFriend = friendList[position]
            Toast.makeText(requireContext(), "View Profile: $selectedFriend", Toast.LENGTH_SHORT).show()
        }

        // Search functionality
        searchFriend.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return rootView
    }
}
