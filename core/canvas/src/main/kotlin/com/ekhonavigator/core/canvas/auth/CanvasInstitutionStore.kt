package com.ekhonavigator.core.canvas.auth

interface CanvasInstitutionStore {
    fun getDomain(uid: String): String?
    fun setDomain(uid: String, domain: String)
    fun clearDomain(uid: String)
    fun clearAll()
}
