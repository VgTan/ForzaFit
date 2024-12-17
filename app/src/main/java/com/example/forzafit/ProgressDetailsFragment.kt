package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.forzafit.databinding.FragmentProgressDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ProgressDetailsFragment : Fragment() {

    private var _binding: FragmentProgressDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProgressDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.imgBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Fetch and update progress data
        fetchAndResetProgress("Today", 24 * 60 * 60 * 1000L) // 1 day
        fetchAndResetProgress("ThisWeek", 7 * 24 * 60 * 60 * 1000L) // 1 week
        fetchAndResetProgress("Last3Months", (3 * 30.44 * 24 * 60 * 60 * 1000L).toLong()) // ~3 months
//        fetchAndResetProgress("Today", 10 * 1000L)       // 10 seconds instead of 1 day
//        fetchAndResetProgress("ThisWeek", 30 * 1000L)    // 30 seconds instead of 1 week
//        fetchAndResetProgress("Last3Months", 60 * 1000L) // 60 seconds instead of 3 months

    }

    private fun fetchAndResetProgress(period: String, resetInterval: Long) {
        val userId = auth.currentUser?.uid ?: return
        val currentTime = System.currentTimeMillis()

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Fetch the last updated time
                    val lastUpdatedKey = "lastUpdated$period"
                    val lastUpdated = document.getLong(lastUpdatedKey) ?: 0L

                    // Check if reset interval has passed
                    val shouldReset = currentTime - lastUpdated > resetInterval

                    // Fetch progress counts
                    val pushUps = if (shouldReset) 0 else document.getLong("pushUps$period")?.toInt() ?: 0
                    val sitUps = if (shouldReset) 0 else document.getLong("sitUps$period")?.toInt() ?: 0
                    val pullUps = if (shouldReset) 0 else document.getLong("pullUps$period")?.toInt() ?: 0
                    val jogging = if (shouldReset) 0 else document.getLong("jogging$period")?.toInt() ?: 0 // In km

                    // Calculate calories
                    val calories = calculateCalories(pushUps, sitUps, pullUps, jogging)

                    if (shouldReset) {
                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "pushUps$period" to 0,
                                    "sitUps$period" to 0,
                                    "pullUps$period" to 0,
                                    "jogging$period" to 0,
                                    "lastUpdated$period" to currentTime
                                )
                            )
                    }

                    // Update UI
                    when (period) {
                        "Today" -> {
                            binding.txtCaloriesCountToday.text = "%.2f kkal".format(calories)
                            binding.txtPushUpCountToday.text = "x$pushUps"
                            binding.txtSitUpCountToday.text = "x$sitUps"
                            binding.txtPullUpCountToday.text = "x$pullUps"
                            binding.txtJoggingCountToday.text = "${jogging}km"
                        }
                        "ThisWeek" -> {
                            binding.txtCaloriesCountWeek.text = "%.2f kkal".format(calories)
                            binding.txtPushUpCountWeek.text = "x$pushUps"
                            binding.txtSitUpCountWeek.text = "x$sitUps"
                            binding.txtPullUpCountWeek.text = "x$pullUps"
                            binding.txtJoggingCountWeek.text = "${jogging}km"
                        }
                        "Last3Months" -> {
                            binding.txtCaloriesCountMonths.text = "%.2f kkal".format(calories)
                            binding.txtPushUpCountMonths.text = "x$pushUps"
                            binding.txtSitUpCountMonths.text = "x$sitUps"
                            binding.txtPullUpCountMonths.text = "x$pullUps"
                            binding.txtJoggingCountMonths.text = "${jogging}km"
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a toast)
            }
    }

    private fun calculateCalories(pushUps: Int, sitUps: Int, pullUps: Int, jogging: Int): Double {
        // Define calorie burn rates
        val pushUpRate = 0.3 // kcal per repetition
        val sitUpRate = 0.4 // kcal per repetition
        val pullUpRate = 1.0 // kcal per repetition
        val joggingRate = 100.0 // kcal per kilometer

        // Calculate total calories
        return (pushUps * pushUpRate) +
                (sitUps * sitUpRate) +
                (pullUps * pullUpRate) +
                (jogging * joggingRate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
