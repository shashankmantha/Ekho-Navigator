package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place

fun interface PlacesSeed {
    fun places(): List<Place>
}
