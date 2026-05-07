package com.ekhonavigator.core.canvas.auth

class FakeCanvasInstitutionStore : CanvasInstitutionStore {
    private val domains = mutableMapOf<String, String>()

    override fun getDomain(uid: String): String? = domains[uid]

    override fun setDomain(uid: String, domain: String) {
        domains[uid] = domain
    }

    override fun clearDomain(uid: String) {
        domains.remove(uid)
    }

    override fun clearAll() {
        domains.clear()
    }
}
