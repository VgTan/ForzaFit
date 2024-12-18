package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SitUpFragment : Fragment() {

    private lateinit var repetitionTextView: TextView
    private lateinit var finishButton: Button
    private lateinit var progressBar: ProgressBar
    private var taskId: String? = null
    private var repetitions: Int = 0

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sit_up, container, false)

        repetitionTextView = view.findViewById(R.id.repetitionTextView)
        finishButton = view.findViewById(R.id.btnFinishSitUp)
        progressBar = view.findViewById(R.id.progressBar)

        taskId = arguments?.getString("taskId")
        loadTaskDetails()

        finishButton.setOnClickListener {
            if (repetitions > 0) {
                progressBar.visibility = View.VISIBLE
                finishButton.isEnabled = false
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
                progressBar.visibility = View.VISIBLE
                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        progressBar.visibility = View.GONE
                        if (document.exists()) {
                            repetitions = document.getString("value")?.toIntOrNull() ?: 0
                            repetitionTextView.text = "Repetitions: $repetitions times"
                        } else {
                            Toast.makeText(context, "Task not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Failed to load task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateXPAndCompleteTask(reps: Int) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val currentTime = System.currentTimeMillis()

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

                        val currentSitUpsToday = document.getLong("sitUpsToday")?.toInt() ?: 0
                        val currentSitUpsThisWeek = document.getLong("sitUpsThisWeek")?.toInt() ?: 0
                        val currentSitUpsLast3Months = document.getLong("sitUpsLast3Months")?.toInt() ?: 0

                        val lastUpdatedToday = document.getLong("lastUpdatedToday") ?: 0L
                        val lastUpdatedWeek = document.getLong("lastUpdatedWeek") ?: 0L
                        val lastUpdated3Months = document.getLong("lastUpdated3Months") ?: 0L

                        val resetToday = currentTime - lastUpdatedToday > 24 * 60 * 60 * 1000
                        val resetWeek = currentTime - lastUpdatedWeek > 7 * 24 * 60 * 60 * 1000
                        val reset3Months = currentTime - lastUpdated3Months > (3 * 30.44 * 24 * 60 * 60 * 1000).toLong()

                        val updatedSitUpsToday = if (resetToday) reps else currentSitUpsToday + reps
                        val updatedSitUpsThisWeek = if (resetWeek) reps else currentSitUpsThisWeek + reps
                        val updatedSitUpsLast3Months = if (reset3Months) reps else currentSitUpsLast3Months + reps

                        val updates = mutableMapOf<String, Any>(
                            "xp" to updatedXP,
                            "level" to updatedLevel,
                            "sitUpsToday" to updatedSitUpsToday,
                            "sitUpsThisWeek" to updatedSitUpsThisWeek,
                            "sitUpsLast3Months" to updatedSitUpsLast3Months,
                            "lastUpdatedToday" to if (resetToday) currentTime else lastUpdatedToday,
                            "lastUpdatedWeek" to if (resetWeek) currentTime else lastUpdatedWeek,
                            "lastUpdated3Months" to if (reset3Months) currentTime else lastUpdated3Months
                        )

                        db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                markTaskAsComplete()
                            }
                            .addOnFailureListener {
                                progressBar.visibility = View.GONE
                                finishButton.isEnabled = true
                                Toast.makeText(context, "Failed to update XP and progress", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    finishButton.isEnabled = true
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
                        progressBar.visibility = View.GONE
                        finishButton.isEnabled = true
                        Toast.makeText(context, "Task marked as complete", Toast.LENGTH_SHORT).show()
                        navigateToProfileFragment()
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        finishButton.isEnabled = true
                        Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToProfileFragment() {
        parentFragmentManager.popBackStack()
    }
}
