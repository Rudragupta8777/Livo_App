package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.livo.works.R
import com.livo.works.ViewModel.ForgotPasswordViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPassword : AppCompatActivity() {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        enableEdgeToEdge()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.sendOtp(email)
            } else {
                etEmail.error = "Enter email"
            }
        }

        lifecycleScope.launch {
            viewModel.initiateState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnSend.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        btnSend.isEnabled = true

                        val intent = Intent(this@ForgotPassword, ResetPassword::class.java)
                        intent.putExtra("REG_ID", state.data?.registrationId)
                        intent.putExtra("RESEND_TIME", state.data?.nextResendAt)
                        intent.putExtra("EMAIL", etEmail.text.toString())
                        startActivity(intent)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnSend.isEnabled = true
                        Toast.makeText(this@ForgotPassword, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}