package com.ekhonavigator.core.model

data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val aliases: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val ownerUid: String? = null,
)

enum class PlaceCategory {
    PARKING,
    BUILDINGS,
    FOOD,
    HOUSING,
    SERVICES,
    GENERAL,
}
