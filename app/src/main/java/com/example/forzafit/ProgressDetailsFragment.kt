package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.forzafit.databinding.FragmentProgressDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        // Fetch progress data from Firestore to display
        fetchProgressData("Today")
        fetchProgressData("ThisWeek")
        fetchProgressData("Last3Months")
    }

    private fun fetchProgressData(period: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Fetch progress values
                    val pushUps = document.getLong("pushUps$period")?.toInt() ?: 0
                    val sitUps = document.getLong("sitUps$period")?.toInt() ?: 0
                    val pullUps = document.getLong("pullUps$period")?.toInt() ?: 0
                    val jogging = document.getLong("jogging$period")?.toInt() ?: 0

                    val calories = calculateCalories(pushUps, sitUps, pullUps, jogging)

                    // Update UI based on the period
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
                Toast.makeText(context, "Failed to load progress data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateCalories(pushUps: Int, sitUps: Int, pullUps: Int, jogging: Int): Double {
        val pushUpRate = 0.3 // kcal per repetition
        val sitUpRate = 0.4 // kcal per repetition
        val pullUpRate = 1.0 // kcal per repetition
        val joggingRate = 70.0 // kcal per kilometer

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
