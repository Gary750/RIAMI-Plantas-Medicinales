package com.gary.riamiplantasmedicinales

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
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
import com.gary.riamiplantasmedicinales.ml.DiseaseDetection

class Principal : AppCompatActivity() {
    //variables de manera global
    lateinit var result: TextView
    lateinit var propiedades : TextView
    lateinit var  imagen: ImageView
    lateinit var btnPicture : Button
    var  imageSize = 224

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
        result = findViewById(R.id.result)
        propiedades = findViewById(R.id.clasified)
        imagen = findViewById(R.id.imageView)
        btnPicture = findViewById(R.id.button)

        //programar el boton
        btnPicture.setOnClickListener {
            //inicializar la camara si se tenemos permisos
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                val cammeraIntet = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cammeraIntet,1)
            }else{
                //si no tiene permisos
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA),100)
            }
        }

    }

    //Funcion que clasifica una imagen utlilizando machine learning
    private fun classifyImage(image: Bitmap) {
        try {
            // Inicializa el modelo de detección de plantas
            val model = DiseaseDetection.newInstance(applicationContext)

            // Crea el input para referencia
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

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
                Toast.makeText(this,"%"+maxConfidence,Toast.LENGTH_SHORT).show();
            }

            // Define las clases posibles
            val classes = arrayOf("Ruda", "Hierbabuena", "Romero", "Vaporub")

            // Umbral de confianza mínima
            val confianzaMin = 0.95f // 95% de confianza

            if (maxConfidence < confianzaMin) {
                // Si la confianza es menor que el umbral, mostramos un mensaje indicando que la planta no está en la base de datos
                result.text = "Planta no reconocida"
                propiedades.text = "No tenemos información sobre esta planta."
            } else {

                // Muestra el nombre de la clase con mayor confianza en el TextView result
                result.text = classes[maxPos]

                //Compara la clase son mayor confianza para mostrar las propiedades en el textview de propiedades
                if (classes[maxPos] == "Ruda") {
                    // Muestra las propiedades de la planta detectada en el TextView properties
                    propiedades.text =
                        "La ${classes[maxPos]} una planta con propiedades medicinales conocidas por sus efectos" +
                                " digestivos, antiespasmódicos y analgésicos. Se utiliza para tratar problemas gastrointestinales, " +
                                "aliviar dolores menstruales y como tónico para el hígado.\nTambién tiene aplicaciones en la medicina" +
                                " tradicional para combatir infecciones y parásitos, aunque su uso en grandes cantidades puede ser" +
                                " tóxico."
                } else if (classes[maxPos] == "Hierbabuena") {
                    // Muestra las propiedades de la planta detectada en el TextView properties
                    propiedades.text =
                        "La ${classes[maxPos]} es una planta con propiedades digestivas, carminativas y " +
                                "antiespasmódicas, que ayuda a aliviar problemas gastrointestinales como gases y cólicos.\nTambién" +
                                " tiene propiedades antimicrobianas y antiinflamatorias, siendo útil en el tratamiento de resfriados" +
                                " y dolores de cabeza. Su aroma refrescante la hace popular en infusiones y como complemento culinario."
                } else if (classes[maxPos] == "Romero") {
                    // Muestra las propiedades de la planta detectada en el TextView properties
                    propiedades.text =
                        "EL ${classes[maxPos]} es una planta con propiedades antioxidantes, antiinflamatorias y " +
                                "digestivas. Se utiliza para mejorar la memoria, aliviar dolores musculares y mejorar la circulación" +
                                " sanguínea.\nAdemás, tiene aplicaciones en el tratamiento de problemas gastrointestinales y como" +
                                " estimulante para el crecimiento del cabello. Su aroma también la convierte en un popular" +
                                " complemento culinario."
                } else if (classes[maxPos] == "Vaporub") {
                    // Muestra las propiedades de la planta detectada en el TextView properties
                    propiedades.text =
                        "La planta de ${classes[maxPos]} conocida también como \"Eucalyptus\" o \"Eucalipto\"," +
                                " tiene propiedades expectorantes, antiinflamatorias y antimicrobianas.\nSe usa para aliviar la" +
                                " congestión nasal, tratar resfriados y mejorar la respiración. Sus aceites esenciales son útiles" +
                                " en vaporizaciones y ungüentos para calmar la tos y descongestionar las vías respiratorias."
                }

                // Configura un listener para el TextView result para abrir una búsqueda en Google de la planta detectada
                result.setOnClickListener {
                    //startActivity(Intent(Intent.ACTION_VIEW, Url.parse("https://www.google.com/search?q=${result.text}")))
                }

                // Cierra el modelo para liberar recursos
                model.close()
            }
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
