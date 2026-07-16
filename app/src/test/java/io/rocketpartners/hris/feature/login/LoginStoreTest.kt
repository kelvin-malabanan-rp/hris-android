package io.rocketpartners.hris.feature.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginStoreTest {

    @Test
    fun trimsEmailWhitespace() {
        val store = LoginStore().apply { email = "  ada@rp.io \n" }
        assertTrue(store.isEmailValid)
        assertTrue(store.hasEmailInput)
    }

    @Test
    fun rejectsMalformedEmails() {
        val store = LoginStore()
        listOf("ada", "ada@", "ada@rp", "@rp.io", "a b@rp.io").forEach {
            store.email = it
            assertFalse("expected invalid: $it", store.isEmailValid)
        }
    }

    @Test
    fun canSubmitRequiresValidEmailAndNonEmptyPassword() {
        val store = LoginStore()
        store.email = "ada@rp.io"
        store.password = ""
        assertFalse(store.canSubmit)
        store.password = "pw"
        assertTrue(store.canSubmit)
        store.email = "nope"
        assertFalse(store.canSubmit)
    }

    @Test
    fun emptyEmailIsNotFlaggedAsInput() {
        assertFalse(LoginStore().hasEmailInput)
    }

    @Test
    fun ssoStubsDoNotTouchFormState() {
        // The OAuth wiring ships separately; until then the SSO entry points must be safe
        // no-ops that leave the email/password form untouched.
        val store = LoginStore().apply {
            email = "ada@rp.io"
            password = "pw"
        }
        store.signInWithGoogle()
        store.signInWithOkta()
        assertEquals("ada@rp.io", store.email)
        assertEquals("pw", store.password)
        assertTrue(store.canSubmit)
    }
}
