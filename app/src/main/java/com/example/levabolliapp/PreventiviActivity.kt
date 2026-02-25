package com.example.levabolliapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class PreventiviActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivi)

        val listView: ListView = findViewById(R.id.listViewPreventivi)

        // Lista temporanea vuota (placeholder per compilazione)
        val preventivi: List<String> = listOf("Nessun preventivo salvato")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            preventivi
        )

        listView.adapter = adapter
    }
}
