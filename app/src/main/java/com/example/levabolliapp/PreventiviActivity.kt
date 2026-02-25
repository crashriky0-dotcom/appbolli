package com.example.levabolliapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PreventiviActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var txtEmpty: TextView
    private var preventivi: List<MainActivity.Preventivo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivi)

        listView = findViewById(R.id.listPreventivi)
        txtEmpty = findViewById(R.id.txtPreventiviEmpty)

        val main = MainActivityHolder.instance
        if (main == null) {
            txtEmpty.text = getString(R.string.error_main_not_available)
            txtEmpty.visibility = android.view.View.VISIBLE
            return
        }

        preventivi = main.loadPreventiviForActivity()

        if (preventivi.isEmpty()) {
            txtEmpty.visibility = android.view.View.VISIBLE
        } else {
            txtEmpty.visibility = android.view.View.GONE
        }

        val items = preventivi.map {
            val titolo = if (it.marca.isNotEmpty() || it.modello.isNotEmpty()) {
                "${it.marca} ${it.modello}".trim()
            } else {
                getString(R.string.quote_no_car)
            }
            val sotto = "${it.data} - ${getString(R.string.total_applied_value, it.totaleApplicato)}"
            "$titolo\n$sotto"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val p = preventivi[position]
            val data = Intent()
            data.putExtra("preventivo_id", p.id)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val p = preventivi[position]
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_quote_title)
                .setMessage(R.string.delete_quote_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    main.deletePreventivoById(p.id)
                    preventivi = main.loadPreventiviForActivity()
                    recreate()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }
}
