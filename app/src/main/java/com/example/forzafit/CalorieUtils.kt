package com.example.forzafit.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object CalorieUtils {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Calculates calories burned based on the activity and metrics.
     * Handles different reset intervals for today, this week, and last 3 months.
     */
    fun saveCaloriesToFirebase(activity: String, metric: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val weight = document.getLong("weight")?.toInt() ?: 70 // Use 70 kg as fallback

                    // Current time and last updated times
                    val currentTime = System.currentTimeMillis()
                    val lastUpdatedToday = document.getLong("lastUpdatedToday") ?: 0L
                    val lastUpdatedWeek = document.getLong("lastUpdatedWeek") ?: 0L
                    val lastUpdated3Months = document.getLong("lastUpdated3Months") ?: 0L

                    // Calculate calories burned for this activity
                    val caloriesToday = calculateCalories(activity, metric, weight)
                    val caloriesThisWeek = calculateCalories(activity, metric, weight)
                    val caloriesLast3Months = calculateCalories(activity, metric, weight)

                    // Reset and update calories if periods have passed
                    val updatedCaloriesToday = if (currentTime - lastUpdatedToday < 24 * 60 * 60 * 1000) {
                        (document.getDouble("caloriesToday") ?: 0.0) + caloriesToday
                    } else {
                        caloriesToday // Reset to current activity's calories
                    }

                    val updatedCaloriesThisWeek = if (currentTime - lastUpdatedWeek < 7 * 24 * 60 * 60 * 1000) {
                        (document.getDouble("caloriesThisWeek") ?: 0.0) + caloriesThisWeek
                    } else {
                        caloriesThisWeek // Reset to current activity's calories
                    }

                    val updatedCaloriesLast3Months = if (currentTime - lastUpdated3Months < 3 * 30.44 * 24 * 60 * 60 * 1000) {
                        (document.getDouble("caloriesLast3Months") ?: 0.0) + caloriesLast3Months
                    } else {
                        caloriesLast3Months // Reset to current activity's calories
                    }

                    // Update Firestore with new calorie values
                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "caloriesToday" to updatedCaloriesToday,
                                "caloriesThisWeek" to updatedCaloriesThisWeek,
                                "caloriesLast3Months" to updatedCaloriesLast3Months,
                                "lastUpdatedToday" to currentTime,
                                "lastUpdatedWeek" to currentTime,
                                "lastUpdated3Months" to currentTime
                            )
                        )
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Calorie calculation logic for different activities.
     */
    private fun calculateCalories(activity: String, metric: Int, weight: Int): Double {
        return when (activity) {
            "jogging" -> {
                val distanceInKm = metric / 1000.0 // Convert meters to km
                val durationInHours = distanceInKm / 8.0 // Assuming 8 km/h speed
                8.3 * weight * durationInHours
            }
            "push_up" -> 0.29 * weight * metric
            "pull_up" -> 0.5 * weight * metric
            "sit_up" -> 0.15 * weight * metric
            else -> 0.0
        }
    }
}
