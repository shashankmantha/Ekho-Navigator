package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceMatcherTest {

    private val matcher = PlaceMatcher()

    private val broome = Place(
        id = "broome_library",
        name = "Broome Library",
        latitude = 34.1627,
        longitude = -119.0410,
        category = PlaceCategory.BUILDINGS,
        aliases = listOf("The Library", "JH Broome"),
    )
    private val sierra = Place(
        id = "sierra_hall",
        name = "Sierra Hall",
        latitude = 34.1622,
        longitude = -119.0446,
        category = PlaceCategory.BUILDINGS,
    )
    private val sageServices = Place(
        id = "sage_hall",
        name = "Sage Hall/Enrollment Center",
        latitude = 34.1640,
        longitude = -119.0422,
        category = PlaceCategory.SERVICES,
        aliases = listOf("Enrollment Center", "Sage Hall"),
    )

    private val places = listOf(broome, sierra, sageServices)

    @Test
    fun `exact name matches`() {
        assertEquals("broome_library", matcher.resolve("Broome Library", places))
    }

    @Test
    fun `case and punctuation are ignored`() {
        assertEquals("broome_library", matcher.resolve("BROOME LIBRARY,", places))
        assertEquals("broome_library", matcher.resolve("broome-library", places))
    }

    @Test
    fun `location string containing name matches`() {
        assertEquals(
            "broome_library",
            matcher.resolve("Broome Library, Room 1360", places),
        )
    }

    @Test
    fun `alias resolves to same id as name`() {
        assertEquals("broome_library", matcher.resolve("The Library", places))
    }

    @Test
    fun `blank or empty input returns null`() {
        assertNull(matcher.resolve("", places))
        assertNull(matcher.resolve("   ", places))
    }

    @Test
    fun `non-matching string returns null`() {
        assertNull(matcher.resolve("Off-campus coffee shop", places))
    }

    @Test
    fun `substring without word boundary does not match`() {
        assertNull(matcher.resolve("Sierrawood Apartments", places))
    }

    @Test
    fun `slash-separated name matches via alias`() {
        assertEquals("sage_hall", matcher.resolve("Enrollment Center", places))
        assertEquals("sage_hall", matcher.resolve("Sage Hall", places))
    }

    @Test
    fun `first place wins on ambiguous match`() {
        val libA = broome
        val libB = broome.copy(id = "other_library", name = "Library")
        val result = matcher.resolve("Library", listOf(libA, libB))
        assertEquals("other_library", result)
    }
}
