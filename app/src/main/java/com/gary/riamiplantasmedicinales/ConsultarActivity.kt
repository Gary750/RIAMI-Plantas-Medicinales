package com.gary.riamiplantasmedicinales

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText
import com.gary.riamiplantasmedicinales.models.Plant

class ConsultarActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var searchBar: EditText
    private val db = FirebaseFirestore.getInstance()
    private var plantList = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultar)

        recyclerView = findViewById(R.id.recyclerView)
        searchBar = findViewById(R.id.search_bar)
        recyclerView.layoutManager = LinearLayoutManager(this)

        plantAdapter = PlantAdapter(plantList)
        recyclerView.adapter = plantAdapter

        fetchPlants()

        // Agregar filtro de búsqueda
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
        })
    }

    private fun fetchPlants() {
        db.collection("plants")
            .get()
            .addOnSuccessListener { documents ->
                plantList.clear()
                for (document in documents) {
                    val plant = Plant(
                        document.getString("Id") ?: "", // ID adicional
                        document.id, // Nombre de la planta (ID del documento)
                        document.getString("Familia") ?: "",
                        document.getString("Genero y especie") ?: "",
                        document.getString("Usos medicinales") ?: ""
                    )
                    plantList.add(plant)
                }

                // Ordenar la lista por el ID adicional (convertido a Int)
                val sortedList = plantList.sortedBy { it.id.toInt() }

                // Actualizar el adaptador con la lista ordenada
                plantAdapter.updateList(sortedList)
            }
    }

    private fun filter(query: String) {
        val filteredList = plantList.filter { plant ->
            // Buscar en el nombre de la planta o en el ID adicional
            plant.nombre.contains(query, ignoreCase = true) || plant.id.contains(query, ignoreCase = true)
        }
        plantAdapter.updateList(filteredList)
    }
}