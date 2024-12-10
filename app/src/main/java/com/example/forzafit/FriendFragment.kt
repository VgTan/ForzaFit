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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendFragment : Fragment() {

    private lateinit var friendListView: ListView
    private lateinit var searchFriend: EditText
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var friendList: ArrayList<String>
    private val db = FirebaseFirestore.getInstance()

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
        friendList = arrayListOf()

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, friendList)
        friendListView.adapter = adapter

        // Load friends from Firestore
        loadFriendsFromFirestore()

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

    private fun loadFriendsFromFirestore() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Please log in to view friends.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil data teman dari Firestore
        db.collection("users")
            .document(currentUserId)
            .collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                friendList.clear() // Bersihkan list sebelum mengisi data baru
                for (document in documents) {
                    val username = document.getString("username")
                    username?.let { friendList.add(it) }
                }
                adapter.notifyDataSetChanged() // Update adapter dengan data baru
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load friends: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
