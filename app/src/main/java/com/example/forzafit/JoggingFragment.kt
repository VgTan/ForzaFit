package com.example.forzafit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class JoggingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var distanceTextView: TextView
    private lateinit var targetTextView: TextView
    private lateinit var finishButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var avatarImageView: ImageView
    private var taskId: String? = null
    private var distance: Float = 0f

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var targetDistance: Float = 0f
    private var actualDistance: Float = 0f
    private var previousLocation: Location? = null
    private var googleMap: GoogleMap? = null
    private val pathPoints = mutableListOf<LatLng>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jogging, container, false)

        mapView = view.findViewById(R.id.joggingMapView)
        distanceTextView = view.findViewById(R.id.distanceTextView)
        targetTextView = view.findViewById(R.id.targetTextView)
        finishButton = view.findViewById(R.id.btnFinishJogging)
        progressBar = view.findViewById(R.id.progressBar)
        avatarImageView = view.findViewById(R.id.avatarImageView)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        taskId = arguments?.getString("taskId")
        loadTaskDetails()
        loadUserAvatar()


        finishButton.setOnClickListener {
            if (actualDistance > 0) {
                progressBar.visibility = View.VISIBLE
                finishButton.isEnabled = false
                updateXPAndCompleteTask(distance.toInt())
            } else {
                Toast.makeText(context, "No distance found to complete", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadUserAvatar() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val avatarId = document.getString("avatarId") ?: "bear" // Default to "bear"
                        updateAvatarImage(avatarId)
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load user avatar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateAvatarImage(avatarId: String) {
        val avatarResId = when (avatarId) {
            "bear" -> R.drawable.runningbear // Replace with your bear image resource
            "chicken" -> R.drawable.runningchicken // Replace with your chicken image resource
            else -> R.drawable.runningbear // Replace with a default image resource
        }
        avatarImageView.setImageResource(avatarResId)
        avatarImageView.visibility = View.VISIBLE
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        checkPermissionsAndStartTracking()
    }

    private fun checkPermissionsAndStartTracking() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            startLocationTracking()
        }
    }

    private fun startLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    updateLocationOnMap(location)
                    previousLocation = location
                }
            }

            fusedLocationClient.requestLocationUpdates(
                com.google.android.gms.location.LocationRequest.create().apply {
                    interval = 5000
                    fastestInterval = 2000
                    priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                },
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val currentLocation = result.lastLocation
                        if (currentLocation != null) {
                            updateDistance(currentLocation)
                            updateLocationOnMap(currentLocation)
                        }
                    }
                },
                null
            )
        }
    }

    private fun updateDistance(currentLocation: Location) {
        if (previousLocation != null) {
            val distanceBetween = previousLocation!!.distanceTo(currentLocation) / 1000 // in km
            actualDistance += distanceBetween

            val roundedDistance = String.format("%.1f", actualDistance)
            distanceTextView.text = "Distance: $roundedDistance km"

            // Stop tracking if target is reached
            if (actualDistance >= targetDistance) {
                finishButton.isEnabled = true
                progressBar.visibility = View.GONE
                stopLocationTracking()
            }
        }
        previousLocation = currentLocation
    }


    private var currentMarker: Marker? = null
    private fun updateLocationOnMap(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        pathPoints.add(latLng)

        drawPolyline()
        currentMarker?.remove()
        val customMarkerIcon = BitmapDescriptorFactory.fromResource(R.drawable.custom_map_marker)
        currentMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(customMarkerIcon)
                .title("You are here")
        )
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }


    private fun drawPolyline() {
        if (googleMap != null && pathPoints.size > 1) {
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(pathPoints)
                    .width(10f)
                    .color(Color.parseColor("#532200"))
            )
        }
    }

    private fun loadTaskDetails() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            taskId?.let { id ->
                progressBar.visibility = View.VISIBLE
                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        progressBar.visibility = View.GONE
                        if (document.exists()) {
                            targetDistance = document.getString("value")?.toFloatOrNull() ?: 0f
                            targetTextView.text = "Target: %.2f km".format(targetDistance)
                            distanceTextView.text = "Distance: 0.00 km"
                        } else {
                            Toast.makeText(context, "Task not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Failed to load task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateXPAndCompleteTask(reps: Int) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val currentTime = System.currentTimeMillis()

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentXP = document.getLong("xp")?.toInt() ?: 0
                        val currentLevel = document.getLong("level")?.toInt() ?: 1
                        val newXP = currentXP + reps

                        var updatedXP = newXP
                        var updatedLevel = currentLevel

                        while (updatedXP >= 100) {
                            updatedXP -= 100
                            updatedLevel += 1
                        }

                        val currentJoggingToday = document.getLong("joggingToday")?.toInt() ?: 0
                        val currentJoggingThisWeek = document.getLong("joggingThisWeek")?.toInt() ?: 0
                        val currentJoggingLast3Months = document.getLong("joggingLast3Months")?.toInt() ?: 0

                        val lastUpdatedToday = document.getLong("lastUpdatedToday") ?: 0L
                        val lastUpdatedWeek = document.getLong("lastUpdatedWeek") ?: 0L
                        val lastUpdated3Months = document.getLong("lastUpdated3Months") ?: 0L

                        val resetToday = currentTime - lastUpdatedToday > 24 * 60 * 60 * 1000
                        val resetWeek = currentTime - lastUpdatedWeek > 7 * 24 * 60 * 60 * 1000
                        val reset3Months = currentTime - lastUpdated3Months > (3 * 30.44 * 24 * 60 * 60 * 1000).toLong()

                        val updatedJoggingToday = if (resetToday) reps else currentJoggingToday + reps
                        val updatedJoggingThisWeek = if (resetWeek) reps else currentJoggingThisWeek + reps
                        val updatedJoggingLast3Months = if (reset3Months) reps else currentJoggingLast3Months + reps

                        val updates = mutableMapOf<String, Any>(
                            "xp" to updatedXP,
                            "level" to updatedLevel,
                            "joggingToday" to updatedJoggingToday,
                            "joggingThisWeek" to updatedJoggingThisWeek,
                            "joggingLast3Months" to updatedJoggingLast3Months,
                            "lastUpdatedToday" to if (resetToday) currentTime else lastUpdatedToday,
                            "lastUpdatedWeek" to if (resetWeek) currentTime else lastUpdatedWeek,
                            "lastUpdated3Months" to if (reset3Months) currentTime else lastUpdated3Months
                        )

                        db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                markTaskAsComplete()
                            }
                            .addOnFailureListener {
                                progressBar.visibility = View.GONE
                                finishButton.isEnabled = true
                                Toast.makeText(context, "Failed to update XP and progress", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    finishButton.isEnabled = true
                    Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun markTaskAsComplete() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            taskId?.let { id ->
                val currentTime =
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                db.collection("users").document(userId)
                    .collection("to_do_list").document(id)
                    .update(mapOf("status" to "complete", "finished_time" to currentTime))
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        finishButton.isEnabled = true
                        Toast.makeText(context, "Task marked as complete", Toast.LENGTH_SHORT).show()
                        navigateToProfileFragment()
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        finishButton.isEnabled = true
                        Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(object : com.google.android.gms.location.LocationCallback() {})
    }

    private fun navigateToProfileFragment() {
        parentFragmentManager.popBackStack()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}