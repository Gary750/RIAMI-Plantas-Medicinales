package com.gary.riamiplantasmedicinales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gary.riamiplantasmedicinales.models.Plant

class PlantAdapter(private var plantList: List<Plant>) : RecyclerView.Adapter<PlantAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.idPlanta)
        val nombre: TextView = itemView.findViewById(R.id.nombrePlanta)
        val familia: TextView = itemView.findViewById(R.id.familiaPlanta)
        val genero: TextView = itemView.findViewById(R.id.generoPlanta)
        val usos: TextView = itemView.findViewById(R.id.usosPlanta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_planta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val planta = plantList[position]
        holder.id.text = "ID: ${planta.id}" // Muestra el ID adicional
        holder.nombre.text = planta.nombre  // Muestra el nombre de la planta
        holder.familia.text = "Familia: ${planta.familia}"
        holder.genero.text = "Género y especie: ${planta.genero}"
        holder.usos.text = "Usos medicinales: ${planta.usos}"
    }

    override fun getItemCount(): Int = plantList.size

    fun updateList(newList: List<Plant>) {
        plantList = newList
        notifyDataSetChanged()
    }
}