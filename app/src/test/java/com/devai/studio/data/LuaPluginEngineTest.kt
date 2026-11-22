package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuaPluginEngineTest {
    @Test
    fun loadsAndRunsLuaCommand() {
        val result = LuaPluginEngine.loadResult(
            """
            return {
              name = "Test",
              commands = {
                { name = "/upper", run = function(input) return string.upper(input) end }
              }
            }
            """.trimIndent()
        )
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        val command = result.getOrThrow().commands.single()
        assertEquals("HI", LuaPluginEngine.run(command, "hi"))
    }
}
