package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        fetchThisWeekData()
        fetchTodayData()
    }

    private fun fetchTodayData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Fetch data and timestamp
                    val pushUpsToday = document.getLong("pushUpsToday")?.toInt() ?: 0
                    val sitUpsToday = document.getLong("sitUpsToday")?.toInt() ?: 0
                    val joggingToday = document.getLong("joggingToday")?.toInt() ?: 0
                    val caloriesToday = document.getDouble("caloriesToday") ?: 0.0
                    val lastUpdatedToday = document.getLong("lastUpdatedToday") ?: 0L

                    // Check if the data is still within 24 hours
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdatedToday < 24 * 60 * 60 * 1000) {
                        // Data is within 24 hours - populate fields
                        binding.txtPushUpCountToday.text = "x$pushUpsToday"
                        binding.txtSitUpCountToday.text = "x$sitUpsToday"
                        binding.txtJoggingCountToday.text = "${joggingToday}km"
                        binding.txtCaloriesCountToday.text = "%.2f kkal".format(caloriesToday)
                    } else {
                        // Data is older than 24 hours - reset progress
                        resetTodayProgress(userId)
                    }
                }
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a toast)
            }
    }

    private fun resetTodayProgress(userId: String) {
        val updates = mapOf(
            "pushUpsToday" to 0,
            "sitUpsToday" to 0,
            "joggingToday" to 0,
            "caloriesToday" to 0.0,
            "lastUpdatedToday" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                // Clear "Today" section fields
                binding.txtPushUpCountToday.text = "x0"
                binding.txtSitUpCountToday.text = "x0"
                binding.txtJoggingCountToday.text = "0km"
                binding.txtCaloriesCountToday.text = "0.00 kkal"
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a toast)
            }
    }


    private fun fetchThisWeekData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Fetch data and timestamp
                    val pushUpsThisWeek = document.getLong("pushUpsThisWeek")?.toInt() ?: 0
                    val sitUpsThisWeek = document.getLong("sitUpsThisWeek")?.toInt() ?: 0
                    val joggingThisWeek = document.getLong("joggingThisWeek")?.toInt() ?: 0
                    val caloriesThisWeek = document.getDouble("caloriesThisWeek") ?: 0.0
                    val lastUpdatedWeek = document.getLong("lastUpdatedWeek") ?: 0L

                    // Check if the data is still within the week (7 days)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdatedWeek < 7 * 24 * 60 * 60 * 1000) {
                        // Data is within 7 days - populate fields
                        binding.txtPushUpCountWeek.text = "x$pushUpsThisWeek"
                        binding.txtSitUpCountWeek.text = "x$sitUpsThisWeek"
                        binding.txtJoggingCountWeek.text = "${joggingThisWeek}km"
                        binding.txtCaloriesCountWeek.text = "%.2f kkal".format(caloriesThisWeek)
                    } else {
                        // Data is older than 7 days - reset progress
                        resetThisWeekProgress(userId)
                    }
                }
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a toast)
            }
    }

    private fun resetThisWeekProgress(userId: String) {
        val updates = mapOf(
            "pushUpsThisWeek" to 0,
            "sitUpsThisWeek" to 0,
            "joggingThisWeek" to 0,
            "caloriesThisWeek" to 0.0,
            "lastUpdatedWeek" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                // Clear "This Week" section fields
                binding.txtPushUpCountWeek.text = "x0"
                binding.txtSitUpCountWeek.text = "x0"
                binding.txtJoggingCountWeek.text = "0km"
                binding.txtCaloriesCountWeek.text = "0.00 kkal"
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a toast)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
