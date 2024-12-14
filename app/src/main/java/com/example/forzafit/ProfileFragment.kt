package com.example.forzafit

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.forzafit.databinding.FragmentProfileBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        fetchUserData()
        (activity as MainActivity).ShowNavtools()

        binding.imgEditProfile.setOnClickListener {
            navigateToEditProfileFragment()
        }

        binding.linearProgressThisWeek.setOnClickListener {
            navigateToProgressDetailsFragment()
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val joggingThisWeek = document.getLong("joggingThisWeek")?.toInt() ?: 0
                    val pushUpsThisWeek = document.getLong("pushUpsThisWeek")?.toInt() ?: 0
                    val pullUpsThisWeek = document.getLong("pullUpsThisWeek")?.toInt() ?: 0
                    val sitUpsThisWeek = document.getLong("sitUpsThisWeek")?.toInt() ?: 0
                    val lastUpdated = document.getLong("lastUpdated") ?: 0L
                    val currentTime = System.currentTimeMillis()
                    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

                    // Reset weekly progress if 7 days have passed
                    if (currentTime - lastUpdated > sevenDaysInMillis) {
                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "joggingThisWeek" to 0,
                                    "pushUpsThisWeek" to 0,
                                    "pullUpsThisWeek" to 0,
                                    "sitUpsThisWeek" to 0,
                                    "lastUpdated" to currentTime
                                )
                            )
                            .addOnSuccessListener {
                                binding.txtJogging.text = "Jogging  0 km"
                                binding.txtPushUp.text = "Push Up  x0"
                                binding.txtPullUp.text = "Pull Up x0"
                                binding.txtSitUp.text = "Sit Up x0"
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to reset weekly progress", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        binding.txtJogging.text = "Jogging  $joggingThisWeek km"
                        binding.txtPushUp.text = "Push Up  x$pushUpsThisWeek"
                        binding.txtPullUp.text = "Pull Up  x$pullUpsThisWeek"
                        binding.txtSitUp.text = "Sit Up  x$pullUpsThisWeek"
                    }

                    val birthDate = document.getString("birthDate") ?: ""

                    // Parse height, weight, and targetWeight as Double, even if saved as String
                    val height = document.get("height")?.toString()?.toDoubleOrNull() ?: 0.0
                    val weight = document.get("weight")?.toString()?.toDoubleOrNull() ?: 0.0
                    val targetWeight = document.get("targetWeight")?.toString()?.toDoubleOrNull() ?: 0.0
                    val startDate = document.getLong("startDate") ?: System.currentTimeMillis()

                    val weeklyWeightsMap =
                        (document.get("weeklyWeight") as? Map<String, Any>) ?: initializeWeeklyWeights(userId, weight)
                    val weeklyWeights = (1..4).map { week ->
                        (weeklyWeightsMap["week$week"] as? Number)?.toDouble() ?: 0.0
                    }

                    val age = calculateAge(birthDate)
                    val bmi = calculateBMI(height, weight)

                    binding.txtUserName.text = "$firstName $lastName"
                    binding.txtUserAge.text = "Age: $age"
                    binding.txtBMI.text = "BMI: %.1f".format(bmi)

                    if (targetWeight > 0) {
                        setupGraph(weeklyWeights, weight, targetWeight)
                    }

