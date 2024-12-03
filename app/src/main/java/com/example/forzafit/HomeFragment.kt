package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var imgBearAvatar: ImageView
    private lateinit var btnLevelUp: Button
    private lateinit var btnToDo: Button
    private lateinit var progressExp: ProgressBar
    private lateinit var tvExp: TextView
    private lateinit var tvLevel: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        (activity as MainActivity).ShowBottomNav()
        (activity as MainActivity).HideNavtools()
        // Initialize views
        tvWelcome = view.findViewById(R.id.tvWelcome)
        imgBearAvatar = view.findViewById(R.id.imgBearAvatar)
        btnLevelUp = view.findViewById(R.id.btnLevelUp)
        btnToDo = view.findViewById(R.id.btnToDo) // Initialize the new button
        progressExp = view.findViewById(R.id.progressExp)
        tvExp = view.findViewById(R.id.tvExp)
        tvLevel = view.findViewById(R.id.tvLevel)

        // Fetch user data from Firestore
        fetchUserData()

        // Set onClick for 'Go Level Up' button
        btnLevelUp.setOnClickListener {
            // Navigate to AddTaskFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTaskFragment())
                .addToBackStack(null)
                .commit()
        }

        // Set onClick for 'Go To To-Do' button
        btnToDo.setOnClickListener {
            // Navigate to ToDoFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ToDoFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Fetch user data from Firestore
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: "[Name]"
                        val lastName = document.getString("lastName") ?: "[Name]"
                        val userName = document.getString("userName") ?: "[Name]"
                        val xp = document.getLong("xp")?.toInt() ?: 0
                        val level = document.getLong("level")?.toInt() ?: 1

                        // Calculate remaining XP to next level
                        val xpToNextLevel = 100 - xp

                        // Display the user's name, XP left to next level, and level
                        tvWelcome.text = "Welcome, $userName"
                        tvLevel.text = "Level $level"
                        tvExp.text = "$xpToNextLevel XP to next level"

                        // Set ProgressBar progress
                        progressExp.progress = xp
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            // Handle the case when the user is not logged in
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
