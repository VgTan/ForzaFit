package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddTaskFragment : Fragment() {

    private lateinit var exerciseTypeSpinner: Spinner
    private lateinit var inputField: EditText
    private lateinit var submitButton: Button

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_task, container, false)

        // Initialize views
        exerciseTypeSpinner = view.findViewById(R.id.exerciseTypeSpinner)
        inputField = view.findViewById(R.id.inputField)
        submitButton = view.findViewById(R.id.btnNext)

        // Set up exercise type dropdown
        val exerciseTypes = arrayOf("Jogging", "Push Up")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exerciseTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        exerciseTypeSpinner.adapter = adapter

        // Set listener for spinner to update input hint
        exerciseTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> inputField.hint = "Enter kilometers"
                    1 -> inputField.hint = "Enter repetitions"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set up submit button click event
        submitButton.setOnClickListener {
            val selectedExercise = exerciseTypeSpinner.selectedItem.toString()
            val inputValue = inputField.text.toString()

            if (inputValue.isNotEmpty()) {
                // Save the task to Firestore
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

            // Task data to be saved
            val taskData = hashMapOf(
                "exercise" to exercise,
                "value" to value,
                "status" to "incomplete"
            )

            // Add task to Firestore under 'to_do_list' sub-collection
            db.collection("users").document(userId)
                .collection("to_do_list") // Sub-collection under user document
                .add(taskData) // Add a new task document
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Task added successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to the ToDoListFragment or another fragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ToDoFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to add task: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
