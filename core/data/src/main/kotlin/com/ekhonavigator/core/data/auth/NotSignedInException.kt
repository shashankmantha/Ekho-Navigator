package com.ekhonavigator.core.data.auth

/**
 * Thrown (or wrapped in `Result.failure`) by repositories that refuse to perform
 * a write because no user is currently authenticated. Lets call sites distinguish
 * "not signed in" from generic Firestore / network failures.
 */
class NotSignedInException : IllegalStateException("No authenticated user")
