package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.forzafit.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).ShowNavtools()
        binding.txtUserName.text = "Ari Maulana Hadijaya"
        binding.txtUserAge.text = "Age: 20"
        binding.txtBMI.text = "BMI: 19.6"
        binding.txtDescription.text = "Hello there! I'm new in this app!"

        // Edit Profile button functionality
        binding.imgEditProfile.setOnClickListener {
            // Navigate to EditProfileFragment
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.linearProgressThisWeek.setOnClickListener {
            // Navigate to ProgressDetailsFragment
            findNavController().navigate(R.id.action_profileFragment_to_progressDetailsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
