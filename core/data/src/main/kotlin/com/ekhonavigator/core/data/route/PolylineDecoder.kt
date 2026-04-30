package com.ekhonavigator.core.data.route

import com.google.android.gms.maps.model.LatLng

/**
 * Utility to decode encoded polyline strings from Google Routes API.
 */
object PolylineDecoder {

    /**
     * Decodes the compressed path string into a list of map coordinates.
     */
    fun decodeEncodedPath(encodedPathString: String): List<LatLng> {
        val pathCoordinates = mutableListOf<LatLng>()
        var currentPositionInString = 0
        val totalPathLength = encodedPathString.length

        var currentLatAccumulator = 0
        var currentLngAccumulator = 0

        while (currentPositionInString < totalPathLength) {
            val (latOffset, nextPositionAfterLat) = decodeNextValueInPath(encodedPathString, currentPositionInString)
            currentLatAccumulator += latOffset

            val (lngOffset, finalPositionAfterLng) = decodeNextValueInPath(encodedPathString, nextPositionAfterLat)
            currentLngAccumulator += lngOffset

            currentPositionInString = finalPositionAfterLng

            val coordinate = LatLng(
                currentLatAccumulator.toDouble() / 1e5,
                currentLngAccumulator.toDouble() / 1e5
            )
            pathCoordinates.add(coordinate)
        }

        return pathCoordinates
    }

    /**
     * Extracts a single numerical value from the encoded string sequence.
     */
    private fun decodeNextValueInPath(encodedPathString: String, startIndex: Int): Pair<Int, Int> {
        var calculatedValue = 0
        var bitShiftAmount = 0
        var indexPointer = startIndex
        var currentByteValue: Int

        do {
            // subtract 63 to get the original value as per Google's algorithm.
            currentByteValue = encodedPathString[indexPointer++].code - 63
            calculatedValue = calculatedValue or (currentByteValue and 0x1f shl bitShiftAmount)
            bitShiftAmount += 5
        } while (currentByteValue >= 0x20)

        // handles the bit manipulation for signed integers
        val finalDecodedValue = if (calculatedValue and 1 != 0) {
            (calculatedValue shr 1).inv()
        } else {
            calculatedValue shr 1
        }

        return Pair(finalDecodedValue, indexPointer)
    }
}