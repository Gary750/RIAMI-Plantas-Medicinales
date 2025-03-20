package com.gary.riamiplantasmedicinales.models

data class Plant(
    val id: String,         // ID adicional (campo "Id" en Firestore)
    val nombre: String,     // Nombre de la planta (ID del documento)
    val familia: String,
    val genero: String,
    val usos: String
)