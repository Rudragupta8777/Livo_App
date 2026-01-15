package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.livo.works.R
import com.livo.works.ViewModel.SignupViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Signup : AppCompatActivity() {

    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignup = findViewById<MaterialButton>(R.id.btnSignup)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.initiateSignup(name, email, pass)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.signupState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnSignup.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        val response = state.data // Contains registrationId & nextResendAt

                        val intent = Intent(this@Signup, OtpVerification::class.java).apply {
                            putExtra("REG_ID", response?.registrationId)
                            putExtra("RESEND_TIME", response?.nextResendAt)
                            putExtra("EMAIL", etEmail.text.toString())
                        }
                        startActivity(intent)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnSignup.isEnabled = true
                        Toast.makeText(this@Signup, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }
}