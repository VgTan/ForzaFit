package com.example.forzafit

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LazyLoadFragment())
            .commit()

        Handler(Looper.getMainLooper()).postDelayed({
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, FirstLandingPageFragment())
            transaction.commit()
        }, 3000)
    }
}
