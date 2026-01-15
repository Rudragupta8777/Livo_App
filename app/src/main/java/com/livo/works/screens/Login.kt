package com.livo.works.screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.livo.works.R
import com.livo.works.ViewModel.LoginViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Login : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()

        if (sharedPreferences.getBoolean("is_first_time", true)) {
            sharedPreferences.edit()
                .putBoolean("is_first_time", false)
                .apply()
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvSignup = findViewById<MaterialButton>(R.id.btn_create_acc)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email required"
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                etPass.error = "Password required"
                return@setOnClickListener
            }

            viewModel.login(email, pass)
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        // Observe login state
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnLogin.isEnabled = false
                    }

                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@Login, "Login Successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to Dashboard
                        val intent = Intent(this@Login, Dashboard::class.java)
                        startActivity(intent)
                        finish()
                    }

                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(this@Login, state.message, Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        // Idle state - do nothing
                    }
                }
            }
        }
    }
}