package com.rinx.artRINXapp.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorsTest {

    @Test
    fun `string_too_long validation yields field-aware message`() {
        val body = """
            {"detail":[{"type":"string_too_long","loc":["query","username"],
             "msg":"String should have at most 50 characters","input":"x","ctx":{"max_length":50}}]}
        """.trimIndent()
        assertEquals("Username must be at most 50 characters.", serverMessageOrNull(body))
    }

    @Test
    fun `string_too_short validation yields field-aware message`() {
        val body = """
            {"detail":[{"type":"string_too_short","loc":["body","display_name"],
             "msg":"too short","ctx":{"min_length":3}}]}
        """.trimIndent()
        assertEquals("Display name must be at least 3 characters.", serverMessageOrNull(body))
    }

    @Test
    fun `missing field yields required message`() {
        val body = """{"detail":[{"type":"missing","loc":["body","title"],"msg":"Field required"}]}"""
        assertEquals("Title is required.", serverMessageOrNull(body))
    }

    @Test
    fun `validation without ctx falls back to labelled msg`() {
        val body = """{"detail":[{"type":"value_error","loc":["body","email"],"msg":"not a valid email"}]}"""
        assertEquals("Email: not a valid email", serverMessageOrNull(body))
    }

    @Test
    fun `plain message envelope is returned as-is`() {
        assertEquals("invite_required", serverMessageOrNull("""{"message":"invite_required"}"""))
    }

    @Test
    fun `string detail envelope is returned as-is`() {
        assertEquals("Not authenticated", serverMessageOrNull("""{"detail":"Not authenticated"}"""))
    }

    @Test
    fun `blank or unparseable body yields null`() {
        assertNull(serverMessageOrNull(null))
        assertNull(serverMessageOrNull(""))
        assertNull(serverMessageOrNull("not json"))
    }
}
