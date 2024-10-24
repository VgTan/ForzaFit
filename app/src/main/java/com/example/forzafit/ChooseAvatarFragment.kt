package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChooseAvatarFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedAvatarId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_choose_avatar, container, false)

        val bearButton: ImageView = view.findViewById(R.id.imageButton2)
        val selectBearButton: Button = view.findViewById(R.id.BearSelect)
        val selectChickenButton: Button = view.findViewById(R.id.ChickenSelect)
        val chickenButton: ImageView = view.findViewById(R.id.imageButton)

        bearButton.setOnClickListener {
            selectedAvatarId = "bear"
            saveAvatarToFirestore(selectedAvatarId)
        }

        selectBearButton.setOnClickListener {
            selectedAvatarId = "bear"
            saveAvatarToFirestore(selectedAvatarId)
        }

        chickenButton.setOnClickListener {
            selectedAvatarId = "chicken"
            saveAvatarToFirestore(selectedAvatarId)
        }

        selectChickenButton.setOnClickListener {
            selectedAvatarId = "chicken"
            saveAvatarToFirestore(selectedAvatarId)
        }

        return view
    }

    private fun saveAvatarToFirestore(avatarId: String?) {
        val userId = auth.currentUser?.uid ?: return

        val userMap = hashMapOf(
            "avatarId" to avatarId
        )

        firestore.collection("users").document(userId)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}