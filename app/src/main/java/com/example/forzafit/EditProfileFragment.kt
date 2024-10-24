package com.example.forzafit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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

    private var profileImageUri: Uri? = null
    private var coverImageUri: Uri? = null

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
        binding.imgEditProfilePicture.apply {
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND


        }

        arguments?.let {
            binding.editFirstName.setText(it.getString("firstName", ""))
            binding.editLastName.setText(it.getString("lastName", ""))
            binding.editAge.setText(it.getString("age", ""))
            binding.editDescription.setText(it.getString("description", ""))
            val profileImageUrl = it.getString("profileImageUrl", "")
            val coverImageUrl = it.getString("coverImageUrl", "")

            if (!profileImageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(binding.imgEditProfilePicture)
            }

            if (!coverImageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(coverImageUrl)
                    .placeholder(R.drawable.cover_placeholder)
                    .error(R.drawable.cover_placeholder)
                    .into(binding.imgEditCover)
            }
        }

        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnSaveChanges.setOnClickListener {
            saveProfileChanges()
        }

        binding.imgAddPictureIcon.setOnClickListener {
            openImagePickerForProfile()
        }

        binding.imgEditCover.setOnClickListener {
            openImagePickerForCover()
        }

        binding.imgAddCoverIcon.setOnTouchListener(DragTouchListener())
    }

    class DragTouchListener : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Calculate the difference between the view's position and touch position
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    // Move the view to the new position
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    // Optional: Save the new position or perform a snap-to-grid
                }
            }
            return true
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            // Determine which image is being picked (profile or cover)
            when {
                profileImageUri != null -> {
                    profileImageUri = selectedImageUri
                    binding.imgEditProfilePicture.setImageURI(profileImageUri)
                }
                coverImageUri != null -> {
                    coverImageUri = selectedImageUri
                    binding.imgEditCover.setImageURI(coverImageUri)
                }
            }
        }
    }

    private fun openImagePickerForProfile() {
        profileImageUri = Uri.EMPTY
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        imagePickerLauncher.launch(intent)
    }

    private fun openImagePickerForCover() {
        coverImageUri = Uri.EMPTY
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
        if (profileImageUri == null && coverImageUri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            updateUserDataInFirestore(userId, firstName, lastName, age, description, null, null)
            return
        }
        uploadImagesToStorage(userId, firstName, lastName, age, description)
    }

    private fun uploadImagesToStorage(
        userId: String,
        firstName: String,
        lastName: String,
        age: String,
        description: String
    ) {
        if (profileImageUri != null) {
            uploadImageToStorage(userId, profileImageUri!!, "profile", firstName, lastName, age, description)
        }
        if (coverImageUri != null) {
            uploadImageToStorage(userId, coverImageUri!!, "cover", firstName, lastName, age, description)
        }
    }

    private fun uploadImageToStorage(
        userId: String,
        imageUri: Uri,
        imageType: String,
        firstName: String,
        lastName: String,
        age: String,
        description: String
    ) {
        val fileName = "${imageType}_images/$userId/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    if (imageType == "profile") {
                        updateUserDataInFirestore(userId, firstName, lastName, age, description, uri.toString(), null)
                    } else if (imageType == "cover") {
                        updateUserDataInFirestore(userId, firstName, lastName, age, description, null, uri.toString())
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUserDataInFirestore(
        userId: String,
        firstName: String,
        lastName: String,
        age: String,
        description: String,
        profileImageUrl: String?,
        coverImageUrl: String?
    ) {
        val updatedUserData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "age" to age,
            "description" to description
        )

        profileImageUrl?.let { updatedUserData["profileImageUrl"] = it }
        coverImageUrl?.let { updatedUserData["coverImageUrl"] = it }

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
