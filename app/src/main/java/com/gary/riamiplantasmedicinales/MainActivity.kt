package com.gary.riamiplantasmedicinales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Crear las variables de kotlin que relacionen la parte logica con el xml
        var usuario : EditText = findViewById (R.id.email)
        var password : EditText = findViewById(R.id.password)
        var button : Button = findViewById(R.id.continue_button)
        val registerLink: TextView = findViewById(R.id.register_link)

        //Controlar el clickn del boton
        button.setOnClickListener {

            //programar si el usuario y la contraseña es correcto
            //usuario y contraseña guardados
            var username : String = "UAT"
            var userpassword :String = "1234"

            //recojer los valores de las cajas de texto
            var edUsuario : String = usuario.text.toString()
            var edPassword : String = password.text.toString()

            if(username.equals(edUsuario) && userpassword.equals(edPassword)){
                //Abrir la siquiente pantalla
                Toast.makeText(this,"Usuario y contraseña correctos", Toast.LENGTH_SHORT).show()
                //Abrir la 2da pantalla
                var segundaPantalla = Intent(this,Principal::class.java)
                //inicar la actividad de la segunda pantalla
                startActivity(segundaPantalla)
            }else{
                Toast.makeText(this,"Usuario y/o contraseña incorrectos",Toast.LENGTH_SHORT).show()
            }
        }

        //Controlar el click del boton Crear cuenta
        registerLink.setOnClickListener {
            val intent = Intent(this, CrearCuenta::class.java)
            startActivity(intent)
        }

    }
}