package com.example.forzafit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.compose.material3.DatePickerDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
        val nextButton: Button = view.findViewById(R.id.button_next_choose)
        val birthDateEditText: EditText = view.findViewById(R.id.editTextDate)
        val alreadyHave : Button = view.findViewById(R.id.alreadyAccount)

        alreadyHave.setOnClickListener() {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        birthDateEditText.setOnClickListener {
            showDatePickerDialog(birthDateEditText)
        }

        nextButton.setOnClickListener {
            val email = view.findViewById<EditText>(R.id.input_email).text.toString()
            val password = view.findViewById<EditText>(R.id.input_password).text.toString()
            val firstName = view.findViewById<EditText>(R.id.input_first_name).text.toString()
            val lastName = view.findViewById<EditText>(R.id.input_last_name).text.toString()
            val userName = view.findViewById<EditText>(R.id.input_username).text.toString()
            val birthDate = birthDateEditText.text.toString()
            val height = view.findViewById<EditText>(R.id.input_height).text.toString()
            val weight = view.findViewById<EditText>(R.id.input_weight).text.toString()
            val targetWeight = view.findViewById<EditText>(R.id.input_target_weight).text.toString()

            registerUser(email, password, firstName, lastName, userName, birthDate, height, weight, targetWeight)
        }

        return view
    }

    private fun registerUser(email: String, password: String, firstName: String, lastName: String, userName: String,
                             birthDate: String, height: String, weight: String, targetWeight: String) {
        // Perhitungan BMI
        val heightValue = height.toDoubleOrNull() ?: 0.0
        val weightValue = weight.toDoubleOrNull() ?: 0.0
        val bmi = if (heightValue > 0 && weightValue > 0) {
            val heightInMeters = heightValue / 100 // Konversi tinggi ke meter
            weightValue / (heightInMeters * heightInMeters)
        } else {
            null // Jika input tidak valid, BMI tidak dihitung
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userData = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "userName" to userName,
                        "birthDate" to birthDate,
                        "height" to height,
                        "weight" to weight,
                        "targetWeight" to targetWeight,
                        "xp" to 0,
                        "level" to 0,
                        "BMI" to (bmi?.let { "%.2f".format(it) } ?: "Invalid"),
                        "bmiLastCalculated" to System.currentTimeMillis()
                    )

                    user?.let {
                        firestore.collection("users").document(it.uid).set(userData)
                            .addOnSuccessListener {
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, ChooseAvatarFragment())
                                    .commit()
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error writing document", e)
                            }
                    }
                } else {
                    Toast.makeText(context, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate =
                    "$selectedDay/${selectedMonth + 1}/$selectedYear" // Adjust format as needed
                editText.setText(selectedDate)
            },
            year,
            month,
            day
        ).show()
    }

    companion object {
        private const val TAG = "SignUpFragment"
    }
}