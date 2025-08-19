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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.example.campus_lost_found.utils.SupabaseManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val supabaseManager = SupabaseManager.getInstance()
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
    private lateinit var skipButton: Button

    // Default credentials for testing
    private val defaultEmail = "test@campus.com"
    private val defaultPassword = "campus123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already signed in
        if (supabaseManager.isUserSignedIn()) {
            Log.d(TAG, "User already signed in, redirecting to MainActivity")
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_login)
        initializeViews()
        setupEventListeners()
    }

    private fun initializeViews() {
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

            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}")
            showErrorDialog("Failed to initialize login screen")
        }
    }

    private fun setupEventListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInputs(email, password)) {
                signInWithEmail(email, password)
            }
        }

        signupButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInputs(email, password)) {
                signUpWithEmail(email, password)
            }
        }

        useDefaultEmailText.setOnClickListener {
            emailInput.setText(defaultEmail)
            passwordInput.setText(defaultPassword)
            Toast.makeText(this, "Default credentials filled", Toast.LENGTH_SHORT).show()
        }

        skipButton.setOnClickListener {
            showSkipConfirmationDialog()
        }

        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            return false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            return false
        }

        // Clear errors
        emailLayout.error = null
        passwordLayout.error = null
        return true
    }

    private fun signInWithEmail(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val result = supabaseManager.signIn(email, password)

                result.onSuccess { userId ->
                    Log.d(TAG, "Sign in successful for user: $userId")
                    runOnUiThread {
                        setLoading(false)
                        Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                        startMainActivity()
                    }
                }.onFailure { exception ->
                    runOnUiThread {
                        setLoading(false)
                        handleAuthError(exception, "Sign In Failed")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    handleAuthError(e, "Sign In Error")
                }
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val result = supabaseManager.signUp(email, password)

                result.onSuccess { message ->
                    Log.d(TAG, "Sign up successful")
                    runOnUiThread {
                        setLoading(false)
                        showSuccessDialog("Account Created", message)
                    }
                }.onFailure { exception ->
                    runOnUiThread {
                        setLoading(false)
                        handleAuthError(exception, "Sign Up Failed")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    handleAuthError(e, "Sign Up Error")
                }
            }
        }
    }

    private fun handleAuthError(exception: Throwable, title: String) {
        Log.e(TAG, "$title: ${exception.message}")

        val errorMessage = when {
            exception.message?.contains("Invalid login credentials") == true ->
                "Invalid email or password. Please check your credentials."
            exception.message?.contains("Email not confirmed") == true ->
                "Please check your email and click the confirmation link before signing in."
            exception.message?.contains("User already registered") == true ->
                "This email is already registered. Please sign in instead."
            else -> exception.message ?: "An unexpected error occurred"
        }

        showErrorDialog("$title\n\n$errorMessage")
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        signupButton.isEnabled = !isLoading

        if (isLoading) {
            loginButton.text = "Signing in..."
            signupButton.text = "Creating account..."
        } else {
            loginButton.text = "Sign In"
            signupButton.text = "Sign Up"
        }
    }

    private fun showSkipConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Skip Authentication")
            .setMessage("Are you sure you want to continue without signing in? Some features may be limited.")
            .setPositiveButton("Continue") { _, _ ->
                startMainActivity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showForgotPasswordDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Forgot Password")
            .setMessage("Password reset functionality will be available soon. For now, please contact support or use the default credentials.")
            .setPositiveButton("Use Default") { _, _ ->
                emailInput.setText(defaultEmail)
                passwordInput.setText(defaultPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // For now, auto-sign in after successful registration
                signInWithEmail(emailInput.text.toString().trim(), passwordInput.text.toString().trim())
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in when activity starts
        if (supabaseManager.isUserSignedIn()) {
            Log.d(TAG, "User already signed in, redirecting to MainActivity")
            startMainActivity()
        }
    }
}
