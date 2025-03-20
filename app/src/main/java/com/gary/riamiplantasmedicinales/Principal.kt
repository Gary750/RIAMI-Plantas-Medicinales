package com.gary.riamiplantasmedicinales

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.gary.riamiplantasmedicinales.ml.PlantDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Principal : AppCompatActivity() {

    // Inicialización de Firebase Firestore
    private val db = FirebaseFirestore.getInstance()

    // Declaración de variables globales
    private lateinit var Name: TextView
    private lateinit var Genero: TextView
    private lateinit var Family: TextView
    private lateinit var propiedades: TextView
    private lateinit var imagen: ImageView
    private lateinit var btnPicture: Button
    private lateinit var IdPlant: TextView
    private lateinit var btnSearch: ImageView
    private val imageSize = 224 // Tamaño de la imagen para el modelo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal)

        // Configuración de insets para manejar correctamente las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicialización de vistas
        Name = findViewById(R.id.name)
        Genero = findViewById(R.id.Genero)
        Family = findViewById(R.id.Family)
        propiedades = findViewById(R.id.Propieties)
        imagen = findViewById(R.id.imageView)
        btnPicture = findViewById(R.id.button)
        IdPlant = findViewById(R.id.idPlant)
        btnSearch = findViewById(R.id.search_icon)

        // Verificación de permisos de la cámara
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }

        // Configuración del listener del botón para abrir la cámara
        btnPicture.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 1)
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del botón de búsqueda para abrir la actividad ConsultarActivity
        btnSearch.setOnClickListener {
            val intent = Intent(this, ConsultarActivity::class.java)
            startActivity(intent)
        }
    }

    // Función para clasificar la imagen utilizando el modelo de TensorFlow Lite
    private fun classifyImage(image: Bitmap) {
        try {
            val model = PlantDetection.newInstance(applicationContext)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f)) // R
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))  // G
                    byteBuffer.putFloat((value and 0xFF) * (1f / 255f))         // B
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val confidence = outputFeature0.floatArray

            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidence.indices) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i]
                    maxPos = i
                }
            }

            val classes = arrayOf("Vaporub", "Ajenjo", "Ruda")
            val confianzaMin = 0.97f

            if (maxConfidence < confianzaMin) {
                Toast.makeText(this, "Planta no encontrada en la base de datos", Toast.LENGTH_SHORT).show()
                Name.text = ""
                Family.text = ""
                propiedades.text = ""
                IdPlant.text = ""
                Genero.text = ""
            } else {
                val documentId = classes[maxPos]
                Name.text = documentId

                db.collection("plants").document(documentId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            Family.text = document.getString("Familia") ?: "Nombre no encontrado"
                            Genero.text = document.getString("Genero y especie") ?: "Información no encontrada"
                            propiedades.text = document.getString("Usos medicinales") ?: "Información no encontrada"
                            IdPlant.text = document.getString("Id") ?: "Información no encontrada"
                        } else {
                            Log.d("Firestore", "No such document")
                            Family.text = "Nombre no encontrado"
                            Genero.text = "Información no encontrada"
                            propiedades.text = "Información no encontrada"
                            IdPlant.text = "Información no encontrada"
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("Firestore", "Error al obtener el documento: ", exception)
                        Name.text = "Error al obtener la información"
                    }
            }
            model.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Manejo del resultado de la cámara
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val image = data?.extras?.get("data") as? Bitmap
            image?.let {
                val dimension = Math.min(it.width, it.height)
                val thumbnail = ThumbnailUtils.extractThumbnail(it, dimension, dimension)
                imagen.setImageBitmap(thumbnail)
                val scaleImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                classifyImage(scaleImage)
            }
        }
    }
}
