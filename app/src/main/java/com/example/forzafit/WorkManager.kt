package com.example.forzafit

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class ProgressResetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            resetProgressIfNeeded(userId, currentTime, "Today", 24 * 60 * 60 * 1000L)       // 10 seconds
            resetProgressIfNeeded(userId, currentTime, "ThisWeek", 7 * 24 * 60 * 60 * 1000L)   // 30 seconds
            resetProgressIfNeeded(userId, currentTime, "Last3Months", (3 * 30.44 * 24 * 60 * 60 * 1000L).toLong())
//            resetProgressIfNeeded(userId, currentTime, "Today", 10 * 1000L)       // 10 seconds
//            resetProgressIfNeeded(userId, currentTime, "ThisWeek", 30 * 1000L)   // 30 seconds
//            resetProgressIfNeeded(userId, currentTime, "Last3Months", 60 * 1000L) // 60 seconds
        }

        // Re-enqueue the worker to run again after a delay
        scheduleNextRun()
        return Result.success()
    }

    private fun resetProgressIfNeeded(userId: String, currentTime: Long, period: String, interval: Long) {
        val resetKey = "reset$period"

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val resetTimestamp = document.getLong(resetKey) ?: 0L
                val shouldReset = currentTime - resetTimestamp > interval

                if (shouldReset) {
                    val updates = mutableMapOf<String, Any>(
                        resetKey to currentTime,
                        "pushUps$period" to 0,
                        "sitUps$period" to 0,
                        "pullUps$period" to 0,
                        "jogging$period" to 0
                    )

                    firestore.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d("ProgressResetWorker", "Progress reset for $period")
                        }
                        .addOnFailureListener {
                            Log.e("ProgressResetWorker", "Failed to reset progress for $period")
                        }
                }
            }
    }

    private fun scheduleNextRun() {
        val workRequest = OneTimeWorkRequestBuilder<ProgressResetWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS) // Adjust delay as needed
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}
