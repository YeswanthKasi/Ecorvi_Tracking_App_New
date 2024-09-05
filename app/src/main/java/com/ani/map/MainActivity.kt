package com.ani.map

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.android.material.navigation.NavigationView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.ani.map.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.type.Color

class MainActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

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
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED_FIELD_NUMBER)
    private var polyline: Polyline? = null
    private var previousLatLng: LatLng? = null

    // Drawer components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Initialize DrawerLayout and NavigationView
        drawerLayout = binding.drawerLayoutMain
        navigationView = binding.navViewMain

        // Set up DrawerToggle to control open/close of the drawer
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.drawer_open, R.string.drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Set up the navigation click listener
        navigationView.setNavigationItemSelectedListener(this)

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

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the back button on the toolbar
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                // Profile navigation action can be handled here
                // Example: Show a Toast message or start a ProfileActivity (not yet implemented)
            }
            R.id.nav_main_activity -> {
                // Reload MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_maps_activity -> {
                // Navigate to MapsActivity
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
            }
        }
        // Close the drawer after an item is selected
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This is required for the drawer toggle to work properly
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
}
