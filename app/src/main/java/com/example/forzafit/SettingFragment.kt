package com.example.forzafit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {

    private lateinit var accountsTitle: TextView
    private lateinit var accountsContent: LinearLayout
    private lateinit var userName: EditText
    private lateinit var submit: Button
    private lateinit var logOut: Button
    private lateinit var delete: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var privacyTitle: TextView
    private lateinit var privacyContent: LinearLayout
    private lateinit var BMIStatus: Switch
    private lateinit var calculatorTitle: TextView
    private lateinit var calculatorContent: LinearLayout
    private lateinit var inputHeight: EditText
    private lateinit var inputWeight: EditText
    private lateinit var yourBmi: TextView
    private lateinit var accesibilityTitle: TextView
    private lateinit var accesibilityContent: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Bind Views
        accountsTitle = view.findViewById(R.id.accounts_title)
        accountsContent = view.findViewById(R.id.accounts_content)
        userName = view.findViewById(R.id.username)
        submit = view.findViewById(R.id.submit_button)
        logOut = view.findViewById(R.id.logout_button)
        delete = view.findViewById(R.id.delete_button)
        progressBar = view.findViewById(R.id.progressBar)
        privacyTitle = view.findViewById(R.id.privacy_title)
        privacyContent = view.findViewById(R.id.privacy_content)
        BMIStatus = view.findViewById(R.id.switch_bmi_status)
        calculatorTitle = view.findViewById(R.id.calculator_title)
        calculatorContent = view.findViewById(R.id.calculator_content)
        inputHeight = view.findViewById(R.id.input_height)
        inputWeight = view.findViewById(R.id.input_weight)
        yourBmi = view.findViewById(R.id.your_bmi)
        accesibilityTitle = view.findViewById(R.id.accesibility_title)
        accesibilityContent = view.findViewById(R.id.accesibility_content)

        userId = auth.currentUser?.uid

        // Set click listeners to toggle visibility for accordion effect
        accountsTitle.setOnClickListener {
            toggleVisibility(accountsContent)
            if (userId != null) loadUserData()
        }

        submit.setOnClickListener {
            val updatedUserName = userName.text.toString()
            if (updatedUserName.isNotEmpty()) {
                handleButtonClick(submit) // Disable button
                updateUser(updatedUserName)
            }
        }

        logOut.setOnClickListener {
            handleButtonClick(logOut)
            auth.signOut()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToFragment(LoginFragment())
        }

        delete.setOnClickListener {
            handleButtonClick(delete)
            deleteUserAccount()
        }

        privacyTitle.setOnClickListener {
            toggleVisibility(privacyContent)
            if(userId != null){
                initializeBMIstatus()

                BMIStatus.setOnCheckedChangeListener { _, isChecked ->
                    updateBMIStatus(isChecked)
                }
            }
        }

        calculatorTitle.setOnClickListener {
            toggleVisibility(calculatorContent)
            if (userId != null) {
                // Ambil data user dari Firestore
                loadBMIUser()
                // Setup BMI Calculation
                setupBmiCalculation()
            } else {
                Log.e("Auth", "User is not logged in")
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
        accesibilityTitle.setOnClickListener {
            toggleVisibility(accesibilityContent)
        }

        return view
    }

    private fun loadUserData() {
        progressBar.visibility = View.VISIBLE
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    progressBar.visibility = View.GONE
                    if (document.exists()) {
                        val user = document.getString("userName")
                        userName.setText(user)
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initializeBMIstatus(){
        userId?.let { uid->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val showBMI = document.getBoolean("showBMI") ?: true
                        BMIStatus.isChecked = showBMI
                    } else {
                        Log.e("Firestore", "Switch tidak ditemukan.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error fetching showBMI status", e)
                }
        }
    }

    private fun loadBMIUser() {
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val height = document.getString("height") ?: "0"
                        val weight = document.getString("weight") ?: "0"

                        inputHeight.setText(height)
                        inputWeight.setText(weight)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error fetching user data", e)
                }
        }
    }

    private fun toggleVisibility(content: View) {
        content.visibility = if (content.visibility == View.GONE) View.VISIBLE else View.GONE
    }

//    function kalkulasi BMI
    private fun setupBmiCalculation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateBmi()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        inputHeight.addTextChangedListener(textWatcher)
        inputWeight.addTextChangedListener(textWatcher)
    }

    private fun calculateBmi() {
        val heightText = inputHeight.text.toString()
        val weightText = inputWeight.text.toString()

        if (heightText.isNotEmpty() && weightText.isNotEmpty()) {
            val height = heightText.toDoubleOrNull() ?: 0.0
            val weight = weightText.toDoubleOrNull() ?: 0.0

            if (height > 0 && weight > 0) {
                val heightInMeters = height / 100 // Convert height from cm to meters
                val bmi = weight / (heightInMeters * heightInMeters)
                yourBmi.text = "Your BMI: %.2f".format(bmi)

                // Update Firestore dengan height, weight, dan BMI
                updateBMI(heightText, weightText, bmi)

            } else {
                yourBmi.text = "Invalid input"
            }
        } else {
            yourBmi.text = "Your BMI: -"
        }
    }

    private fun updateUser(userName: String) {
        progressBar.visibility = View.VISIBLE
        userId?.let { uid ->
            val updates = mapOf("userName" to userName)
            firestore.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    submit.isEnabled = true
                    Toast.makeText(context, "Username updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    submit.isEnabled = true
                    Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateBMIStatus(showBMI: Boolean) {
        userId?.let { uid ->
            val userUpdates = mapOf(
                "showBMI" to showBMI
            )
            firestore.collection("users").document(uid)
                .update(userUpdates)
                .addOnSuccessListener {
                    Log.d("Firestore", "BMI status updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating showBMI", e)
                }
        }
    }

    private fun updateBMI(height: String, weight: String, bmi: Double) {
        userId?.let { uid ->
            val userUpdates = mapOf(
                "height" to height,
                "weight" to weight,
                "BMI" to bmi.toString()
            )

            firestore.collection("users").document(uid)
                .update(userUpdates)
                .addOnSuccessListener {
                    Log.d("Firestore", "User data updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating user data", e)
                }
        }
    }

    private fun deleteUserAccount() {
        progressBar.visibility = View.VISIBLE
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .delete()
                .addOnSuccessListener {
                    auth.currentUser?.delete()?.addOnCompleteListener { task ->
                        progressBar.visibility = View.GONE
                        delete.isEnabled = true
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            navigateToFragment(SignUpFragment())
                        } else {
                            Toast.makeText(context, "Failed to delete account", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    delete.isEnabled = true
                    Toast.makeText(context, "Failed to delete data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleButtonClick(button: Button) {
        button.isEnabled = false
        progressBar.visibility = View.VISIBLE
    }

    private fun navigateToFragment(fragment: Fragment) {
        (activity as? MainActivity)?.HideBottomNav()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}
