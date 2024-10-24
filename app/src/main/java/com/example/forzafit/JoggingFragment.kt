package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class JoggingFragment : Fragment() {

    private lateinit var distanceTextView: TextView
    private lateinit var finishButton: Button
    private var taskId: String? = null
    private var distance: Int = 0

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jogging, container, false)

        // Initialize views
        distanceTextView = view.findViewById(R.id.distanceTextView)
        finishButton = view.findViewById(R.id.btnFinishJogging)

        // Retrieve the task ID from arguments
        taskId = arguments?.getString("taskId")

        // Load the task details based on the task ID
        loadTaskDetails()

        // Handle finish button click
        finishButton.setOnClickListener {
            if (distance > 0) {
                updateXPAndCompleteTask(distance)
            } else {
                Toast.makeText(context, "No distance found to complete", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadTaskDetails() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            taskId?.let { id ->
                // Fetch the task from Firestore using the task ID
                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val value = document.getString("value")?.toIntOrNull() ?: 0
                            distance = value
                            distanceTextView.text = "Distance: $distance km"
                        } else {
                            Toast.makeText(context, "Task not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to load task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateXPAndCompleteTask(dist: Int) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Get the current user data from Firestore
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentXP = document.getLong("xp")?.toInt() ?: 0
                        val currentLevel = document.getLong("level")?.toInt() ?: 1
                        val newXP = currentXP + dist

                        // Calculate level up and remaining XP
                        var updatedXP = newXP
                        var updatedLevel = currentLevel

                        while (updatedXP >= 100) {
                            updatedXP -= 100
                            updatedLevel += 1
                        }

                        // Update XP, level, and mark task as complete
                        db.collection("users").document(userId)
                            .update(mapOf("xp" to updatedXP, "level" to updatedLevel))
                            .addOnSuccessListener {
                                markTaskAsComplete()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to update XP", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun markTaskAsComplete() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            taskId?.let { id ->
                // Get current time in "dd/MM/yyyy HH:mm:ss" format
                val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                // Update the task status to 'complete' and add the finished time
                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .update(mapOf(
                        "status" to "complete",
                        "finished_time" to currentTime
                    ))
                    .addOnSuccessListener {
                        Toast.makeText(context, "Task marked as complete", Toast.LENGTH_SHORT).show()
                        navigateToHomeFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToHomeFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }
}
