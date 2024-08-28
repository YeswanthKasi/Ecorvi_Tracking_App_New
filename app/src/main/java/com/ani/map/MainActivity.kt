package com.ani.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var fetchLocationButton: Button
    private lateinit var signOutButton: Button
    private lateinit var database: DatabaseReference
    private var isFetching = false
    private var locationListener: ValueEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 5000L // 5 seconds
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)
    private var polyline: Polyline? = null
    private var previousLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth and Google Sign-In
        mAuth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI elements
        signOutButton = findViewById(R.id.sign_out_button)
        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)
        fetchLocationButton = findViewById(R.id.fetchLocationButton)

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().getReference("locations")

        // Set up button click listeners
        fetchLocationButton.setOnClickListener {
            if (isFetching) {
                stopFetchingLocationData()
            } else {
                startFetchingLocationData()
            }
        }

        signOutButton.setOnClickListener {
            signOutAndStartSignInActivity()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        googleMap.isMyLocationEnabled = true

        // Center the map on India
        val indiaLatLng = LatLng(20.5937, 78.9629)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(indiaLatLng, 5.0f))

        // Apply the custom map style
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        } catch (e: Exception) {
            Log.e("MainActivity", "Can't find style. Error: ", e)
        }
    }

    private fun startFetchingLocationData() {
        isFetching = true
        fetchLocationButton.text = "Stop Fetching"

        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (locationSnapshot in snapshot.children) {
                    updateMapWithLocation(locationSnapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                latitudeTextView.text = "Failed to fetch data"
                longitudeTextView.text = ""
            }
        }

        // Start fetching location data every 5 seconds
        handler.post(object : Runnable {
            override fun run() {
                if (isFetching) {
                    database.orderByKey().limitToLast(1).addListenerForSingleValueEvent(locationListener as ValueEventListener)
                    handler.postDelayed(this, updateInterval)
                }
            }
        })
    }

    private fun stopFetchingLocationData() {
        isFetching = false
        fetchLocationButton.text = "Fetch Location"

        locationListener?.let {
            database.removeEventListener(it)
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateMapWithLocation(locationSnapshot: DataSnapshot) {
        val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
        val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)

        if (latitude != null && longitude != null) {
            val latLng = LatLng(latitude, longitude)
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Host Location"))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            // Update polyline with new location
            if (previousLatLng != null) {
                polylineOptions.add(previousLatLng!!).add(latLng)
                polyline?.remove()
                polyline = googleMap.addPolyline(polylineOptions)
            } else {
                // First point, initialize the polyline with the starting point
                polylineOptions.add(latLng)
                polyline = googleMap.addPolyline(polylineOptions)
            }

            previousLatLng = latLng

            latitudeTextView.text = "Latitude: $latitude"
            longitudeTextView.text = "Longitude: $longitude"
        } else {
            latitudeTextView.text = "Latitude: N/A"
            longitudeTextView.text = "Longitude: N/A"
        }
    }

    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFetchingLocationData()
    }
}
