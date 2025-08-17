package com.example.campus_lost_found

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "LoginActivity"

    // UI elements
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var useDefaultEmailText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var googleSignInButton: SignInButton
    private lateinit var skipButton: Button

    // Default credentials
    private val defaultEmail = "sgp.noreplydce@gmail.com"
    private val defaultPassword = "campus123"

    // Modern Activity Result API for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleSignInResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Force apply the dark theme
            setTheme(R.style.Theme_CAMPUS_LOST_FOUND)
            setContentView(R.layout.activity_login)

            // Check if Firebase is properly initialized
            if (!CampusLostFoundApplication.isFirebaseInitialized) {
                Log.w(TAG, "Firebase not initialized, proceeding without authentication")
                showFirebaseUnavailableDialog()
                return
            }

            // Initialize Firebase Auth first
            auth = FirebaseAuth.getInstance()

            // Check if user is already signed in
            if (auth.currentUser != null) {
                startMainActivity()
                return
            }

            // Initialize views safely
            initializeViewsSafely()

            // Set up listeners
            setupListeners()

        } catch (e: Exception) {
            Log.e(TAG, "Critical error during initialization: ${e.message}")
            // If anything fails, just go to main activity
            startMainActivity()
        }
    }

    private fun initializeViewsSafely() {
        try {
            emailLayout = findViewById(R.id.emailLayout)
            passwordLayout = findViewById(R.id.passwordLayout)
            emailInput = findViewById(R.id.emailInput)
            passwordInput = findViewById(R.id.passwordInput)
            loginButton = findViewById(R.id.loginButton)
            signupButton = findViewById(R.id.signupButton)
            forgotPasswordText = findViewById(R.id.forgotPasswordText)
            useDefaultEmailText = findViewById(R.id.useDefaultEmailText)
            progressBar = findViewById(R.id.progressBar)
            skipButton = findViewById(R.id.skipButton)

            // Initialize Google Sign-In safely
            try {
                googleSignInButton = findViewById(R.id.googleSignInButton)
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("810761260274-qpi92nq7i379d91ob1r2v35f7rpljc42.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In initialization failed: ${e.message}")
                // Hide Google Sign-In button if it fails
                findViewById<View>(R.id.googleSignInButton)?.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "View initialization failed: ${e.message}")
            throw e
        }
    }

    private fun setupListeners() {
        // Skip authentication button
        skipButton.setOnClickListener {
            // Skip authentication and continue to main activity
            Toast.makeText(this,
                "Proceeding without authentication",
                Toast.LENGTH_SHORT).show()
            startMainActivity()
        }

        // Google Sign-In button
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Default email login
        useDefaultEmailText.setOnClickListener {
            // First try to sign in with default credentials
            signInWithEmailPassword(defaultEmail, defaultPassword)
        }

        // Regular login button
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInput(email, password)) {
                signInWithEmailPassword(email, password)
            }
        }

        // Sign up button
        signupButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInput(email, password)) {
                createAccount(email, password)
            }
        }

        // Forgot password text
        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            isValid = false
        } else {
            emailLayout.error = null
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordLayout.error = null
        }

        return isValid
    }

    private fun signInWithGoogle() {
        try {
            showProgress(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            showProgress(false)
            Log.e(TAG, "Error starting Google Sign-In: ${e.message}")
            Toast.makeText(
                this,
                "Google Sign-In is not available. Proceeding without authentication.",
                Toast.LENGTH_LONG
            ).show()
            // Just proceed to main activity instead of showing dialog
            startMainActivity()
        }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        showProgress(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    startMainActivity()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)

                    // Provide more specific error messages
                    when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            // If using the default email and it fails, create the account
                            if (email == defaultEmail) {
                                createAccount(defaultEmail, defaultPassword)
                            } else {
                                Toast.makeText(this, "User does not exist. Please create an account first.",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Invalid password. Please try again.",
                                Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()

                            // Show dialog offering alternatives
                            showAuthenticationFailedDialog()
                        }
                    }
                }
            }
    }

    private fun createAccount(email: String, password: String) {
        showProgress(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()

                    // If everything fails, offer to proceed without authentication
                    showAuthenticationFailedDialog()
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = EditText(this)
        emailInput.hint = "Enter your email"

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a password reset link")
            .setView(emailInput)
            .setPositiveButton("Send") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    resetPassword(email)
                } else {
                    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetPassword(email: String) {
        showProgress(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showAuthenticationFailedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Authentication Failed")
            .setMessage("Would you like to continue without signing in? Some features may be limited.")
            .setPositiveButton("Continue Without Sign In") { _, _ ->
                startMainActivity()
            }
            .setNegativeButton("Try Again", null)
            .show()
    }

    private fun showGoogleSignInNotAvailableDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Google Sign-In Not Available")
            .setMessage("Please use email/password authentication or proceed without authentication.")
            .setPositiveButton("Skip Authentication") { _, _ ->
                startMainActivity()
            }
            .setNegativeButton("Try Email/Password", null)
            .show()
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.idToken != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                throw Exception("Google account or token is null")
            }
        } catch (e: ApiException) {
            showProgress(false)
            Log.w(TAG, "Google sign in failed: ${e.statusCode}", e)
            Toast.makeText(this, "Google Sign-In failed. Proceeding without authentication.",
                Toast.LENGTH_SHORT).show()
            startMainActivity()
        } catch (e: Exception) {
            showProgress(false)
            Log.e(TAG, "Unexpected error in Google Sign-In: ${e.message}")
            startMainActivity()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    startMainActivity()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Google authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()

                    // If Google auth fails, show dialog offering alternatives
                    showAuthenticationFailedDialog()
                }
            }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showFirebaseUnavailableDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Authentication Unavailable")
            .setMessage("Firebase services are not available. You can still use the app with limited functionality.")
            .setPositiveButton("Continue") { _, _ ->
                startMainActivity()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
