package com.example.forzafit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.forzafit.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        arguments?.let {
            binding.editFirstName.setText(it.getString("firstName", ""))
            binding.editLastName.setText(it.getString("lastName", ""))
            binding.editAge.setText(it.getString("age", ""))
            binding.editDescription.setText(it.getString("description", ""))
        }

        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.btnSaveChanges.setOnClickListener {
            saveProfileChanges()
        }
        binding.imgAddPictureIcon.setOnClickListener {
            openImagePicker()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            binding.imgEditProfilePicture.setImageURI(imageUri)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfileChanges() {
        val firstName = binding.editFirstName.text.toString()
        val lastName = binding.editLastName.text.toString()
        val age = binding.editAge.text.toString()
        val description = binding.editDescription.text.toString()

        val userId = auth.currentUser?.uid ?: return
        if (imageUri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            updateUserDataInFirestore(userId, firstName, lastName, age, description, null)
            return
        }
        uploadImageToStorage(userId, firstName, lastName, age, description)
    }


    private fun uploadImageToStorage(
        userId: String,
        firstName: String,
        lastName: String,
        age: String,
        description: String
    ) {
        val fileName = "profile_images/$userId/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        imageUri?.let {
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updateUserDataInFirestore(userId, firstName, lastName, age, description, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun updateUserDataInFirestore(
        userId: String,
        firstName: String,
        lastName: String,
        age: String,
        description: String,
        imageUrl: String?
    ) {
        val updatedUserData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "age" to age,
            "description" to description
        )

        if (imageUrl != null) {
            updatedUserData["imageUrl"] = imageUrl
        }

        firestore.collection("users").document(userId)
            .update(updatedUserData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