//                    Handler(Looper.getMainLooper()).postDelayed({
//                        updateWeeklyWeightIfNecessary(userId, startDate, weight, weeklyWeightsMap)
//                    }, 60000)

                    if (currentTime - startDate > sevenDaysInMillis) {
                        updateWeeklyWeightIfNecessary(userId, startDate, weight, weeklyWeightsMap)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeWeeklyWeights(userId: String, startingWeight: Double): Map<String, Any> {
        val defaultWeights = mapOf(
            "week1" to startingWeight,
            "week2" to 0.0,
            "week3" to 0.0,
            "week4" to 0.0
        )
        firestore.collection("users").document(userId)
            .update("weeklyWeight", defaultWeights)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Weekly weights initialized: $defaultWeights")
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to initialize weekly weights: ${e.message}")
            }
        return defaultWeights
    }

    private fun setupGraph(weeklyWeights: List<Double>, currentWeight: Double, targetWeight: Double) {
        if (weeklyWeights.isEmpty()) {
            Log.e("ProfileFragment", "Graph data is empty. Skipping graph setup.")
            return
        }

        val entries = ArrayList<Entry>()
        weeklyWeights.forEachIndexed { index, weight ->
            if (weight >= 0) { // Ensure valid weight
                entries.add(Entry((index + 1).toFloat(), weight.toFloat()))
            }
        }

        if (entries.isEmpty()) {
            Log.e("ProfileFragment", "No valid graph entries found.")
            return
        }

        val lineDataSet = LineDataSet(entries, "Weight Progress")
        lineDataSet.color = Color.BLUE
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.lineWidth = 2f
        lineDataSet.setCircleColor(Color.RED)
        lineDataSet.circleRadius = 5f

        val lineData = LineData(lineDataSet)

        binding.lineChart.data = lineData
        binding.lineChart.description.text = "4-Week Weight Progress"
        binding.lineChart.axisLeft.axisMinimum = (currentWeight - 10).toFloat().coerceAtLeast(0f)
        binding.lineChart.axisLeft.axisMaximum = (targetWeight + 10).toFloat()
        binding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.lineChart.xAxis.granularity = 1f
        binding.lineChart.xAxis.axisMinimum = 1f
        binding.lineChart.xAxis.axisMaximum = 4f
        binding.lineChart.invalidate()
    }

    private fun updateWeeklyWeightIfNecessary(
        userId: String,
        startDate: Long,
        currentWeight: Double,
        weeklyWeightsMap: Map<String, Any>
    ) {
        val currentWeek = getCurrentWeek(startDate)
        val nextWeek = getNextIncompleteWeek(weeklyWeightsMap)

        if (nextWeek != null) { // If there's a week with incomplete data
            val updatedWeights = weeklyWeightsMap.toMutableMap()
            showWeightInputDialog(userId, nextWeek, updatedWeights, currentWeight)
        } else {
            resetWeeklyWeights(userId, currentWeight)
        }
    }

    private fun resetWeeklyWeights(userId: String, currentWeight: Double) {
        val resetWeights = mapOf(
            "week1" to currentWeight, // Keep current weight for week 1
            "week2" to 0.0,
            "week3" to 0.0,
            "week4" to 0.0
        )

        firestore.collection("users").document(userId)
            .update("weeklyWeight", resetWeights)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Weekly weights reset. Starting from week 1.")
                // Start from week 1 again
                updateWeeklyWeightIfNecessary(userId, System.currentTimeMillis(), currentWeight, resetWeights)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to reset weekly weights: ${e.message}")
                Toast.makeText(context, "Failed to reset weekly weights. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun getNextIncompleteWeek(weeklyWeightsMap: Map<String, Any>): String? {
        for (i in 1..4) {
            val weekKey = "week$i"
            val weight = weeklyWeightsMap[weekKey] as? Double ?: 0.0
            if (weight <= 0.0) { // If the weight for this week is not filled
                return weekKey
            }
        }
        return null // All weeks are filled
    }


    private fun showWeightInputDialog(
        userId: String,
        week: String,
        updatedWeights: MutableMap<String, Any>,
        currentWeight: Double
    ) {
        val weightInputDialog = android.app.AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        weightInputDialog.setTitle("Update Weight")
        weightInputDialog.setMessage("Please enter your weight for $week:")
        weightInputDialog.setView(input)

        weightInputDialog.setPositiveButton("Save") { _, _ ->
            val newWeight = input.text.toString().toDoubleOrNull()
            if (newWeight != null && newWeight > 0) {
                if (newWeight != currentWeight) {
                    // Update weekly weight
                    updatedWeights[week] = newWeight
                    firestore.collection("users").document(userId)
                        .update("weeklyWeight", updatedWeights)
                        .addOnSuccessListener {
                            // Update BMI after successful weight update
                            updateBMI(userId, newWeight)
                            Log.d("ProfileFragment", "Weight updated for $week: $newWeight")
                            Toast.makeText(context, "Weight and BMI updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileFragment", "Failed to update weight: ${e.message}")
                            Toast.makeText(context, "Failed to update weight. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Weight is the same as previous. No changes made.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Invalid weight input. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        weightInputDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        weightInputDialog.show()
    }

    private fun updateBMI(userId: String, newWeight: Double) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val height = document.getString("height")?.toDoubleOrNull() ?: 0.0
                    if (height > 0) {
                        val newBMI = calculateBMI(height, newWeight)
                        val updates = mapOf(
                            "BMI" to "%.2f".format(newBMI),
                            "weight" to newWeight // Update the weight in Firestore as well
                        )
                        firestore.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                // Safely update UI only if fragment is attached
                                if (isAdded && _binding != null) {
                                    binding.txtBMI.text = "BMI: %.2f".format(newBMI)
                                    Log.d("ProfileFragment", "BMI and weight updated: BMI = $newBMI, Weight = $newWeight")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileFragment", "Failed to update BMI: ${e.message}")
                            }
                    } else {
                        Log.e("ProfileFragment", "Height not found for BMI calculation.")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to fetch user data for BMI update: ${e.message}")
            }
    }


    private fun getCurrentWeek(startDate: Long): String {
        val millisInWeek = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val weeksSinceStart = ((currentTime - startDate) / millisInWeek).toInt()
        return "week${(weeksSinceStart % 4) + 1}"
    }

    private fun getNextWeek(currentWeek: String): String {
        val weekNumber = currentWeek.last().toString().toInt()
        return "week${(weekNumber % 4) + 1}"
    }

    private fun calculateAge(birthDate: String): Int {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(birthDate) ?: return 0
            val birthCalendar = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return age
        } catch (e: Exception) {
            return 0
        }
    }

    private fun calculateBMI(height: Double, weight: Double): Double {
        return if (height > 0) weight / ((height / 100) * (height / 100)) else 0.0
    }


    private fun navigateToEditProfileFragment() {
        val editProfileFragment = EditProfileFragment()
        val bundle = Bundle()

        val nameParts = binding.txtUserName.text.toString().split(" ")
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""
        val profileImageUrl = binding.imgProfile.tag?.toString()
        val coverImageUrl = binding.imgCover.tag?.toString()

        bundle.putString("firstName", firstName)
        bundle.putString("lastName", lastName)
        bundle.putString("age", binding.txtUserAge.text.toString().replace("Age: ", ""))
        bundle.putString("description", binding.txtDescription.text.toString())
        bundle.putString("profileImageUrl", profileImageUrl)
        bundle.putString("coverImageUrl", coverImageUrl)

        editProfileFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, editProfileFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToProgressDetailsFragment() {
        val progressDetailsFragment = ProgressDetailsFragment()

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, progressDetailsFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}