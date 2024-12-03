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

class PushUpFragment : Fragment() {

    private lateinit var repetitionTextView: TextView
    private lateinit var finishButton: Button
    private var taskId: String? = null
    private var repetitions: Int = 0

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_push_up, container, false)

        repetitionTextView = view.findViewById(R.id.repetitionTextView)
        finishButton = view.findViewById(R.id.btnFinishPushUp)

        taskId = arguments?.getString("taskId")
        loadTaskDetails()

        finishButton.setOnClickListener {
            if (repetitions > 0) {
                updateXPAndCompleteTask(repetitions)
            } else {
                Toast.makeText(context, "No repetitions found to complete", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadTaskDetails() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            taskId?.let { id ->
                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            repetitions = document.getString("value")?.toIntOrNull() ?: 0
                            repetitionTextView.text = "Repetitions: $repetitions times"
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

    private fun updateXPAndCompleteTask(reps: Int) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Current XP and Level
                        val currentXP = document.getLong("xp")?.toInt() ?: 0
                        val currentLevel = document.getLong("level")?.toInt() ?: 1
                        val newXP = currentXP + reps

                        var updatedXP = newXP
                        var updatedLevel = currentLevel

                        // Calculate level up
                        while (updatedXP >= 100) {
                            updatedXP -= 100
                            updatedLevel += 1
                        }

                        // Current progress for "Today"
                        val currentPushUpsToday = document.getLong("pushUpsToday")?.toInt() ?: 0
                        val lastUpdatedToday = document.getLong("lastUpdatedToday") ?: 0L
                        val currentTime = System.currentTimeMillis()

                        // Check if 24 hours have passed since the last update
                        val updatedPushUpsToday = if (currentTime - lastUpdatedToday < 24 * 60 * 60 * 1000) {
                            currentPushUpsToday + reps
                        } else {
                            reps // Reset progress if 24 hours have passed
                        }

                        // Update Firestore
                        db.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "xp" to updatedXP,
                                    "level" to updatedLevel,
                                    "pushUpsThisWeek" to (document.getLong("pushUpsThisWeek")?.toInt()
                                        ?: 0) + reps,
                                    "pushUpsToday" to updatedPushUpsToday,
                                    "lastUpdatedToday" to currentTime,
                                    "lastUpdated" to currentTime
                                )
                            )
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
                val currentTime =
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .update(mapOf("status" to "complete", "finished_time" to currentTime))
                    .addOnSuccessListener {
                        Toast.makeText(context, "Task marked as complete", Toast.LENGTH_SHORT).show()
                        navigateToProfileFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToProfileFragment() {
        parentFragmentManager.popBackStack()
    }
}
