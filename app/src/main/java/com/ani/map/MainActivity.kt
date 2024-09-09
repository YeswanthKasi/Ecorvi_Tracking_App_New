package com.ani.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ani.map.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import org.json.JSONObject
import com.google.android.gms.maps.model.Polyline
import org.checkerframework.checker.units.qual.A

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
    private val updateInterval = 1000L // 1 second
    private lateinit var mAuth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var binding: ActivityMainBinding

    private var currentLocationMarker: Marker? = null
    private val coveredRoutePoints = mutableListOf<LatLng>()
    private var coveredRoutePolyline: Polyline? = null

    // New variables for recenter functionality
    private lateinit var recenterButton: Button
    private var isUserZooming = false
    private var defaultZoomLevel: Float = 15f // Default zoom level for recentering

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        mAuth = Firebase.auth

        // Initialize UI elements
        signOutButton = findViewById(R.id.sign_out_button)
        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)
        fetchLocationButton = findViewById(R.id.fetchLocationButton)
        recenterButton = findViewById(R.id.recenter_button) // New recenter button

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().getReference("locations")

        // Initialize DrawerLayout and NavigationView
        drawerLayout = binding.drawerLayoutMain
        navigationView = binding.navViewMain
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

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

        // Recenter button functionality
        recenterButton.setOnClickListener {
            isUserZooming = false
            recenterToLiveLocation()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the back button on the toolbar
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                // Profile navigation action
            }
            R.id.nav_main_activity -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_maps_activity -> {
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

            // Remove old marker if it exists
            currentLocationMarker?.remove()

            // Add new marker
            currentLocationMarker = googleMap.addMarker(
                MarkerOptions().position(latLng).title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            // Update map camera only if the user is not zooming manually
            if (!isUserZooming) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoomLevel))
            }

            // Update polyline with new location
            coveredRoutePoints.add(latLng)
            updateCoveredRoutePolyline()

            // Update latitude and longitude TextViews
            latitudeTextView.text = "Latitude: $latitude"
            longitudeTextView.text = "Longitude: $longitude"
        } else {
            latitudeTextView.text = "Latitude: N/A"
            longitudeTextView.text = "Longitude: N/A"
        }
    }

    private fun updateCoveredRoutePolyline() {
        val polylineOptions = PolylineOptions()
            .width(15f)
            .color(android.graphics.Color.GREEN)
            .geodesic(true)

        polylineOptions.addAll(coveredRoutePoints)

        // Remove the old polyline if it exists
        coveredRoutePolyline?.remove()

        // Add the new polyline
        coveredRoutePolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Method to get the direction URL for Visakhapatnam to Hyderabad
    fun getDirectionURL(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=driving&key=AIzaSyBgNDkbkEYXu3Lup7mpOCZ1kRXrPMU047U"
    }

    // AsyncTask to fetch and draw the route
    private inner class GetDirection(val url: String) : AsyncTask<Void, Void, List<List<LatLng>>>() {
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body?.string() ?: ""
            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)

                val path = ArrayList<LatLng>()
                for (step in respObj.routes[0].legs[0].steps) {
                    path.addAll(decodePolyline(step.polyline.points))
                }
                result.add(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices) {
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(android.graphics.Color.RED)
                lineoption.geodesic(true)
            }
            googleMap.addPolyline(lineoption)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Detect when the user manually changes the zoom level
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserZooming = true
            }
        }

        // Apply custom map style (this part remains unchanged)

        // Move camera to the default location initially
        val GVPCE = LatLng(17.8199, 83.3438)
        val GVPCEW = LatLng(17.8222, 83.3487)

        // Fit the camera to the route initially
        fitCameraToRoute(GVPCE, GVPCEW)

        // Draw the route from GVPCE to GVPCEW
        val directionURL = getDirectionURL(GVPCE, GVPCEW)
        GetDirection(directionURL).execute()
    }

    private fun fitCameraToRoute(startPoint: LatLng, endPoint: LatLng) {
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(startPoint)
        boundsBuilder.include(endPoint)

        val bounds = boundsBuilder.build()
        val padding = 100 // Padding to fit the route in the camera view
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    private fun recenterToLiveLocation() {
        // Fetch the last known location from the database and move the camera back to it
        database.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (locationSnapshot in snapshot.children) {
                    val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)

                    if (latitude != null && longitude != null) {
                        val latLng = LatLng(latitude, longitude)

                        // Move camera back to the live location and reset the zoom level
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoomLevel))

                        // Clear the user's custom zoom state
                        isUserZooming = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    // Polyline decoding logic remains the same...
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }

        return poly
    }
}
