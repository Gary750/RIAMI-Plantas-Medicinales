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
import com.google.firebase.firestore.FirebaseFirestore // Importar Firestore

class CrearCuenta : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val regUser: EditText = findViewById(R.id.userEditText)
        val regEmail: EditText = findViewById(R.id.emailEditText)
        val regPassword: EditText = findViewById(R.id.passwordEditText)
        val createAccount: Button = findViewById(R.id.createAccountButton)

        createAccount.setOnClickListener {
            if (regEmail.text.isNotEmpty() && regPassword.text.isNotEmpty() && regUser.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    regEmail.text.toString(), regPassword.text.toString()
                ).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Usuario creado correctamente
                        val userId = task.result.user?.uid // Obtener el ID del usuario
                        val userMap = hashMapOf(
                            "username" to regUser.text.toString(),
                            "email" to regEmail.text.toString()
                        )

                        // Guardar el usuario en Firestore
                        userId?.let {
                            db.collection("users").document(it)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                                    // Redirigir a la pantalla de inicio de sesión
                                    val principal = Intent(this, Login::class.java)
                                    startActivity(principal)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    showAlert("Error al registrar al usuario: ${e.localizedMessage}")
                                    Log.e("Firestore", "Error registrando usuario", e)
                                }
                        }
                    } else {
                        showAlert("Error creando la cuenta: ${task.exception?.localizedMessage}")
                        Log.e("FirebaseAuth", "Error creando cuenta", task.exception)
                    }
                }
            } else {
                showAlert("Por favor, rellena todos los campos")
            }
        }
    }
}
