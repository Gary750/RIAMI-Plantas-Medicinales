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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.gary.riamiplantasmedicinales.ml.PlantDetection
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale


class Principal : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance() //inicializar Firebase Firestore
    //variables de manera global
    lateinit var Name: TextView
    lateinit var Genero: TextView
    lateinit var Family: TextView
    lateinit var propiedades: TextView
    lateinit var imagen: ImageView
    lateinit var btnPicture: Button
    lateinit var IdPlant : TextView
    var imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //relacionar las variables globales con los ids del xml
        Family = findViewById(R.id.Family)
        Genero = findViewById(R.id.Genero)
        Name = findViewById(R.id.name)
        propiedades = findViewById(R.id.Propieties)
        imagen = findViewById(R.id.imageView)
        btnPicture = findViewById(R.id.button)
        IdPlant = findViewById(R.id.idPlant)

        //programar el boton
        btnPicture.setOnClickListener {
            //inicializar la camara si se tenemos permisos
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cammeraIntet = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cammeraIntet, 1)
            } else {
                //si no tiene permisos
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            }
        }

    }

    //Funcion que clasifica una imagen utlilizando machine learning
    private fun classifyImage(image: Bitmap) {
        try {
            // Inicializa el modelo de detección de plantas //DiseaseDetection.newInstance(applicationContext)
            val model = PlantDetection.newInstance(applicationContext)

            // Crea el input para referencia
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            // Crea un ByteBuffer con el tamaño adecuado para los datos de la imagen
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            // Obtiene un array 1D de 224 * 224 píxeles de la imagen
            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            // Itera sobre los píxeles, extrae los valores R, G, B y los agrega al ByteBuffer
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++] // RGB
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat((value and 0xFF) * (1f / 255f))
                }
            }

            // Carga el ByteBuffer en el input del modelo
            inputFeature0.loadBuffer(byteBuffer)

            // Ejecuta la inferencia del modelo y obtiene el resultado
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            // Obtiene el array de confianza del resultado
            val confidence = outputFeature0.floatArray

            // Encuentra el índice de la clase con la mayor confianza
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidence.indices) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i]
                    maxPos = i
                }
                // Formatear maxConfidence como porcentaje con 2 decimales
                //val percentage = String.format(Locale.getDefault(), "%.2f%% ", maxConfidence * 100)
                //Toast.makeText(this, percentage, Toast.LENGTH_SHORT).show()

            }

            // Define las clases posibles
            val classes = arrayOf("Romero","Ruda","Sábila")
            // Umbral de confianza mínima
            val confianzaMin = 0.95f // 95% de confianza


            if (maxConfidence < confianzaMin) {
                // Si la confianza es menor que el umbral, mostramos un mensaje indicando que la planta no está en la base de datos
                Toast.makeText(this, "Planta no encontrada en la base de datos", Toast.LENGTH_SHORT).show()
                Name.text = " "
                Family.text = " "
                propiedades.text = " "
                IdPlant.text = ""
                Genero.text = ""
            } else {

                //variable para inicializar el id del documento
                val documentId = classes[maxPos]

                //Muestra el nombre de la planta con mayor confianza
                Name.text = classes[maxPos]

                //Consulta la familia a la que pertenece
                db.collection("plants").document(documentId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            //Obtiene el campo "Nombre cientifico"
                            val Familia = document.getString("Familia")
                            //Asignar el valor en el texview
                            Family.text = Familia
                        } else {
                            //si el no existe
                            Log.d("Firestore", "No such document")
                            Family.text = "Nombre no encontrado"
                        }

                    } .addOnFailureListener { exception ->
                        // Si hubo un error al obtener el documento
                        Log.d("Firestore", "Error al obtener el documento: ", exception)
                        Name.text = "Error al obtener el nombre"
                    }


                //Consulta para mostrar el genero a l que pertenece
                db.collection("plants").document(documentId)
                    .get()
                    .addOnSuccessListener { document->
                        if(document!=null && document.exists()){
                            //Obetener el campo Descripcion
                            val genero = document.getString("Genero y especie")
                            //Asignar el valor en el textview
                            Genero.text =genero
                        }else{
                            Log.d("Firestore", "No such document")
                            Genero.text= "Informacion no encontrada"
                        }
                    }.addOnFailureListener { exception ->
                        // Si hubo un error al obtener el documento
                        Log.d("Firestore", "Error al obtener el documento: ", exception)
                        Name.text = "Error al obtener la descripcion"
                    }

                //Consulta para mostrar las caractetisticas(propiedades de la planta)
                db.collection("plants").document(documentId)
                    .get()
                    .addOnSuccessListener { document->
                        if (document!=null && document.exists()){
                            //Obtener el campo Caracteristicas
                            val CaracPlant = document.getString("Usos medicinales")
                            // Mostrar las características en el TextView con formato
                            propiedades.text = CaracPlant
                        }else{
                            Log.d("Firestore", "No such document")
                            propiedades.text = "Información no encontrada"
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Si hubo un error al obtener el documento
                        Log.d("Firestore", "Error al obtener el documento: ", exception)
                        Name.text = "Error al obtener la información"
                    }
                //Consulta el id de la planta
                db.collection("plants").document(documentId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document!=null && document.exists()){
                            //Obtener el campo id
                            val ID_Planta = document.getString("Id")
                            //Mostrar el id de la planta en el texview asignado
                            IdPlant.text = ID_Planta
                        }else{
                            Log.d("Firestore", "No such document")
                            IdPlant.text = "Información no encontrada"
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Si hubo un error al obtener el documento
                        Log.d("Firestore", "Error al obtener el documento: ", exception)
                        Name.text = "Error al obtener la información"
                    }
            }

            // Configura un listener para el TextView result para abrir una búsqueda en Google de la planta detectada
            /*result.setOnClickListener {
                    //startActivity(Intent(Intent.ACTION_VIEW, Url.parse("https://www.google.com/search?q=${result.text}")))
                }*/

            // Cierra el modelo para liberar recursos
            model.close()
        } catch (e: IOException) {
            // Maneja la excepción en caso de error
            e.printStackTrace()
        }
    }
    //Crear Funcion que devuelve la planta clasificada
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK){
            val image  = data?.extras?.get("data") as Bitmap
            val  dimension = Math.min(image.width, image.height)
            val thumbnail = ThumbnailUtils.extractThumbnail(image,dimension,dimension)
            imagen.setImageBitmap(thumbnail)
            val scaleImage = Bitmap.createScaledBitmap(thumbnail,imageSize,imageSize, false)
            classifyImage(scaleImage)
        }
    }
}