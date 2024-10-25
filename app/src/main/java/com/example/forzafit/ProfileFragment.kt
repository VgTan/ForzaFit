package com.example.forzafit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.forzafit.databinding.FragmentProfileBinding
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
                    val birthDate = document.getString("birthDate") ?: ""
                    val height = document.getString("height")?.toDoubleOrNull() ?: 0.0
                    val weight = document.getString("weight")?.toDoubleOrNull() ?: 0.0
                    val description = document.getString("description") ?: ""
                    val profileImageUrl = document.getString("imageUrl")
                    val coverImageUrl = document.getString("coverImageUrl")
                    val bmiLastCalculated = document.getLong("bmiLastCalculated") ?: 0L

                    val age = calculateAge(birthDate)
                    val bmi = calculateBMI(height, weight)

                    binding.txtUserName.text = "$firstName $lastName"
                    binding.txtUserAge.text = "Age: $age"
                    binding.txtBMI.text = "BMI: %.1f".format(bmi)
                    binding.txtDescription.text = description
                    if (!profileImageUrl.isNullOrEmpty()) {
                        binding.imgProfile.tag = profileImageUrl
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .error(R.drawable.profile_placeholder)
                            .into(binding.imgProfile)
                    }
                    binding.imgProfile.apply {
                        clipToOutline = true
                        outlineProvider = ViewOutlineProvider.BACKGROUND
                    }

                    // Load updated cover image
                    if (!coverImageUrl.isNullOrEmpty()) {
                        binding.imgCover.tag = coverImageUrl
                        Glide.with(this)
                            .load(coverImageUrl)
                            .placeholder(R.drawable.cover_placeholder)
                            .error(R.drawable.cover_placeholder)
                            .into(binding.imgCover)
                    }
                    displayLastCalculatedBMI(bmiLastCalculated)
                }
            }
            .addOnFailureListener { e ->
                // Handle the error here
            }
    }

    private fun displayLastCalculatedBMI(bmiLastCalculated: Long) {
        if (bmiLastCalculated == 0L) {
            binding.txtLastCalculated.text = "Last calculated: N/A"
            return
        }

        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - bmiLastCalculated

        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMillis / (1000 * 60 * 60)
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)

        val lastCalculatedText = when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes > 1) "s" else ""} ago"
            diffHours < 24 -> "$diffHours hour${if (diffHours > 1) "s" else ""} ago"
            else -> "$diffDays day${if (diffDays > 1) "s" else ""} ago"
        }

        binding.txtLastCalculated.text = "Last calculated: $lastCalculatedText"
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