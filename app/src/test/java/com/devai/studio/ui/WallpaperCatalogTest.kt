package com.devai.studio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperCatalogTest {
    @Test
    fun catalogHasTwentyFourUniqueWallpapers() {
        assertEquals(24, ChatWallpapers.all.size)
        assertEquals(24, ChatWallpapers.all.map { it.id }.distinct().size)
        assertEquals(24, ChatWallpapers.all.map { it.full }.distinct().size)
        assertEquals(24, ChatWallpapers.all.map { it.thumb }.distinct().size)
    }

    @Test
    fun everyWallpaperHasTitleAndResources() {
        ChatWallpapers.all.forEach { wallpaper ->
            assertTrue(wallpaper.id.isNotBlank())
            assertTrue(wallpaper.title.isNotBlank())
            assertTrue(wallpaper.full != 0)
            assertTrue(wallpaper.thumb != 0)
        }
        assertEquals("Без обоев", ChatWallpapers.title("none"))
        assertEquals("Sunset", ChatWallpapers.title("wall_02"))
    }
}
