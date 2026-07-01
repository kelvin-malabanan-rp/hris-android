package io.rocketpartners.hris.feature.login

/**
 * Holds the sign-in form's fields and derived validation. Pure logic (no Compose/Android deps) so it
 * unit-tests directly; the Compose screen binds [email]/[password] to it. Mirrors iOS `LoginStore`.
 */
class LoginStore {
    var email: String = ""
    var password: String = ""

    /** Email with surrounding whitespace/newlines removed (e.g. from autofill/paste). */
    val trimmedEmail: String get() = email.trim()

    val canSubmit: Boolean get() = isEmailValid && password.isNotEmpty()

    /** Whether the entered email parses as a valid address — drives the field's error styling. */
    val isEmailValid: Boolean get() = EMAIL_REGEX.matches(trimmedEmail)

    /** Whether the user has typed anything into email yet (so we don't flag an empty field). */
    val hasEmailInput: Boolean get() = trimmedEmail.isNotEmpty()

    private companion object {
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
