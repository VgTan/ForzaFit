package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ToDoFragment : Fragment() {

    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var progressBar: ProgressBar // Added ProgressBar

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val taskList = mutableListOf<TaskAdapter.Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_todo, container, false)

        // Initialize views
        taskRecyclerView = view.findViewById(R.id.taskRecyclerView)
        backButton = view.findViewById(R.id.btnBack)
        progressBar = view.findViewById(R.id.progressBar) // Initialize ProgressBar

        // Setup RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        taskRecyclerView.adapter = TaskAdapter(taskList) { task ->
            when {
                task.name.contains("Push Up") -> navigateToFragment(PushUpFragment(), task.taskId)
                task.name.contains("Jogging") -> navigateToFragment(JoggingFragment(), task.taskId)
                task.name.contains("Pull Up") -> navigateToFragment(PullUpFragment(), task.taskId)
                task.name.contains("Sit Up") -> navigateToFragment(SitUpFragment(), task.taskId)
                else -> Toast.makeText(context, "Unknown task type", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch tasks and display progress bar
        fetchIncompleteTasks()

        backButton.setOnClickListener {
            backButton.isEnabled = false
            navigateToHomeFragment()
        }

        return view
    }

    private fun fetchIncompleteTasks() {
        progressBar.visibility = View.VISIBLE
        backButton.isEnabled = false // Disable back button during task fetch

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            db.collection("users").document(userId)
                .collection("to_do_list")
                .whereEqualTo("status", "incomplete")
                .get()
                .addOnSuccessListener { documents ->
                    taskList.clear()

                    for (document in documents) {
                        val taskType = document.getString("exercise") ?: "Unknown"
                        val taskValue = document.getString("value") ?: "0"
                        val taskId = document.id

                        taskList.add(TaskAdapter.Task(taskId, taskType, "$taskValue units"))
                    }

                    taskRecyclerView.adapter?.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    progressBar.visibility = View.GONE
                    backButton.isEnabled = true // Re-enable button
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

    private fun navigateToHomeFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }
}
