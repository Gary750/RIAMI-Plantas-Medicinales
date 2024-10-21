package com.gary.riamiplantasmedicinales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    // Inicializar FirebaseAuth y Firestore para manejar la autenticación y la base de datos
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore

    // Método onCreate que se llama al crear la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Llamar al método de la superclase
        enableEdgeToEdge() // Activar el diseño de borde a borde
        setContentView(R.layout.activity_login) // Establecer el XML que se usará para esta actividad

        // Ajustar márgenes para la barra del sistema (status bar, navigation bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Obtener los márgenes de las barras del sistema
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Establecer el padding del layout principal
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Retornar insets para que se apliquen
        }

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Crear las variables de Kotlin que relacionan la parte lógica con el XML
        val usuario: EditText = findViewById(R.id.username) // EditText para el nombre de usuario
        val password: EditText = findViewById(R.id.password) // EditText para la contraseña
        val button: Button = findViewById(R.id.continue_button) // Botón para continuar
        val registerLink: TextView = findViewById(R.id.register_link) // Enlace para crear una cuenta

        // Controlar el clic del botón
        button.setOnClickListener {
            // Recolectar los valores de las cajas de texto
            val edUsuario: String = usuario.text.toString().trim() // Usar trim() para eliminar espacios
            val edPassword: String = password.text.toString().trim() // Usar trim() para eliminar espacios

            // Validar entradas vacías
            if (edUsuario.isEmpty() || edPassword.isEmpty()) {
                // Mostrar un mensaje si alguno de los campos está vacío
                Toast.makeText(this, "Por favor, ingresa el nombre de usuario y la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Salir si hay campos vacíos
            }

            // Buscar el usuario en Firestore usando el nombre de usuario
            db.collection("users") // Asegúrate de que este sea el nombre correcto de tu colección
                .whereEqualTo("username", edUsuario) // Buscar por el nombre de usuario
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Usuario no encontrado
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    } else {
                        // Si se encuentra el usuario, obtener el correo electrónico
                        val userDocument = documents.first()
                        val userEmail = userDocument.getString("email") // Obtener el correo electrónico

                        // Autenticar al usuario con Firebase
                        auth.signInWithEmailAndPassword(userEmail ?: "", edPassword)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Usuario autenticado correctamente
                                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                    // Abrir la segunda pantalla (actividad principal)
                                    val segundaPantalla = Intent(this, Principal::class.java)
                                    startActivity(segundaPantalla) // Iniciar la actividad principal
                                    finish() // Opcionalmente, finalizar la actividad de login
                                } else {
                                    // Mostrar un mensaje de error si algo falla en la autenticación
                                    task.exception?.let { exception ->
                                        Toast.makeText(this, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                        Toast.makeText(this, "Error desconocido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Manejo de errores en la consulta
                    Toast.makeText(this, "Error al buscar el usuario: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // Controlar el clic del botón para crear cuenta
        registerLink.setOnClickListener {
            // Crear un Intent para iniciar la actividad de creación de cuenta
            val intent = Intent(this, CrearCuenta::class.java)
            startActivity(intent) // Iniciar la actividad de creación de cuenta
        }
    }
}
