package com.example.forzafit

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.forzafit.databinding.FragmentProfileBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.FirebaseFirestore
import com.example.forzafit.databinding.FragmentFriendProfileBinding

class FriendProfileFragment : DialogFragment() {

    private var _binding: FragmentFriendProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var friendUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        friendUsername = arguments?.getString("friendUsername")
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendProfileBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, // Match the parent width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Wrap content for height
            )
            setBackgroundDrawableResource(android.R.color.transparent) // Transparent background
        }

        fetchFriendData()
        hideEditOptions()
    }


    private fun fetchFriendData() {
        if (friendUsername.isNullOrEmpty()) {
            Toast.makeText(context, "Friend's profile not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Fetch friend's data
        firestore.collection("users")
            .whereEqualTo("userName", friendUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    // Load basic info
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val description = document.getString("description") ?: "No description available"
                    val imageUrl = document.getString("imageUrl")
                    val coverImageUrl = document.getString("coverImageUrl")
                    val height = document.get("height")?.toString()?.toDoubleOrNull() ?: 0.0
                    val weight = document.get("weight")?.toString()?.toDoubleOrNull() ?: 0.0
                    val targetWeight = document.get("targetWeight")?.toString()?.toDoubleOrNull() ?: 0.0
                    val weeklyWeightsMap = document.get("weeklyWeight") as? Map<String, Any> ?: emptyMap()

                    val joggingThisWeek = document.getLong("joggingThisWeek")?.toInt() ?: 0
                    val pushUpsThisWeek = document.getLong("pushUpsThisWeek")?.toInt() ?: 0
                    val pullUpsThisWeek = document.getLong("pullUpsThisWeek")?.toInt() ?: 0
                    val sitUpsThisWeek = document.getLong("sitUpsThisWeek")?.toInt() ?: 0

                    val bmi = calculateBMI(height, weight)

                    // Update UI
                    binding.txtUserName.text = "$firstName $lastName"
                    binding.txtBMI.text = "BMI: %.1f".format(bmi)
                    binding.txtDescription.text = description
                    binding.txtJogging.text = "Jogging  ${joggingThisWeek} km"
                    binding.txtPushUp.text = "Push Up  x${pushUpsThisWeek}"
                    binding.txtPullUp.text = "Pull Up  x${pullUpsThisWeek}"
                    binding.txtSitUp.text = "Sit Up  x${sitUpsThisWeek}"

                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .into(binding.imgProfile)

                    Glide.with(this)
                        .load(coverImageUrl)
                        .placeholder(R.drawable.cover_placeholder)
                        .into(binding.imgCover)

                    // Setup graph for weight progress
                    setupGraph(weeklyWeightsMap, weight, targetWeight)
                } else {
                    Toast.makeText(context, "Friend's profile not found", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load friend's profile", Toast.LENGTH_SHORT).show()
                dismiss()
            }
    }

    private fun hideEditOptions() {
        binding.linearProgressThisWeek.isClickable = false
    }

    private fun calculateBMI(height: Double, weight: Double): Double {
        return if (height > 0) weight / ((height / 100) * (height / 100)) else 0.0
    }

    private fun setupGraph(weeklyWeights: Map<String, Any>, currentWeight: Double, targetWeight: Double) {
        val entries = ArrayList<Entry>()

        // Extract weight data for weeks 1 to 4
        for (i in 1..4) {
            val weekKey = "week$i"
            val weight = (weeklyWeights[weekKey] as? Number)?.toDouble() ?: 0.0
            if (weight > 0) {
                entries.add(Entry(i.toFloat(), weight.toFloat()))
            }
        }

        if (entries.isEmpty()) return

        val lineDataSet = LineDataSet(entries, "Weight Progress")
        lineDataSet.color = Color.BLUE
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.lineWidth = 2f
        lineDataSet.setCircleColor(Color.RED)
        lineDataSet.circleRadius = 5f

        val lineData = LineData(lineDataSet)

        binding.lineChart.data = lineData
        binding.lineChart.description.text = "Friend's 4-Week Weight Progress"
        binding.lineChart.axisLeft.axisMinimum = (currentWeight - 10).toFloat().coerceAtLeast(0f)
        binding.lineChart.axisLeft.axisMaximum = (targetWeight + 10).toFloat()
        binding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.lineChart.xAxis.granularity = 1f
        binding.lineChart.xAxis.axisMinimum = 1f
        binding.lineChart.xAxis.axisMaximum = 4f
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
