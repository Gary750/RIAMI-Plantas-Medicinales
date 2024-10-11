package com.gary.riamiplantasmedicinales

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics

class ActivitySplash : AppCompatActivity() {

    // Variable que define el tiempo de espera antes de iniciar la actividad principal (2000 milisegundos)
    val splash_tiempo = 2000

    // Método onCreate que se llama cuando se crea la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Indicar el XML que se usará para esta actividad (layout)
        setContentView(R.layout.activity_splash)

        // Crear un Handler que se ejecutará después de un retraso definido por splash_tiempo
        Handler().postDelayed({
            // Después de transcurrido el tiempo, iniciar la actividad principal
            val principal = Intent(this, Login::class.java) // Crear un Intent para iniciar la actividad Login
            startActivity(principal) // Iniciar la actividad Login
            finish() // Finalizar la actividad Splash para que no se pueda volver a ella
        }, splash_tiempo.toLong()) // Convertir splash_tiempo a Long para el método postDelayed

        // Inicializar Firebase Analytics para realizar seguimiento de eventos
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle() // Crear un Bundle para almacenar parámetros del evento
        // Agregar un mensaje al Bundle para el seguimiento
        bundle.putString("message", "Integracion con FireBase Completa")
        // Registrar un evento de inicialización de pantalla con Firebase Analytics
        analytics.logEvent("InitScreen", bundle)
    }
}