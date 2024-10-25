package com.example.forzafit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SettingFragment : Fragment() {

    private lateinit var accountsTitle: TextView
    private lateinit var accountsContent: LinearLayout
    private lateinit var privacyTitle: TextView
    private lateinit var privacyContent: LinearLayout
    private lateinit var calculatorTitle: TextView
    private lateinit var calculatorContent: LinearLayout
    private lateinit var accesibilityTitle: TextView
    private lateinit var accesibilityContent: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Bind Views
        accountsTitle = view.findViewById(R.id.accounts_title)
        accountsContent = view.findViewById(R.id.accounts_content)
        privacyTitle = view.findViewById(R.id.privacy_title)
        privacyContent = view.findViewById(R.id.privacy_content)
        calculatorTitle = view.findViewById(R.id.calculator_title)
        calculatorContent = view.findViewById(R.id.calculator_content)
        accesibilityTitle = view.findViewById(R.id.accesibility_title)
        accesibilityContent = view.findViewById(R.id.accesibility_content)

        // Set click listeners to toggle visibility for accordion effect
        accountsTitle.setOnClickListener {
            toggleVisibility(accountsContent)
        }

        privacyTitle.setOnClickListener {
            toggleVisibility(privacyContent)
        }

        calculatorTitle.setOnClickListener {
            toggleVisibility(calculatorContent)
        }
        accesibilityTitle.setOnClickListener {
            toggleVisibility(accesibilityContent)
        }

        return view
    }

    // Function to toggle visibility of the content sections
    private fun toggleVisibility(content: View) {
        if (content.visibility == View.GONE) {
            content.visibility = View.VISIBLE
        } else {
            content.visibility = View.GONE
        }
    }
}
