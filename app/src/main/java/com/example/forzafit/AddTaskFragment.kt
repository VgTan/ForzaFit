package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddTaskFragment : Fragment() {

    private lateinit var exerciseTypeSpinner: Spinner
    private lateinit var inputField: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar // Added ProgressBar

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_task, container, false)

        exerciseTypeSpinner = view.findViewById(R.id.exerciseTypeSpinner)
        inputField = view.findViewById(R.id.inputField)
        submitButton = view.findViewById(R.id.btnNext)
        progressBar = view.findViewById(R.id.progressBar)

        val exerciseTypes = arrayOf("Jogging", "Push Up", "Pull Up", "Sit Up")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exerciseTypes)
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        exerciseTypeSpinner.adapter = adapter

        exerciseTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                inputField.hint = when (position) {
                    0 -> "Enter kilometers"
                    else -> "Enter repetitions"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        submitButton.setOnClickListener {
            val selectedExercise = exerciseTypeSpinner.selectedItem.toString()
            val inputValue = inputField.text.toString()

            if (inputValue.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                submitButton.isEnabled = false
                saveTaskToFirestore(selectedExercise, inputValue)
            } else {
                Toast.makeText(requireContext(), "Please enter a value.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun saveTaskToFirestore(exercise: String, value: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            val taskData = hashMapOf(
                "exercise" to exercise,
                "value" to value,
                "status" to "incomplete"
            )

            db.collection("users").document(userId)
                .collection("to_do_list")
                .add(taskData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Task added successfully!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ToDoFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to add task: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true
                }
        } ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            submitButton.isEnabled = true
        }
    }
}
