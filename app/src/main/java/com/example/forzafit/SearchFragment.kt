package com.example.forzafit

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var adapter: UserAdapter
    private val filteredUsernames = mutableListOf<String>()
    val filteredImageUrls = mutableListOf<String?>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        recyclerView.visibility = View.GONE

        adapter = UserAdapter(filteredUsernames.toTypedArray(), filteredImageUrls.toTypedArray()) { username, imageUrl ->
            showAddFriendPopup(username, imageUrl)
        }
        recyclerView.adapter = adapter

        searchEditText = view.findViewById(R.id.search_friend)
        searchEditText.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableRight = searchEditText.compoundDrawables[2]
                if (drawableRight != null && event.rawX >= (searchEditText.right - drawableRight.bounds.width())) {
                    val query = searchEditText.text.toString().trim()
                    if (query.isNotEmpty()) {
                        performSearch(query)
                    } else {
                        Toast.makeText(context, "Please enter a username to search.", Toast.LENGTH_SHORT).show()
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }


        val buttonFriend: ImageButton = view.findViewById(R.id.button_friend)
        buttonFriend.setOnClickListener {
            val fragmentManager = parentFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, FriendFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }

    private fun performSearch(query: String) {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                filteredUsernames.clear()
                filteredImageUrls.clear()

                for (document in result) {
                    val username = document.getString("userName")
                    val imageUrl = document.getString("imageUrl")

                    if (username != null && username.contains(query, ignoreCase = true)) {
                        filteredUsernames.add(username)
                        filteredImageUrls.add(imageUrl)
                    }
                }

                adapter.updateData(filteredUsernames.toTypedArray(), filteredImageUrls.toTypedArray())

                recyclerView.visibility = if (filteredUsernames.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddFriendPopup(username: String, imageUrl: String?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_add_friend, null)

        val profileImageView: ImageView = dialogView.findViewById(R.id.profile_image)
        val usernameTextView: TextView = dialogView.findViewById(R.id.popup_username)
        val addFriendButton: Button = dialogView.findViewById(R.id.btn_add_friend)
        val closeButton: Button = dialogView.findViewById(R.id.btn_close)

        usernameTextView.text = username

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.profile)
            .error(R.drawable.profile)
            .into(profileImageView)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        addFriendButton.setOnClickListener {
            addFriendButton.isEnabled = false
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Toast.makeText(requireContext(), "Please log in to add a friend.", Toast.LENGTH_SHORT).show()
                addFriendButton.isEnabled = true
                return@setOnClickListener
            }
            val friendData = hashMapOf(
                "username" to username,
                "imageUrl" to imageUrl
            )

            db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .whereEqualTo("username", username) // Check if the friend already exists
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        db.collection("users")
                            .document(currentUserId)
                            .collection("friends")
                            .add(friendData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "$username added as a friend!", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(requireContext(), "Failed to add friend: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "$username is already your friend.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error checking friend: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

                .addOnCompleteListener {
                    addFriendButton.isEnabled = true
                }
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
