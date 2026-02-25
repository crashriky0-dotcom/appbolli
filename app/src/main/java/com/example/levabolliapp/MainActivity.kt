package com.example.levabolliapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Rotella impostazioni (se presente nel layout)
        findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Impostazioni")
            .setMessage("Impostazioni base (placeholder).")
            .setPositiveButton("OK", null)
            .show()
    }
}
