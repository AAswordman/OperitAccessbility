package com.ai.assistance.operit.provider

import org.junit.Assert.*
import org.junit.Test

class UtilsTest {
    @Test
    fun testReverse() {
        assertEquals("cba", Utils.reverse("abc"))
        assertEquals("321", Utils.reverse("123"))
    }
}
