package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ToDoFragment : Fragment() {

    private lateinit var taskListView: ListView
    private lateinit var backButton: Button

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val taskList = mutableListOf<String>()
    private val taskIds = mutableListOf<String>() // Store task IDs for navigation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_todo, container, false)

        // Initialize views
        taskListView = view.findViewById(R.id.taskListView)
        backButton = view.findViewById(R.id.btnBack)

        // Fetch tasks from Firestore
        fetchIncompleteTasks()

        // Handle list item clicks
        taskListView.setOnItemClickListener { _, _, position, _ ->
            val selectedTaskId = taskIds[position]
            val selectedTask = taskList[position]

            // Navigate to the respective fragment based on the task type
            when {
                selectedTask.contains("Push Up") -> navigateToFragment(PushUpFragment(), selectedTaskId)
                selectedTask.contains("Jogging") -> navigateToFragment(JoggingFragment(), selectedTaskId)
                else -> Toast.makeText(context, "Unknown task type", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button click
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun fetchIncompleteTasks() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Query Firestore to get only tasks with status "incomplete"
            db.collection("users").document(userId)
                .collection("to_do_list")
                .whereEqualTo("status", "incomplete")
                .get()
                .addOnSuccessListener { documents ->
                    taskList.clear()
                    taskIds.clear()

                    for (document in documents) {
                        val taskType = document.getString("exercise") ?: "Unknown"
                        val taskValue = document.getString("value") ?: "0"
                        val taskId = document.id

                        // Add the task to the list and store its ID
                        taskList.add("$taskType $taskValue")
                        taskIds.add(taskId)
                    }

                    // Update the ListView adapter
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, taskList)
                    taskListView.adapter = adapter
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToFragment(fragment: Fragment, taskId: String) {
        val bundle = Bundle()
        bundle.putString("taskId", taskId)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}