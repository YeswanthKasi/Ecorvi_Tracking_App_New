package com.ani.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.ani.map.databinding.ActivityMapsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var liveLocationButton: Button
    private lateinit var signOutButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var requestingLocationUpdates = false
    private var isTracking = false
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)
    private var startPoint: LatLng? = null
    private var endPoint: LatLng? = null
    private var polyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        liveLocationButton = findViewById(R.id.live_location_button)
        liveLocationButton.setOnClickListener {
            requestLocationPermission()
        }

        signOutButton = findViewById(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            signOutAndStartSignInActivity()
        }

        startButton = findViewById(R.id.start_button)
        startButton.setOnClickListener {
            isTracking = true
            startLocationUpdates()
        }

        stopButton = findViewById(R.id.stop_button)
        stopButton.setOnClickListener {
            isTracking = false
            stopLocationUpdates()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.SECONDS.toMillis(5)
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                Log.i("@ani", "Location Changed ${result.lastLocation?.latitude}, ${result.lastLocation?.longitude} ")
                updateMapLocation(result.lastLocation)
            }
        }

        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("locations")
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Log.e("Permission denied", "Location permission denied")
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        requestingLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        requestingLocationUpdates = false
    }

    private fun updateMapLocation(location: Location?) {
        if (location != null && isTracking) {
            val latLng = LatLng(location.latitude, location.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

            if (startPoint == null) {
                // First point, set as start point and add marker
                startPoint = latLng
                startMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
            } else {
                // Add new point and update polyline
                endPoint = latLng
                endMarker?.remove() // Remove previous end marker
                endMarker = mMap.addMarker(MarkerOptions().position(latLng).title("End Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

                // Update polyline to join location points
                if (polylineOptions.points.isNotEmpty()) {
                    polylineOptions.add(latLng) // Add the latest point to the polyline
                } else {
                    polylineOptions.add(startPoint!!) // Add the start point if it’s the first update
                }

                // Remove previous polyline if it exists
                polyline?.remove()

                // Add new polyline
                polyline = mMap.addPolyline(polylineOptions)

                // Update start point for the next iteration
                startPoint = endPoint
            }

            // Store location data in Firebase Realtime Database
            val locationData = LocationData(location.latitude, location.longitude, System.currentTimeMillis())
            locationRef.push().setValue(locationData)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Apply the custom map style using the JSON file
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        } catch (e: Exception) {
            Log.e("MapsActivity", "Can't find style. Error: ", e)
        }

        // Center the map on India and set zoom level
        val indiaLatLng = LatLng(20.5937, 78.9629)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(indiaLatLng, 5.0f))

        // Adjust padding to move zoom controls up
        mMap.setPadding(0, 350, 0, 0)  // Adjust the top padding as needed
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}

data class LocationData(val latitude: Double, val longitude: Double, val timestamp: Long)
