package com.example.evaluacion

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.w3c.dom.Text

class ConexionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_conexion)

        val txtTituloConexion = findViewById<TextView>(R.id.txtTituloConexion)
        val txtEstadoConexion = findViewById<TextView>(R.id.txtEstadoConexion)
        val txtDispositivo = findViewById<TextView>(R.id.txtDispositivo)

        val btnBluettoth = findViewById<Button>(R.id.btnBluetooth)
        val btnWifi = findViewById<Button>(R.id.btnWiFi)
        val btnDesconectar = findViewById<Button>(R.id.btnDesconectar)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        val origen = intent.getStringExtra("ORIGEN")
        txtTituloConexion.text = "Conexion - $origen"

        btnBluettoth.setOnClickListener {
            txtEstadoConexion.text = "Estado: Modo Bluetooth seleccionado"
            txtDispositivo.text = "Dispositivo: ESP32 pendiente"
        }
        btnWifi.setOnClickListener {
            txtEstadoConexion.text = "Estado: Modo Wi-Fi seleccionado"
            txtDispositivo.text = "Dispositivo: ESP32 pendiente"
        }
        btnDesconectar.setOnClickListener {
            txtEstadoConexion.text = "Estado: Sin Conexion"
            txtDispositivo.text = "Dispositivo: -- "
        }
        btnVolver.setOnClickListener {
            finish()
        }

    }
}