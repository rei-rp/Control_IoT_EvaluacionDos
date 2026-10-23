package com.example.evaluacion

import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class DistanciaActivity : AppCompatActivity() {

    private val ipEsp32 = "192.168.4.1"
    private val intervaloMs = 500L

    private lateinit var layoutPrincipal: LinearLayout
    private lateinit var txtDistancia: TextView
    private lateinit var txtAlerta: TextView
    private lateinit var txtUmbral: TextView
    private lateinit var seekUmbral: SeekBar
    private lateinit var btnIniciar: Button
    private lateinit var btnVolver: Button

    private val handler = Handler(Looper.getMainLooper())
    private var monitoreando = false
    private var umbralCm = 20
    private var enAlerta = false
    private var tono: ToneGenerator? = null

    private val tareaLectura = object : Runnable {
        override fun run() {
            if (!monitoreando) return
            leerDistancia()
            handler.postDelayed(this, intervaloMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distancia)

        layoutPrincipal = findViewById(R.id.main)
        txtDistancia = findViewById(R.id.txtDistancia)
        txtAlerta = findViewById(R.id.txtAlerta)
        txtUmbral = findViewById(R.id.txtUmbral)
        seekUmbral = findViewById(R.id.seekUmbral)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnVolver = findViewById(R.id.btnVolverDistancia)

        tono = try {
            ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            null
        }

        seekUmbral.progress = umbralCm
        txtUmbral.text = "Umbral de alerta: $umbralCm cm"

        seekUmbral.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progreso: Int, usuario: Boolean) {
                umbralCm = if (progreso < 5) 5 else progreso
                txtUmbral.text = "Umbral de alerta: $umbralCm cm"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                // Se envia al ESP32 una sola vez, al soltar el control
                enviarUmbral(umbralCm)
            }
        })

        btnIniciar.setOnClickListener {
            if (monitoreando) detener() else iniciar()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun iniciar() {
        monitoreando = true
        btnIniciar.text = "DETENER"
        txtAlerta.text = "Midiendo..."
        handler.post(tareaLectura)
    }

    private fun detener() {
        monitoreando = false
        handler.removeCallbacks(tareaLectura)
        btnIniciar.text = "INICIAR"
        txtAlerta.text = "Monitoreo detenido"
        quitarAlerta()
    }

    private fun leerDistancia() {
        Thread {
            var conexion: HttpURLConnection? = null
            try {
                val url = URL("http://$ipEsp32/distancia")
                conexion = url.openConnection() as HttpURLConnection
                conexion.requestMethod = "GET"
                conexion.connectTimeout = 1500
                conexion.readTimeout = 1500

                val codigo = conexion.responseCode
                if (codigo == 200) {
                    val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }.trim()
                    val distancia = respuesta.toFloatOrNull()

                    runOnUiThread {
                        if (!monitoreando) return@runOnUiThread
                        if (distancia == null || distancia < 0) {
                            txtDistancia.text = "-- cm"
                            txtAlerta.text = "Sin lectura del sensor"
                            quitarAlerta()
                        } else {
                            actualizarDistancia(distancia)
                        }
                    }
                } else {
                    runOnUiThread { txtAlerta.text = "Error HTTP: $codigo" }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!monitoreando) return@runOnUiThread
                    txtDistancia.text = "-- cm"
                    txtAlerta.text = "No se pudo conectar a Esp32"
                    quitarAlerta()
                }
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }

    private fun actualizarDistancia(distancia: Float) {
        txtDistancia.text = String.format("%.1f cm", distancia)

        if (distancia <= umbralCm) {
            enAlerta = true
            txtAlerta.text = "ALERTA: objeto muy cerca"
            layoutPrincipal.setBackgroundColor(Color.parseColor("#FFCDD2"))
            tono?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        } else {
            quitarAlerta()
            txtAlerta.text = "Distancia segura"
        }
    }

    private fun quitarAlerta() {
        if (enAlerta) {
            enAlerta = false
            tono?.stopTone()
        }
        layoutPrincipal.setBackgroundColor(Color.WHITE)
    }

    private fun enviarUmbral(cm: Int) {
        Thread {
            var conexion: HttpURLConnection? = null
            try {
                val url = URL("http://$ipEsp32/umbral?cm=$cm")
                conexion = url.openConnection() as HttpURLConnection
                conexion.requestMethod = "GET"
                conexion.connectTimeout = 1500
                conexion.readTimeout = 1500
                conexion.responseCode
            } catch (e: Exception) {
                // Si falla, la app sigue alertando con su propio umbral
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        if (monitoreando) detener()
    }

    override fun onDestroy() {
        super.onDestroy()
        tono?.release()
        tono = null
    }
}
