package com.ani.map

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signinButton: Button
    private lateinit var clientLoginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin_layout)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Change status bar color to match theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(R.color.colorPrimaryDark, theme)
        }

        // Set light status bar for dark icons on a light background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Initialize views
        usernameEditText = findViewById(R.id.username_edittext)
        passwordEditText = findViewById(R.id.password_edittext)
        signinButton = findViewById(R.id.signin_button)
        clientLoginButton = findViewById(R.id.client_login_button)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check for updates on app launch
        UpdateChecker(this).checkForUpdates(
            onUpdateAvailable = { apkUrl ->
                showUpdateDialog(apkUrl)
            },
            onNoUpdate = {
                Toast.makeText(this, "App is up-to-date", Toast.LENGTH_SHORT).show()
            }
        )

        // Sign-in button click listener (for default sign-in action)
        signinButton.setOnClickListener {
            val email = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Authenticate user
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, default to MapsActivity
                        val intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Client login button click listener
        clientLoginButton.setOnClickListener {
            val email = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Authenticate user and open ClientMainActivity (or equivalent)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, start MainActivity (Client app)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showUpdateDialog(apkUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version of the app is available. Please update to continue.")
            .setPositiveButton("Update") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
