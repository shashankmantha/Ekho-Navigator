package com.ekhonavigator.core.canvas.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultCanvasAccountSourceTest {

    @Test
    fun `returns null when no user is signed in`() {
        val source = DefaultCanvasAccountSource(
            identitySource = FakeCanvasIdentitySource(uid = null),
            institutionStore = FakeCanvasInstitutionStore().apply { setDomain("uid-1", "csuci.instructure.com") },
        )

        assertNull(source.currentOrNull())
    }

    @Test
    fun `returns null when signed in but no institution stored for that uid`() {
        val source = DefaultCanvasAccountSource(
            identitySource = FakeCanvasIdentitySource(uid = "uid-1"),
            institutionStore = FakeCanvasInstitutionStore(),
        )

        assertNull(source.currentOrNull())
    }

    @Test
    fun `composes account from current uid and stored institution domain`() {
        val source = DefaultCanvasAccountSource(
            identitySource = FakeCanvasIdentitySource(uid = "uid-1"),
            institutionStore = FakeCanvasInstitutionStore().apply { setDomain("uid-1", "csuci.instructure.com") },
        )

        assertEquals(CanvasAccount("uid-1", "csuci.instructure.com"), source.currentOrNull())
    }
}
