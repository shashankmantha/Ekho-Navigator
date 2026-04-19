package com.ekhonavigator.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedLocation(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val details: String = ""
)