package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {

    private lateinit var emailEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forgot_password, container, false)

        emailEditText = view.findViewById(R.id.emailEditText)
        sendButton = view.findViewById(R.id.sendButton)
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        sendButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email)
            }
        }

        return view
    }

    private fun resetPassword(email: String) {
        progressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { fetchTask ->
            if (fetchTask.isSuccessful) {
                val signInMethods = fetchTask.result?.signInMethods
                if (signInMethods.isNullOrEmpty()) {
                    progressBar.visibility = View.GONE
                    sendButton.isEnabled = true
                    Toast.makeText(context, "Email not found in the database.", Toast.LENGTH_SHORT).show()
                } else {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            progressBar.visibility = View.GONE
                            sendButton.isEnabled = true
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } else {
                progressBar.visibility = View.GONE
                sendButton.isEnabled = true
                Toast.makeText(context, "Error checking email: ${fetchTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
