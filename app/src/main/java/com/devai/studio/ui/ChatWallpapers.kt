package com.devai.studio.ui

import androidx.annotation.DrawableRes
import com.devai.studio.R

data class ChatWallpaper(
    val id: String,
    val title: String,
    @DrawableRes val full: Int,
    @DrawableRes val thumb: Int,
)

object ChatWallpapers {
    val none = ChatWallpaper("none", "Без обоев", 0, 0)

    val all: List<ChatWallpaper> = listOf(
        ChatWallpaper("wall_01", "Indigo Mint", R.drawable.chat_wall_01, R.drawable.chat_wall_thumb_01),
        ChatWallpaper("wall_02", "Sunset", R.drawable.chat_wall_02, R.drawable.chat_wall_thumb_02),
        ChatWallpaper("wall_03", "Ocean", R.drawable.chat_wall_03, R.drawable.chat_wall_thumb_03),
        ChatWallpaper("wall_04", "Forest", R.drawable.chat_wall_04, R.drawable.chat_wall_thumb_04),
        ChatWallpaper("wall_05", "Lavender", R.drawable.chat_wall_05, R.drawable.chat_wall_thumb_05),
        ChatWallpaper("wall_06", "Mono", R.drawable.chat_wall_06, R.drawable.chat_wall_thumb_06),
        ChatWallpaper("wall_07", "Cyber", R.drawable.chat_wall_07, R.drawable.chat_wall_thumb_07),
        ChatWallpaper("wall_08", "Peach", R.drawable.chat_wall_08, R.drawable.chat_wall_thumb_08),
        ChatWallpaper("wall_09", "Ice", R.drawable.chat_wall_09, R.drawable.chat_wall_thumb_09),
        ChatWallpaper("wall_10", "Gold", R.drawable.chat_wall_10, R.drawable.chat_wall_thumb_10),
        ChatWallpaper("wall_11", "Rose", R.drawable.chat_wall_11, R.drawable.chat_wall_thumb_11),
        ChatWallpaper("wall_12", "Aurora", R.drawable.chat_wall_12, R.drawable.chat_wall_thumb_12),
        ChatWallpaper("wall_13", "Cobalt", R.drawable.chat_wall_13, R.drawable.chat_wall_thumb_13),
        ChatWallpaper("wall_14", "Ember", R.drawable.chat_wall_14, R.drawable.chat_wall_thumb_14),
        ChatWallpaper("wall_15", "Violet", R.drawable.chat_wall_15, R.drawable.chat_wall_thumb_15),
        ChatWallpaper("wall_16", "Slate", R.drawable.chat_wall_16, R.drawable.chat_wall_thumb_16),
        ChatWallpaper("wall_17", "Mint", R.drawable.chat_wall_17, R.drawable.chat_wall_thumb_17),
        ChatWallpaper("wall_18", "Coral", R.drawable.chat_wall_18, R.drawable.chat_wall_thumb_18),
        ChatWallpaper("wall_19", "Night", R.drawable.chat_wall_19, R.drawable.chat_wall_thumb_19),
        ChatWallpaper("wall_20", "Sand", R.drawable.chat_wall_20, R.drawable.chat_wall_thumb_20),
        ChatWallpaper("wall_21", "Plum", R.drawable.chat_wall_21, R.drawable.chat_wall_thumb_21),
        ChatWallpaper("wall_22", "Teal", R.drawable.chat_wall_22, R.drawable.chat_wall_thumb_22),
        ChatWallpaper("wall_23", "Grape", R.drawable.chat_wall_23, R.drawable.chat_wall_thumb_23),
        ChatWallpaper("wall_24", "Steel", R.drawable.chat_wall_24, R.drawable.chat_wall_thumb_24),
    )

    fun byId(id: String?): ChatWallpaper? = all.firstOrNull { it.id == id }

    fun title(id: String?): String = byId(id)?.title ?: none.title
}
