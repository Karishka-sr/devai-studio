package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModeTest {
    @Test
    fun fromId_returnsKnownModes() {
        assertEquals(ChatMode.CHAT, ChatMode.fromId("chat"))
        assertEquals(ChatMode.DEV, ChatMode.fromId("dev"))
    }

    @Test
    fun fromId_fallsBackToChat() {
        assertEquals(ChatMode.CHAT, ChatMode.fromId(null))
        assertEquals(ChatMode.CHAT, ChatMode.fromId("unknown"))
    }
}
