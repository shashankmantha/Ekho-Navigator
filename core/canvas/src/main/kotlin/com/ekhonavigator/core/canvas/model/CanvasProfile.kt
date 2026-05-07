package com.ekhonavigator.core.canvas.model

data class CanvasProfile(
    val id: String,
    val name: String,
    val shortName: String?,
    val primaryEmail: String?,
    val avatarUrl: String?,
)
