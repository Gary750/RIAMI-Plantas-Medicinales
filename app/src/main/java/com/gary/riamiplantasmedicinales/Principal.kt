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

        // Inicialización de vistas usando el bloque `apply` para mejor legibilidad
        findViewById<TextView>(R.id.name).apply { Name = this }
        findViewById<TextView>(R.id.Genero).apply { Genero = this }
        findViewById<TextView>(R.id.Family).apply { Family = this }
        findViewById<TextView>(R.id.Propieties).apply { propiedades = this }
        findViewById<ImageView>(R.id.imageView).apply { imagen = this }
        findViewById<Button>(R.id.button).apply { btnPicture = this }
        findViewById<TextView>(R.id.idPlant).apply { IdPlant = this }

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
    }

    // Función para clasificar la imagen utilizando el modelo de TensorFlow Lite
    private fun classifyImage(image: Bitmap) {
        try {
            // Inicialización del modelo de detección de plantas
            val model = PlantDetection.newInstance(applicationContext)

            // Creación del buffer de entrada para el modelo
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
                order(ByteOrder.nativeOrder())
            }

            // Conversión de la imagen a un array de píxeles
            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            // Extracción de los valores R, G, B y carga en el ByteBuffer
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f)) // R
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))  // G
                    byteBuffer.putFloat((value and 0xFF) * (1f / 255f))         // B
                }
            }

            // Carga del ByteBuffer en el modelo
            inputFeature0.loadBuffer(byteBuffer)

            // Ejecución de la inferencia del modelo
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val confidence = outputFeature0.floatArray

            // Encontrar la clase con la mayor confianza
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidence.indices) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i]
                    maxPos = i
                }
            }

            // Definición de las clases y umbral de confianza
            val classes = arrayOf("Vaporub", "Ajenjo", "Ruda")
            val confianzaMin = 0.97f // 95% de confianza

            //Toast.makeText(this,"Palnta: " + classes[maxPos] +", Confianza $maxConfidence",Toast.LENGTH_SHORT).show()

            if (maxConfidence < confianzaMin) {
                // Si la confianza es menor que el umbral, mostrar mensaje de error
                Toast.makeText(this, "Planta no encontrada en la base de datos", Toast.LENGTH_SHORT).show()
                Name.text = ""
                Family.text = ""
                propiedades.text = ""
                IdPlant.text = ""
                Genero.text = ""
            } else {
                // Obtener el ID del documento de Firestore
                val documentId = classes[maxPos]
                Name.text = documentId

                // Consulta a Firestore para obtener los detalles de la planta
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

            // Cerrar el modelo para liberar recursos
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