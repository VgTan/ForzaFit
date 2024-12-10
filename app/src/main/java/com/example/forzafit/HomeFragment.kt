package com.example.forzafit

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit


class HomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var imgBearAvatar: ImageView
    private lateinit var btnLevelUp: Button
    private lateinit var btnToDo: Button
    private lateinit var progressExp: ProgressBar
    private lateinit var tvExp: TextView
    private lateinit var tvLevel: TextView
    private lateinit var konfettiView: KonfettiView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        tvWelcome = view.findViewById(R.id.tvWelcome)
        imgBearAvatar = view.findViewById(R.id.imgBearAvatar)
        btnLevelUp = view.findViewById(R.id.btnLevelUp)
        btnToDo = view.findViewById(R.id.btnToDo)
        progressExp = view.findViewById(R.id.progressExp)
        tvExp = view.findViewById(R.id.tvExp)
        tvLevel = view.findViewById(R.id.tvLevel)
//        konfettiView = view.findViewById(R.id.konfettiView)

        // Fetch user data from Firestore
        fetchUserData()

        val buttonClickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click)
        val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce)

        // Start bounce animation for the avatar
        imgBearAvatar.startAnimation(bounceAnimation)


        // Set onClick listeners for buttons
        btnLevelUp.setOnClickListener {
            showConfetti()
            it.startAnimation(buttonClickAnimation)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTaskFragment())
                .addToBackStack(null)
                .commit()

        }

        btnToDo.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ToDoFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    /**
     * Fetches user data from Firestore and updates the UI.
     */
    private fun fetchUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userName = document.getString("userName") ?: "[Name]"
                        var xp = document.getLong("xp")?.toInt() ?: 0
                        var level = document.getLong("level")?.toInt() ?: 1

                        // Handle leveling up
                        if (xp >= 100) {
                            xp -= 100
                            level += 1

                            db.collection("users").document(userId)
                                .update("xp", xp, "level", level)
                                .addOnSuccessListener {
                                    showConfetti()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to update level", Toast.LENGTH_SHORT).show()
                                }
                        }

                        val xpToNextLevel = 100 - xp

                        // Update UI elements
                        tvWelcome.text = "Welcome, $userName"
                        tvLevel.text = "Level $level"
                        tvExp.text = "$xpToNextLevel XP to next level"
                        animateProgressBar(progressExp, xp)
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Animates the progress bar from its current value to the new progress value.
     */
    private fun animateProgressBar(progressBar: ProgressBar, progress: Int) {
        val animator = android.animation.ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progress)
        animator.duration = 1500
        animator.start()
    }

    /**
     * Shows confetti animation when leveling up.
     */
    private fun showConfetti() {
        konfettiView.start(
            Party(
                speed = 10f, // Speed of the particles
                maxSpeed = 15f, // Maximum speed
                damping = 0.9f, // Damping for smooth movement
                angle = 270, // Angle for upward movement
                spread = 360, // Full-circle spread
                colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
                shapes = listOf(Shape.Circle, Shape.Square),
                size = listOf(Size.SMALL, Size.LARGE),
                position = Position.Relative(0.5, 0.0), // Start from top center
                emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(50)
            )
        )
    }

}
