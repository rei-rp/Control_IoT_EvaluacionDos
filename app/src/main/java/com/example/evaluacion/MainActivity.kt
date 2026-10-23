package com.example.evaluacion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val ipEsp32 = "192.168.4.1"

    private lateinit var txtEstado: TextView
    private lateinit var btnEncender: Button
    private lateinit var btnApagar: Button
    private lateinit var btnBuzzer: Button
    private lateinit var btnDistancia: Button
    private lateinit var btnConexion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtEstado = findViewById(R.id.txtEstado)
        btnEncender = findViewById(R.id.btnEncender)
        btnApagar = findViewById(R.id.btnApagar)
        btnBuzzer = findViewById(R.id.btnBuzzer)
        btnDistancia = findViewById(R.id.btnDistancia)
        btnConexion = findViewById(R.id.btnConfigurar)

        btnEncender.setOnClickListener {
            enviarComando("/led/on")
        }

        btnApagar.setOnClickListener {
            enviarComando("/led/off")
        }

        btnBuzzer.setOnClickListener {
            txtEstado.text = "Estado: Buzzer activado"
        }

        btnDistancia.setOnClickListener {
            startActivity(Intent(this, DistanciaActivity::class.java))
        }

        btnConexion.setOnClickListener {
            val intent = Intent(this, ConexionActivity::class.java)
            intent.putExtra("ORIGEN", "Control IoT")
            startActivity(intent)
        }
    }

    private fun enviarComando(ruta: String) {
        txtEstado.text = "Enviando comando...."
        Thread {
            var conexion: HttpURLConnection? = null
            try {
                val url = URL("http://$ipEsp32$ruta")
                conexion = url.openConnection() as HttpURLConnection

                conexion.requestMethod = "GET"
                conexion.connectTimeout = 3000
                conexion.readTimeout = 3000

                val codigoRespuesta = conexion.responseCode

                if (codigoRespuesta == 200) {
                    val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }
                    runOnUiThread { txtEstado.text = respuesta }
                } else {
                    runOnUiThread { txtEstado.text = "Error HTTP: $codigoRespuesta" }
                }

            } catch (e: Exception) {
                runOnUiThread { txtEstado.text = "No se pudo conectar a Esp32" }

            } finally {
                conexion?.disconnect()
            }
        }.start()
    }
}
