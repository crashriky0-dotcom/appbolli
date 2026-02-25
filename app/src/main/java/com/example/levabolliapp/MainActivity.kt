package com.example.levabolliapp

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.levabolliapp.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val PREFS_NAME = "levabolli_prefs"
        private const val KEY_CUSTOM_LISTINO = "custom_listino"
        private const val KEY_PREVENTIVI = "preventivi"
        private const val KEY_AUTO_SHARE_PDF = "auto_share_pdf"
        private const val REQUEST_PREVENTIVI = 2001
    }

    // -------- LISTINO --------

    data class PrezzoBollo(
        val minBolli: Int,
        val maxBolli: Int,
        val misura: Int,   // 1 = <10mm, 2 = <25mm, 3 = <45mm
        val prezzo: Int
    )

    private val defaultListino = listOf(
        // MISURA 1 (<10 mm)
        PrezzoBollo(1, 2, 1, 44),
        PrezzoBollo(3, 5, 1, 71),
        PrezzoBollo(6, 10, 1, 99),
        PrezzoBollo(11, 20, 1, 137),
        PrezzoBollo(21, 30, 1, 181),
        PrezzoBollo(31, 40, 1, 201),
        PrezzoBollo(41, 60, 1, 217),
        PrezzoBollo(61, 80, 1, 291),
        PrezzoBollo(81, 100, 1, 346),
        PrezzoBollo(101, 120, 1, 371),
        PrezzoBollo(121, 150, 1, 392),
        PrezzoBollo(151, 180, 1, 418),
        PrezzoBollo(181, 210, 1, 478),
        PrezzoBollo(211, 240, 1, 510),
        PrezzoBollo(241, 270, 1, 555),
        PrezzoBollo(271, 300, 1, 695),
        PrezzoBollo(301, 350, 1, 737),

        // MISURA 2 (<25 mm)
        PrezzoBollo(1, 2, 2, 79),
        PrezzoBollo(3, 5, 2, 107),
        PrezzoBollo(6, 10, 2, 151),
        PrezzoBollo(11, 20, 2, 184),
        PrezzoBollo(21, 30, 2, 245),
        PrezzoBollo(31, 40, 2, 312),
        PrezzoBollo(41, 60, 2, 380),
        PrezzoBollo(61, 80, 2, 459),
        PrezzoBollo(81, 100, 2, 492),
        PrezzoBollo(101, 120, 2, 562),
        PrezzoBollo(121, 150, 2, 618),
        PrezzoBollo(151, 180, 2, 686),
        PrezzoBollo(181, 210, 2, 735),
        PrezzoBollo(211, 240, 2, 801),
        PrezzoBollo(241, 270, 2, 862),
        PrezzoBollo(271, 300, 2, 908),
        PrezzoBollo(301, 350, 2, 979),

        // MISURA 3 (<45 mm)
        PrezzoBollo(1, 2, 3, 123),
        PrezzoBollo(3, 5, 3, 158),
        PrezzoBollo(6, 10, 3, 201),
        PrezzoBollo(11, 20, 3, 241),
        PrezzoBollo(21, 30, 3, 314),
        PrezzoBollo(31, 40, 3, 382),
        PrezzoBollo(41, 60, 3, 459),
        PrezzoBollo(61, 80, 3, 566),
        PrezzoBollo(81, 100, 3, 639),
        PrezzoBollo(101, 120, 3, 717),
        PrezzoBollo(121, 150, 3, 774),
        PrezzoBollo(151, 180, 3, 827),
        PrezzoBollo(181, 210, 3, 921),
        PrezzoBollo(211, 240, 3, 992),
        PrezzoBollo(241, 270, 3, 997),
        PrezzoBollo(271, 300, 3, 1064),
        PrezzoBollo(301, 350, 3, 1138),
        PrezzoBollo(351, 500, 3, 1221)
    )

    private var currentListino: List<PrezzoBollo> = defaultListino

    // -------- PANNELLI --------

    data class PanelData(
        val nome: String,
        var numBolli: Int = 0,
        var diametroMm: Double = 0.0,
        var alluminio: Boolean = false,
        var numSportellate: Int = 0,
        var diametroSportellataMm: Double = 0.0,
        var prezzoConsigliato: Int = 0,
        var prezzoApplicato: Int = 0
    )

    private val panels = mutableMapOf<Int, PanelData>()

    // -------- PREVENTIVI --------

    data class Preventivo(
        val id: Long,
        val tecnico: String,
        val data: String,
        val marca: String,
        val modello: String,
        val targa: String,
        val totaleConsigliato: Int,
        val totaleApplicato: Int,
        val pannelli: List<PanelData>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCustomListino()

        // Mappa ID pulsante -> pannello
        panels[binding.btnCofano.id] = PanelData(getString(R.string.panel_hood))
        panels[binding.btnTetto.id] = PanelData(getString(R.string.panel_roof))
        panels[binding.btnBaule.id] = PanelData(getString(R.string.panel_trunk))

        panels[binding.btnParafangoAntSx.id] = PanelData(getString(R.string.panel_fender_fl))
        panels[binding.btnPortaAntSx.id] = PanelData(getString(R.string.panel_door_fl))
        panels[binding.btnPortaPostSx.id] = PanelData(getString(R.string.panel_door_rl))
        panels[binding.btnParafangoPostSx.id] = PanelData(getString(R.string.panel_fender_rl))

        panels[binding.btnParafangoAntDx.id] = PanelData(getString(R.string.panel_fender_fr))
        panels[binding.btnPortaAntDx.id] = PanelData(getString(R.string.panel_door_fr))
        panels[binding.btnPortaPostDx.id] = PanelData(getString(R.string.panel_door_rr))
        panels[binding.btnParafangoPostDx.id] = PanelData(getString(R.string.panel_fender_rr))

        // Click pannelli
        listOf(
            binding.btnCofano,
            binding.btnTetto,
            binding.btnBaule,
            binding.btnParafangoAntSx,
            binding.btnPortaAntSx,
            binding.btnPortaPostSx,
            binding.btnParafangoPostSx,
            binding.btnParafangoAntDx,
            binding.btnPortaAntDx,
            binding.btnPortaPostDx,
            binding.btnParafangoPostDx
        ).forEach { button ->
            button.setOnClickListener {
                val panel = panels[button.id]!!
                showPanelDialog(panel, button)
            }
        }

        // Misura da foto
        binding.btnMisuraFoto.setOnClickListener {
            val intent = Intent(this, MeasureActivity::class.java)
            startActivity(intent)
        }

        // Listino personalizzato
        binding.btnListino.setOnClickListener {
            val intent = Intent(this, ListinoActivity::class.java)
            startActivity(intent)
        }


        // Informazioni / Crediti
        binding.btnInfo.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        
        // Impostazioni (rotella)
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Salva preventivo
        binding.btnSalvaPreventivo.setOnClickListener {
            salvaPreventivo()
        }

        // Preventivi salvati
        binding.btnPreventiviSalvati.setOnClickListener {
            val intent = Intent(this, PreventiviActivity::class.java)
            startActivityForResult(intent, REQUEST_PREVENTIVI)
        }

        // Esporta PDF
        binding.btnEsportaPdf.setOnClickListener {
            exportPdf()
        }

        updateTotali()
    }

    // --- LISTINO CUSTOM ---

    private fun loadCustomListino() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CUSTOM_LISTINO, null)
        if (json.isNullOrEmpty()) {
            currentListino = defaultListino
            return
        }
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<PrezzoBollo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PrezzoBollo(
                        o.getInt("min"),
                        o.getInt("max"),
                        o.getInt("misura"),
                        o.getInt("prezzo")
                    )
                )
            }
            if (list.isNotEmpty()) {
                currentListino = list
            } else {
                currentListino = defaultListino
            }
        } catch (e: Exception) {
            currentListino = defaultListino
        }
    }

    fun saveCustomListinoFromActivity(json: String) {
        // chiamata da ListinoActivity tramite instance (semplice approccio)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_LISTINO, json).apply()
        loadCustomListino()
        ricalcolaPannelli()
    }

    private fun ricalcolaPannelli() {
        panels.values.forEach { panel ->
            if (panel.numBolli > 0 || panel.numSportellate > 0) {
                val prezzoGrandine = calcolaPrezzoGrandine(panel.numBolli, panel.diametroMm, panel.alluminio)
                val prezzoSportellate = calcolaPrezzoSportellate(panel.numSportellate, panel.diametroSportellataMm, panel.alluminio)
                val cons = (prezzoGrandine ?: 0) + (prezzoSportellate ?: 0)
                if (cons > 0 && panel.prezzoApplicato == panel.prezzoConsigliato) {
                    // se prima non era stato modificato dall'utente, aggiorno anche l'applicato
                    panel.prezzoApplicato = cons
                }
                panel.prezzoConsigliato = cons
            }
        }
        updateTotali()
    }

    // --- calcolo prezzo grandine (con +30% alluminio) ---

    private fun calcolaPrezzoGrandine(numBolli: Int, diametroMm: Double, isAlluminio: Boolean): Int? {
        if (numBolli <= 0 || diametroMm <= 0.0) return null

        val misura = when {
            diametroMm < 10.0 -> 1
            diametroMm < 25.0 -> 2
            diametroMm < 45.0 -> 3
            else -> 3 // consideriamo fascia massima
        }

        val voce = currentListino.firstOrNull {
            numBolli >= it.minBolli && numBolli <= it.maxBolli && it.misura == misura
        } ?: return null

        val base = voce.prezzo
        return if (isAlluminio) {
            (base * 1.3).roundToInt()
        } else {
            base
        }
    }

    // --- calcolo prezzo sportellate (idea di prezzo) ---

    private fun calcolaPrezzoSportellate(
        numSportellate: Int,
        diametroMm: Double,
        isAlluminio: Boolean
    ): Int? {
        if (numSportellate <= 0) return null

        // se il diametro non è indicato, assumiamo misura media (2)
        val misura = when {
            diametroMm <= 0.0 -> 2
            diametroMm < 10.0 -> 1
            diametroMm < 25.0 -> 2
            else -> 3
        }

        val bolliEqPerSportellata = when (misura) {
            1 -> 5
            2 -> 10
            else -> 20
        }

        val numBolliEq = numSportellate * bolliEqPerSportellata

        val voce = currentListino.firstOrNull {
            numBolliEq >= it.minBolli && numBolliEq <= it.maxBolli && it.misura == misura
        } ?: return null

        val base = voce.prezzo
        return if (isAlluminio) {
            (base * 1.3).roundToInt()
        } else {
            base
        }
    }

    // --- dialog per inserire dati pannello ---

    private fun showPanelDialog(panel: PanelData, button: Button) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_panel, null)

        val txtName = dialogView.findViewById<TextView>(R.id.txtPanelName)
        val edtNumBolli = dialogView.findViewById<EditText>(R.id.edtNumBolli)
        val edtDiametro = dialogView.findViewById<EditText>(R.id.edtDiametro)
        val chkAlluminio = dialogView.findViewById<CheckBox>(R.id.chkAlluminio)
        val edtNumSportellate = dialogView.findViewById<EditText>(R.id.edtNumSportellate)
        val edtDiametroSportellata = dialogView.findViewById<EditText>(R.id.edtDiametroSportellata)
        val txtPrezzoConsigliatoPanel = dialogView.findViewById<TextView>(R.id.txtPrezzoConsigliatoPanel)
        val edtPrezzoApplicatoPanel = dialogView.findViewById<EditText>(R.id.edtPrezzoApplicatoPanel)

        txtName.text = panel.nome

        if (panel.numBolli > 0) edtNumBolli.setText(panel.numBolli.toString())
        if (panel.diametroMm > 0) edtDiametro.setText(panel.diametroMm.toString())
        chkAlluminio.isChecked = panel.alluminio
        if (panel.numSportellate > 0) edtNumSportellate.setText(panel.numSportellate.toString())
        if (panel.diametroSportellataMm > 0) edtDiametroSportellata.setText(panel.diametroSportellataMm.toString())

        if (panel.prezzoConsigliato > 0) {
            txtPrezzoConsigliatoPanel.text = getString(R.string.panel_suggested_price_value, panel.prezzoConsigliato)
        }
        if (panel.prezzoApplicato > 0) {
            edtPrezzoApplicatoPanel.setText(panel.prezzoApplicato.toString())
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                val numBolli = edtNumBolli.text.toString().toIntOrNull() ?: 0
                val diametro = edtDiametro.text.toString().toDoubleOrNull() ?: 0.0
                val isAlluminio = chkAlluminio.isChecked
                val numSportellate = edtNumSportellate.text.toString().toIntOrNull() ?: 0
                val diametroSport = edtDiametroSportellata.text.toString().toDoubleOrNull() ?: 0.0

                panel.numBolli = numBolli
                panel.diametroMm = diametro
                panel.alluminio = isAlluminio
                panel.numSportellate = numSportellate
                panel.diametroSportellataMm = diametroSport

                val prezzoGrandine = calcolaPrezzoGrandine(numBolli, diametro, isAlluminio) ?: 0
                val prezzoSportellate = calcolaPrezzoSportellate(numSportellate, diametroSport, isAlluminio) ?: 0
                val cons = prezzoGrandine + prezzoSportellate

                panel.prezzoConsigliato = cons

                val prezzoAppManuale = edtPrezzoApplicatoPanel.text.toString().toIntOrNull()
                panel.prezzoApplicato = when {
                    prezzoAppManuale != null && prezzoAppManuale > 0 -> prezzoAppManuale
                    cons > 0 -> cons
                    else -> 0
                }

                if (panel.prezzoApplicato > 0) {
                    button.text = "${panel.nome}\n€ ${panel.prezzoApplicato}"
                } else {
                    button.text = panel.nome
                }

                updateTotali()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateTotali() {
        val totCons = panels.values.sumOf { it.prezzoConsigliato }
        val totApp = panels.values.sumOf { it.prezzoApplicato }
        val sconto = (totCons - totApp).coerceAtLeast(0)
        val scontoPerc = if (totCons > 0) (sconto.toDouble() / totCons.toDouble() * 100.0) else 0.0

        binding.txtTotConsigliato.text = getString(R.string.total_suggested_value, totCons)
        binding.txtTotApplicato.text = getString(R.string.total_applied_value, totApp)
        binding.txtTotSconto.text = getString(R.string.total_discount_value, sconto, String.format("%.1f", scontoPerc))
    }

    // --- PREVENTIVI ---

    private fun salvaPreventivo() {
        val totCons = panels.values.sumOf { it.prezzoConsigliato }
        val totApp = panels.values.sumOf { it.prezzoApplicato }
        if (totCons == 0 && totApp == 0) {
            Toast.makeText(this, R.string.no_data_to_save, Toast.LENGTH_SHORT).show()
            return
        }

        val id = System.currentTimeMillis()
        val tecnico = binding.edtTecnico.text.toString()
        val data = binding.edtData.text.toString()
        val marca = binding.edtMarca.text.toString()
        val modello = binding.edtModello.text.toString()
        val targa = binding.edtTarga.text.toString()

        val pannelliList = panels.values.toList()

        val preventivo = Preventivo(
            id,
            tecnico,
            data,
            marca,
            modello,
            targa,
            totCons,
            totApp,
            pannelliList
        )

        val arr = loadPreventiviJson()
        arr.put(preventivoToJson(preventivo))
        savePreventiviJson(arr)

        Toast.makeText(this, R.string.quote_saved, Toast.LENGTH_SHORT).show()
    }

    private fun loadPreventiviJson(): JSONArray {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREVENTIVI, null) ?: return JSONArray()
        return try {
            JSONArray(json)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun savePreventiviJson(arr: JSONArray) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREVENTIVI, arr.toString()).apply()
    }

    private fun preventivoToJson(p: Preventivo): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("tecnico", p.tecnico)
        o.put("data", p.data)
        o.put("marca", p.marca)
        o.put("modello", p.modello)
        o.put("targa", p.targa)
        o.put("totCons", p.totaleConsigliato)
        o.put("totApp", p.totaleApplicato)

        val pannelliArr = JSONArray()
        p.pannelli.forEach { panel ->
            val po = JSONObject()
            po.put("nome", panel.nome)
            po.put("numBolli", panel.numBolli)
            po.put("diametroMm", panel.diametroMm)
            po.put("alluminio", panel.alluminio)
            po.put("numSportellate", panel.numSportellate)
            po.put("diametroSport", panel.diametroSportellataMm)
            po.put("prezzoCons", panel.prezzoConsigliato)
            po.put("prezzoApp", panel.prezzoApplicato)
            pannelliArr.put(po)
        }
        o.put("pannelli", pannelliArr)
        return o
    }

    private fun jsonToPreventivo(o: JSONObject): Preventivo {
        val id = o.getLong("id")
        val tecnico = o.optString("tecnico", "")
        val data = o.optString("data", "")
        val marca = o.optString("marca", "")
        val modello = o.optString("modello", "")
        val targa = o.optString("targa", "")
        val totCons = o.optInt("totCons", 0)
        val totApp = o.optInt("totApp", 0)

        val pannelliArr = o.optJSONArray("pannelli") ?: JSONArray()
        val pannelliList = mutableListOf<PanelData>()
        for (i in 0 until pannelliArr.length()) {
            val po = pannelliArr.getJSONObject(i)
            pannelliList.add(
                PanelData(
                    po.getString("nome"),
                    po.optInt("numBolli", 0),
                    po.optDouble("diametroMm", 0.0),
                    po.optBoolean("alluminio", false),
                    po.optInt("numSportellate", 0),
                    po.optDouble("diametroSport", 0.0),
                    po.optInt("prezzoCons", 0),
                    po.optInt("prezzoApp", 0)
                )
            )
        }

        return Preventivo(
            id,
            tecnico,
            data,
            marca,
            modello,
            targa,
            totCons,
            totApp,
            pannelliList
        )
    }

    fun loadPreventiviForActivity(): List<Preventivo> {
        val arr = loadPreventiviJson()
        val list = mutableListOf<Preventivo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(jsonToPreventivo(o))
        }
        return list.sortedByDescending { it.id }
    }

    fun deletePreventivoById(id: Long) {
        val arr = loadPreventiviJson()
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("id") != id) {
                newArr.put(o)
            }
        }
        savePreventiviJson(newArr)
    }

    private fun applyPreventivo(p: Preventivo) {
        binding.edtTecnico.setText(p.tecnico)
        binding.edtData.setText(p.data)
        binding.edtMarca.setText(p.marca)
        binding.edtModello.setText(p.modello)
        binding.edtTarga.setText(p.targa)

        // reset pannelli
        panels.values.forEach { panel ->
            panel.numBolli = 0
            panel.diametroMm = 0.0
            panel.alluminio = false
            panel.numSportellate = 0
            panel.diametroSportellataMm = 0.0
            panel.prezzoConsigliato = 0
            panel.prezzoApplicato = 0
        }

        // riapplica i pannelli per nome
        p.pannelli.forEach { pPanel ->
            val target = panels.values.firstOrNull { it.nome == pPanel.nome }
            if (target != null) {
                target.numBolli = pPanel.numBolli
                target.diametroMm = pPanel.diametroMm
                target.alluminio = pPanel.alluminio
                target.numSportellate = pPanel.numSportellate
                target.diametroSportellataMm = pPanel.diametroSportellataMm
                target.prezzoConsigliato = pPanel.prezzoConsigliato
                target.prezzoApplicato = pPanel.prezzoApplicato
            }
        }

        // aggiorna testi pulsanti pannelli
        listOf(
            binding.btnCofano,
            binding.btnTetto,
            binding.btnBaule,
            binding.btnParafangoAntSx,
            binding.btnPortaAntSx,
            binding.btnPortaPostSx,
            binding.btnParafangoPostSx,
            binding.btnParafangoAntDx,
            binding.btnPortaAntDx,
            binding.btnPortaPostDx,
            binding.btnParafangoPostDx
        ).forEach { button ->
            val panel = panels[button.id]!!
            if (panel.prezzoApplicato > 0) {
                button.text = "${panel.nome}\n€ ${panel.prezzoApplicato}"
            } else {
                button.text = panel.nome
            }
        }

        updateTotali()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PREVENTIVI && resultCode == RESULT_OK && data != null) {
            val id = data.getLongExtra("preventivo_id", -1L)
            if (id > 0) {
                val list = loadPreventiviForActivity()
                val p = list.firstOrNull { it.id == id }
                if (p != null) {
                    applyPreventivo(p)
                }
            }
        }
    }

    // --- esportazione PDF ---

    private fun exportPdf() {
        val totCons = panels.values.sumOf { it.prezzoConsigliato }
        val totApp = panels.values.sumOf { it.prezzoApplicato }
        if (totCons == 0 && totApp == 0) {
            Toast.makeText(this, R.string.no_data_to_export, Toast.LENGTH_SHORT).show()
            return
        }

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 circa
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 12f

        var y = 30f

        canvas.drawText(getString(R.string.pdf_title), 30f, y, paint)
        y += 20

        canvas.drawText(getString(R.string.technician_label, binding.edtTecnico.text), 30f, y, paint); y += 15
        canvas.drawText(getString(R.string.date_label, binding.edtData.text), 30f, y, paint); y += 15
        canvas.drawText(getString(R.string.brand_label, binding.edtMarca.text), 30f, y, paint); y += 15
        canvas.drawText(getString(R.string.model_label, binding.edtModello.text), 30f, y, paint); y += 15
        canvas.drawText(getString(R.string.plate_label, binding.edtTarga.text), 30f, y, paint); y += 25

        canvas.drawText(getString(R.string.pdf_panels_title), 30f, y, paint); y += 20

        panels.values.forEach { p ->
            if (p.prezzoApplicato > 0 || p.prezzoConsigliato > 0) {
                val allu = if (p.alluminio) " (${getString(R.string.aluminum_short)})" else ""
                val grandineDesc = if (p.numBolli > 0 && p.diametroMm > 0) {
                    " - " + getString(R.string.pdf_hail_desc, p.numBolli, p.diametroMm)
                } else {
                    ""
                }
                val sportellateDesc = if (p.numSportellate > 0) {
                    val dim = if (p.diametroSportellataMm > 0) " (${p.diametroSportellataMm} mm)" else ""
                    " - " + getString(R.string.pdf_door_ding_desc, p.numSportellate, dim)
                } else {
                    ""
                }
                val riga = "${p.nome}$allu$grandineDesc$sportellateDesc -> " +
                        getString(R.string.pdf_panel_price, p.prezzoConsigliato, p.prezzoApplicato)
                canvas.drawText(riga, 30f, y, paint)
                y += 15

                // nuova pagina se serve
                if (y > 800f) {
                    doc.finishPage(page)
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 30f
                }
            }
        }

        y += 20
        paint.textSize = 14f
        canvas.drawText(getString(R.string.total_suggested_value, totCons), 30f, y, paint); y += 18
        canvas.drawText(getString(R.string.total_applied_value, totApp), 30f, y, paint); y += 18
        val sconto = (totCons - totApp).coerceAtLeast(0)
        val scontoPerc = if (totCons > 0) (sconto.toDouble() / totCons.toDouble() * 100.0) else 0.0
        canvas.drawText(
            getString(R.string.total_discount_value, sconto, String.format("%.1f", scontoPerc)),
            30f,
            y,
            paint
        )

        doc.finishPage(page)

        // Salvataggio in Download/Levabolli (più comodo di Android/data/...)
        val fileName = "preventivo_${System.currentTimeMillis()}.pdf"
        var outUri: Uri? = null

        try {
            outUri = createPdfInDownloads(fileName) { os ->
                doc.writeTo(os)
            }
            Toast.makeText(this, getString(R.string.pdf_saved_downloads), Toast.LENGTH_LONG).show()

            // Se attivo, apro subito la condivisione
            if (getAutoSharePdf() && outUri != null) {
                sharePdf(outUri)
            } else if (outUri != null) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.share_pdf)
                    .setMessage(R.string.pdf_saved_downloads)
                    .setPositiveButton(R.string.share_pdf) { _, _ -> sharePdf(outUri) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.pdf_error, e.message ?: ""), Toast.LENGTH_LONG).show()
        } finally {
            doc.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MainActivityHolder.instance === this) {
            MainActivityHolder.instance = null
        }
    }

}
