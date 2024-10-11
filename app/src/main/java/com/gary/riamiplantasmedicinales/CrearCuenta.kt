package com.gary.riamiplantasmedicinales

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class CrearCuenta : AppCompatActivity() {

    // Método privado para mostrar una alerta de error
    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this) // Crear un constructor de AlertDialog
        builder.setTitle("Error") // Establecer el título del diálogo
        builder.setMessage(message) // Establecer el mensaje del diálogo
        builder.setPositiveButton("Aceptar", null) // Botón para aceptar, sin acción adicional
        val dialog: AlertDialog = builder.create() // Crear el diálogo
        dialog.show() // Mostrar el diálogo
    }

    // Método onCreate que se llama al crear la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Llamar al método de la superclase
        enableEdgeToEdge() // Activar el diseño de borde a borde
        setContentView(R.layout.activity_crear_cuenta) // Establecer el XML que se usará para esta actividad

        // Ajustar márgenes para la barra del sistema (status bar, navigation bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Obtener los márgenes de las barras del sistema
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Establecer el padding del layout principal
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Retornar insets para que se apliquen
        }

        // Obtener referencias a los elementos en el layout
        val regEmail: EditText = findViewById(R.id.emailEditText) // EditText para el correo electrónico
        val regPassword: EditText = findViewById(R.id.passwordEditText) // EditText para la contraseña
        val createAccount: Button = findViewById(R.id.createAccountButton) // Botón para crear la cuenta

        // Configurar el listener para el botón de crear cuenta
        createAccount.setOnClickListener {
            // Verificar que ambos campos no estén vacíos
            if (regEmail.text.isNotEmpty() && regPassword.text.isNotEmpty()) {
                // Crear el usuario en Firebase con el correo y contraseña proporcionados
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    regEmail.text.toString(), regPassword.text.toString()
                ).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Usuario creado correctamente, mostrar un mensaje
                        Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()

                        // Redirigir a la pantalla de inicio de sesión
                        val principal = Intent(this, Login::class.java) // Crear un Intent para iniciar la actividad Login
                        startActivity(principal) // Iniciar la actividad Login
                        finish() // Finalizar la actividad actual para no volver a ella
                    } else {
                        // Mostrar un mensaje de error si algo falla en la creación de la cuenta
                        showAlert("Error creando la cuenta: ${task.exception?.localizedMessage}")
                        // Registrar el error en el log
                        Log.e("FirebaseAuth", "Error creando cuenta", task.exception)
                    }
                }
            } else {
                // Mostrar alerta si los campos están vacíos
                showAlert("Por favor, rellena todos los campos")
            }
        }
    }
}