package com.livo.works.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.livo.works.R

class Developers : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developers)
        enableEdgeToEdge()


        // Rudra's Links
        findViewById<MaterialButton>(R.id.btnRudraGithub).setOnClickListener {
            openUrl("https://github.com/Rudragupta8777")
        }
        findViewById<MaterialButton>(R.id.btnRudraLinkedIn).setOnClickListener {
            openUrl("https://www.linkedin.com/in/rudra-gupta-36827828b/")
        }

        // Pratham's Links
        findViewById<MaterialButton>(R.id.btnPrathamGithub).setOnClickListener {
            openUrl("https://github.com/pratham-developer")
        }
        findViewById<MaterialButton>(R.id.btnPrathamLinkedIn).setOnClickListener {
            openUrl("https://www.linkedin.com/in/pratham-khanduja/")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
}