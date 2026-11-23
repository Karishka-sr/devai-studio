package com.devai.studio.data

enum class StyleBackground { AURORA, SOLID, GRID, DOTS, MESH, PAPER, TERMINAL, NEON, GLASS }
enum class StyleBubble { ROUNDED, SOFT, SHARP, MINIMAL, BUBBLE, TERMINAL, BORDER, FLAT }
enum class StyleNav { FLOATING_PILL, FLAT_BAR, MINIMAL, SEGMENTED, TERMINAL }
enum class StyleFont { SANS, SERIF, MONO }
enum class StyleAvatar { CIRCLE, ROUNDED, SQUARE }

data class StylePalette(
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val userBubble: Long,
    val onUserBubble: Long,
    val aiBubble: Long,
    val onAiBubble: Long,
)

data class UiStyle(
    val id: String,
    val name: String,
    val maker: String,
    val description: String,
    val light: StylePalette,
    val dark: StylePalette,
    val corner: Int = 16,
    val bubble: StyleBubble = StyleBubble.SOFT,
    val background: StyleBackground = StyleBackground.SOLID,
    val nav: StyleNav = StyleNav.FLOATING_PILL,
    val font: StyleFont = StyleFont.SANS,
    val avatar: StyleAvatar = StyleAvatar.CIRCLE,
    val forceDark: Boolean = false,
    val forceLight: Boolean = false,
)

object UiStyleCatalog {
    const val CLASSIC_ID = "classic"

    val all: List<UiStyle> = listOf(
        classic(), chatgpt(), claude(), gemini(), copilot(), perplexity(), grok(),
        deepseek(), mistral(), notion(), linear(), vercel(), github(), discord(),
        slack(), telegram(), whatsapp(), imessage(), materialYou(), materialDesign(),
        materialExpressive(),
        fluent(), macos(), liquidGlass(), glass(), terminal(), nord(), solarized(),
        dracula(), neoBrutalist(), cyberpunk(), paper(), midnight(), mono(), sunset(),
        forest(), ocean(), rose(),
    )

    val classic: UiStyle get() = all.first { it.id == CLASSIC_ID }

    fun byId(id: String?): UiStyle = all.firstOrNull { it.id == id } ?: classic

    fun next(id: String?): UiStyle {
        val index = all.indexOfFirst { it.id == id }.let { if (it < 0) 0 else it }
        return all[(index + 1) % all.size]
    }

    private fun classic() = UiStyle(
        id = CLASSIC_ID,
        name = "Классика DevAI",
        maker = "DevAI Studio",
        description = "Фирменная Aurora-тема и выбор старого раздела «Вид».",
        light = light(
            0xFF5B4BE8, 0xFFF8F7FF, 0xFFFFFFFF,
            onBackground = 0xFF141322, onSurfaceVariant = 0xFF5B5870, outline = 0xFFE3DFF5,
            secondary = 0xFF0B7A6B, tertiary = 0xFFC2415A, surfaceVariant = 0xFFEEEBFF,
        ),
        dark = dark(
            0xFFC9C1FF, 0xFF0B0B14, 0xFF141422,
            onBackground = 0xFFF1EFFF, onSurfaceVariant = 0xFFB9B4D0, outline = 0xFF2A2840,
            secondary = 0xFF6FE7D4, tertiary = 0xFFFFB3C0, surfaceVariant = 0xFF1E1D30,
        ),
        corner = 24, bubble = StyleBubble.ROUNDED, background = StyleBackground.AURORA,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun chatgpt() = UiStyle(
        id = "chatgpt",
        name = "ChatGPT",
        maker = "OpenAI · вдохновение",
        description = "Белый минимализм, серые пузыри и чёрный текст.",
        light = light(
            0xFF10A37F, 0xFFFFFFFF, 0xFFFFFFFF,
            onBackground = 0xFF0D0D0D, onSurfaceVariant = 0xFF6B6B6B, outline = 0xFFE5E5E5,
            secondary = 0xFF10A37F, tertiary = 0xFF0D0D0D, surfaceVariant = 0xFFF4F4F4,
            userBubble = 0xFFF4F4F4, onUserBubble = 0xFF0D0D0D,
        ),
        dark = dark(
            0xFF10A37F, 0xFF212121, 0xFF171717,
            onBackground = 0xFFECECEC, onSurfaceVariant = 0xFFB4B4B4, outline = 0xFF3A3A3A,
            secondary = 0xFF10A37F, tertiary = 0xFFECECEC, surfaceVariant = 0xFF2F2F2F,
            userBubble = 0xFF303030, onUserBubble = 0xFFECECEC, aiBubble = 0xFF212121,
        ),
        corner = 14, bubble = StyleBubble.MINIMAL, background = StyleBackground.SOLID,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun claude() = UiStyle(
        id = "claude",
        name = "Claude",
        maker = "Anthropic · вдохновение",
        description = "Тёплая бумага, терракота и serif-заголовки.",
        light = light(
            0xFFD97757, 0xFFF7F4EE, 0xFFFFFCF7,
            onBackground = 0xFF2D2A26, onSurfaceVariant = 0xFF6F665C, outline = 0xFFE2D9CC,
            secondary = 0xFF8A6F5A, tertiary = 0xFFC15F3C, surfaceVariant = 0xFFEDE6DB,
            userBubble = 0xFFE8DFD3, onUserBubble = 0xFF2D2A26, aiBubble = 0xFFFFFCF7,
        ),
        dark = dark(
            0xFFE08A6B, 0xFF1F1E1D, 0xFF262422,
            onBackground = 0xFFF3EEE7, onSurfaceVariant = 0xFFB9AFA3, outline = 0xFF4A443E,
            secondary = 0xFFB99B80, tertiary = 0xFFE8A07E, surfaceVariant = 0xFF3A3632,
            userBubble = 0xFF3A332D, onUserBubble = 0xFFF3EEE7, aiBubble = 0xFF262422,
        ),
        corner = 20, bubble = StyleBubble.SOFT, background = StyleBackground.PAPER,
        nav = StyleNav.MINIMAL, font = StyleFont.SERIF,
    )

    private fun gemini() = UiStyle(
        id = "gemini",
        name = "Gemini",
        maker = "Google · вдохновение",
        description = "Чистый белый лист и сине-фиолетовые переливы.",
        light = light(
            0xFF4285F4, 0xFFFFFFFF, 0xFFF8F9FA,
            onBackground = 0xFF1F1F1F, onSurfaceVariant = 0xFF5F6368, outline = 0xFFDADCE0,
            secondary = 0xFF9B72CB, tertiary = 0xFFD96570, surfaceVariant = 0xFFE8F0FE,
            userBubble = 0xFFE8F0FE, onUserBubble = 0xFF174EA6,
        ),
        dark = dark(
            0xFF8AB4F8, 0xFF131314, 0xFF1E1F20,
            onBackground = 0xFFE3E3E3, onSurfaceVariant = 0xFF9AA0A6, outline = 0xFF3C4043,
            secondary = 0xFFC58AF9, tertiary = 0xFFF28B82, surfaceVariant = 0xFF2D2F31,
            userBubble = 0xFF364A6B, onUserBubble = 0xFFD3E3FD,
        ),
        corner = 28, bubble = StyleBubble.SOFT, background = StyleBackground.MESH,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun copilot() = UiStyle(
        id = "copilot",
        name = "Copilot",
        maker = "Microsoft · вдохновение",
        description = "Голубые карточки, аккуратная сетка и мягкие углы.",
        light = light(
            0xFF0F6CBD, 0xFFF3F6FC, 0xFFFFFFFF,
            onBackground = 0xFF1B1B1F, onSurfaceVariant = 0xFF5B6577, outline = 0xFFC8D5E8,
            secondary = 0xFF5B5FC7, tertiary = 0xFF8764B8, surfaceVariant = 0xFFE8EEF9,
        ),
        dark = dark(
            0xFF4CC2FF, 0xFF1B1B1F, 0xFF252528,
            onBackground = 0xFFF5F5F5, onSurfaceVariant = 0xFFB8B8C0, outline = 0xFF44444C,
            secondary = 0xFF9B8AFB, tertiary = 0xFFC586C0, surfaceVariant = 0xFF33343A,
            aiBubble = 0xFF252528,
        ),
        corner = 16, bubble = StyleBubble.ROUNDED, background = StyleBackground.GRID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun perplexity() = UiStyle(
        id = "perplexity",
        name = "Perplexity",
        maker = "Perplexity · вдохновение",
        description = "Морской teal, точечная бумага и ответы с источниками.",
        light = light(
            0xFF20808D, 0xFFFCFCF9, 0xFFFFFFFF,
            onBackground = 0xFF13343B, onSurfaceVariant = 0xFF5C7378, outline = 0xFFD0DEDC,
            secondary = 0xFF1FB8CD, tertiary = 0xFFA78BFA, surfaceVariant = 0xFFE8F2F1,
        ),
        dark = dark(
            0xFF2FB8C6, 0xFF0E1719, 0xFF152326,
            onBackground = 0xFFE8F4F2, onSurfaceVariant = 0xFF9BB4B7, outline = 0xFF2B4347,
            secondary = 0xFF5CE1E6, tertiary = 0xFFC4B5FD, surfaceVariant = 0xFF1E3033,
            aiBubble = 0xFF152326,
        ),
        corner = 18, bubble = StyleBubble.MINIMAL, background = StyleBackground.DOTS,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun grok() = UiStyle(
        id = "grok",
        name = "Grok",
        maker = "xAI · вдохновение",
        description = "Чёрно-белый максимализм, острые углы и ноль декора.",
        light = light(
            0xFF000000, 0xFFFFFFFF, 0xFFF7F7F7,
            onBackground = 0xFF0A0A0A, onSurfaceVariant = 0xFF5C5C5C, outline = 0xFFDADADA,
            secondary = 0xFF333333, tertiary = 0xFF666666, surfaceVariant = 0xFFEFEFEF,
        ),
        dark = dark(
            0xFFFFFFFF, 0xFF000000, 0xFF111111,
            onBackground = 0xFFF5F5F5, onSurfaceVariant = 0xFFB5B5B5, outline = 0xFF333333,
            secondary = 0xFFCCCCCC, tertiary = 0xFF999999, surfaceVariant = 0xFF1C1C1C,
            userBubble = 0xFFFFFFFF, onUserBubble = 0xFF000000, aiBubble = 0xFF111111,
        ),
        corner = 4, bubble = StyleBubble.SHARP, background = StyleBackground.SOLID,
        nav = StyleNav.MINIMAL, font = StyleFont.SANS,
    )

    private fun deepseek() = UiStyle(
        id = "deepseek",
        name = "DeepSeek",
        maker = "DeepSeek · вдохновение",
        description = "Глубокий синий, техничная сетка и холодный акцент.",
        light = light(
            0xFF4D6BFE, 0xFFF5F7FF, 0xFFFFFFFF,
            onBackground = 0xFF1A1F36, onSurfaceVariant = 0xFF5C6685, outline = 0xFFD4DCF5,
            secondary = 0xFF00B8D9, tertiary = 0xFF7C3AED, surfaceVariant = 0xFFE8EDFF,
        ),
        dark = dark(
            0xFF6B8AFD, 0xFF0B1020, 0xFF121A30,
            onBackground = 0xFFE6EBFF, onSurfaceVariant = 0xFF9AA7C7, outline = 0xFF2B3D66,
            secondary = 0xFF4DD0E1, tertiary = 0xFFB388FF, surfaceVariant = 0xFF1C2948,
            aiBubble = 0xFF121A30,
        ),
        corner = 14, bubble = StyleBubble.SOFT, background = StyleBackground.GRID,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun mistral() = UiStyle(
        id = "mistral",
        name = "Mistral",
        maker = "Mistral AI · вдохновение",
        description = "Тёплый оранжевый, строгие линии и французский минимализм.",
        light = light(
            0xFFFF7000, 0xFFFFF8F0, 0xFFFFFFFF,
            onBackground = 0xFF2B1B0E, onSurfaceVariant = 0xFF7A6251, outline = 0xFFF3D3B3,
            secondary = 0xFFFFB000, tertiary = 0xFF3D2B1F, surfaceVariant = 0xFFFFE8D1,
        ),
        dark = dark(
            0xFFFF8A2A, 0xFF1A120B, 0xFF241A12,
            onBackground = 0xFFFFF0E2, onSurfaceVariant = 0xFFC9AE95, outline = 0xFF4A3525,
            secondary = 0xFFFFC046, tertiary = 0xFFE6D5C5, surfaceVariant = 0xFF332418,
            userBubble = 0xFFFF7000, onUserBubble = 0xFF1A120B, aiBubble = 0xFF241A12,
        ),
        corner = 10, bubble = StyleBubble.SHARP, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun notion() = UiStyle(
        id = "notion",
        name = "Notion",
        maker = "Notion · вдохновение",
        description = "Бумажный фон, чёрный текст и редакционный serif.",
        light = light(
            0xFF000000, 0xFFF7F6F3, 0xFFFFFFFF,
            onBackground = 0xFF37352F, onSurfaceVariant = 0xFF787774, outline = 0xFFE3E1DB,
            secondary = 0xFF787774, tertiary = 0xFFEB5757, surfaceVariant = 0xFFEFEDE8,
            userBubble = 0xFFEFEDE8, onUserBubble = 0xFF37352F,
        ),
        dark = dark(
            0xFFFFFFFF, 0xFF191919, 0xFF202020,
            onBackground = 0xFFE9E9E7, onSurfaceVariant = 0xFFA6A6A3, outline = 0xFF3A3A3A,
            secondary = 0xFFB8B8B5, tertiary = 0xFFFF7369, surfaceVariant = 0xFF2C2C2C,
            userBubble = 0xFF2C2C2C, onUserBubble = 0xFFE9E9E7, aiBubble = 0xFF202020,
        ),
        corner = 8, bubble = StyleBubble.FLAT, background = StyleBackground.PAPER,
        nav = StyleNav.MINIMAL, font = StyleFont.SERIF,
    )

    private fun linear() = UiStyle(
        id = "linear",
        name = "Linear",
        maker = "Linear · вдохновение",
        description = "Тёмно-фиолетовый акцент, плотная сетка и скорость.",
        light = light(
            0xFF5E6AD2, 0xFFF7F8FA, 0xFFFFFFFF,
            onBackground = 0xFF1F2937, onSurfaceVariant = 0xFF667085, outline = 0xFFD9DEE8,
            secondary = 0xFF26B5CE, tertiary = 0xFFF2994A, surfaceVariant = 0xFFEEF0F5,
        ),
        dark = dark(
            0xFF8B93FF, 0xFF0F1117, 0xFF171A21,
            onBackground = 0xFFEDEFF7, onSurfaceVariant = 0xFF9BA3B4, outline = 0xFF303644,
            secondary = 0xFF5CE1E6, tertiary = 0xFFFFB86B, surfaceVariant = 0xFF222631,
            userBubble = 0xFF5E6AD2, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF171A21,
        ),
        corner = 10, bubble = StyleBubble.ROUNDED, background = StyleBackground.GRID,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun vercel() = UiStyle(
        id = "vercel",
        name = "Vercel",
        maker = "Vercel · вдохновение",
        description = "Чёрное на белом, геометрия и почти нулевые радиусы.",
        light = light(
            0xFF000000, 0xFFFFFFFF, 0xFFFAFAFA,
            onBackground = 0xFF000000, onSurfaceVariant = 0xFF666666, outline = 0xFFE5E5E5,
            secondary = 0xFF666666, tertiary = 0xFF0070F3, surfaceVariant = 0xFFF2F2F2,
        ),
        dark = dark(
            0xFFFFFFFF, 0xFF000000, 0xFF0A0A0A,
            onBackground = 0xFFFAFAFA, onSurfaceVariant = 0xFFA3A3A3, outline = 0xFF333333,
            secondary = 0xFFA3A3A3, tertiary = 0xFF3291FF, surfaceVariant = 0xFF1A1A1A,
            userBubble = 0xFFFFFFFF, onUserBubble = 0xFF000000, aiBubble = 0xFF0A0A0A,
        ),
        corner = 2, bubble = StyleBubble.SHARP, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun github() = UiStyle(
        id = "github",
        name = "GitHub",
        maker = "GitHub · вдохновение",
        description = "Кодовый интерфейс, тонкие рамки и синий акцент.",
        light = light(
            0xFF0969DA, 0xFFFFFFFF, 0xFFF6F8FA,
            onBackground = 0xFF1F2328, onSurfaceVariant = 0xFF656D76, outline = 0xFFD0D7DE,
            secondary = 0xFF1F883D, tertiary = 0xFF8250DF, surfaceVariant = 0xFFF6F8FA,
            aiBubble = 0xFFF6F8FA,
        ),
        dark = dark(
            0xFF58A6FF, 0xFF0D1117, 0xFF161B22,
            onBackground = 0xFFE6EDF3, onSurfaceVariant = 0xFF8D96A0, outline = 0xFF30363D,
            secondary = 0xFF3FB950, tertiary = 0xFFBC8CFF, surfaceVariant = 0xFF21262D,
            userBubble = 0xFF1F6FEB, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF161B22,
        ),
        corner = 6, bubble = StyleBubble.BORDER, background = StyleBackground.GRID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun discord() = UiStyle(
        id = "discord",
        name = "Discord",
        maker = "Discord · вдохновение",
        description = "Тёмный blurple, крупные пузыри и игривый вайб.",
        light = light(
            0xFF5865F2, 0xFF313338, 0xFF2B2D31,
            onBackground = 0xFFF2F3F5, onSurfaceVariant = 0xFFB5BAC1, outline = 0xFF3F4147,
            secondary = 0xFFEB459E, tertiary = 0xFF57F287, surfaceVariant = 0xFF1E1F22,
            userBubble = 0xFF5865F2, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF2B2D31,
        ),
        dark = dark(
            0xFF5865F2, 0xFF313338, 0xFF2B2D31,
            onBackground = 0xFFF2F3F5, onSurfaceVariant = 0xFFB5BAC1, outline = 0xFF3F4147,
            secondary = 0xFFEB459E, tertiary = 0xFF57F287, surfaceVariant = 0xFF1E1F22,
            userBubble = 0xFF5865F2, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF2B2D31,
        ),
        corner = 18, bubble = StyleBubble.BUBBLE, background = StyleBackground.SOLID,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS, forceDark = true,
    )

    private fun slack() = UiStyle(
        id = "slack",
        name = "Slack",
        maker = "Slack · вдохновение",
        description = "Баклажановый, белый и четыре фирменных цвета.",
        light = light(
            0xFF4A154B, 0xFFFFFFFF, 0xFFF8F8F8,
            onBackground = 0xFF1D1C1D, onSurfaceVariant = 0xFF616061, outline = 0xFFDDDDDD,
            secondary = 0xFF36C5F0, tertiary = 0xFFECB22E, surfaceVariant = 0xFFF0E8F0,
        ),
        dark = dark(
            0xFFEBB0F5, 0xFF1A1D21, 0xFF222529,
            onBackground = 0xFFF8F8F8, onSurfaceVariant = 0xFFB9B9B9, outline = 0xFF3F3F3F,
            secondary = 0xFF36C5F0, tertiary = 0xFFECB22E, surfaceVariant = 0xFF2C2D30,
            userBubble = 0xFF4A154B, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF222529,
        ),
        corner = 12, bubble = StyleBubble.SOFT, background = StyleBackground.SOLID,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun telegram() = UiStyle(
        id = "telegram",
        name = "Telegram",
        maker = "Telegram · вдохновение",
        description = "Голубые пузыри, лёгкий фон и быстрые сообщения.",
        light = light(
            0xFF2AABEE, 0xFFFFFFFF, 0xFFF4F4F5,
            onBackground = 0xFF1C1C1E, onSurfaceVariant = 0xFF707579, outline = 0xFFD6EAF5,
            secondary = 0xFF229ED9, tertiary = 0xFF54A9EB, surfaceVariant = 0xFFE9F5FC,
        ),
        dark = dark(
            0xFF2AABEE, 0xFF17212B, 0xFF1E2C3A,
            onBackground = 0xFFF5F5F5, onSurfaceVariant = 0xFFA8B8C5, outline = 0xFF2E4A5E,
            secondary = 0xFF54A9EB, tertiary = 0xFF7CC7F5, surfaceVariant = 0xFF22303D,
            userBubble = 0xFF2AABEE, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF1E2C3A,
        ),
        corner = 16, bubble = StyleBubble.BUBBLE, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun whatsapp() = UiStyle(
        id = "whatsapp",
        name = "WhatsApp",
        maker = "Meta · вдохновение",
        description = "Зелёные пузыри, тёплый чат и уютный ритм.",
        light = light(
            0xFF25D366, 0xFFE5DDD5, 0xFFFFFFFF,
            onBackground = 0xFF111B21, onSurfaceVariant = 0xFF667781, outline = 0xFFD1D7DB,
            secondary = 0xFF128C7E, tertiary = 0xFF075E54, surfaceVariant = 0xFFF0F2F5,
            userBubble = 0xFFD9FDD3, onUserBubble = 0xFF111B21,
        ),
        dark = dark(
            0xFF25D366, 0xFF0B141A, 0xFF202C33,
            onBackground = 0xFFE9EDEF, onSurfaceVariant = 0xFF8696A0, outline = 0xFF3B4A54,
            secondary = 0xFF00A884, tertiary = 0xFF53BDEB, surfaceVariant = 0xFF2A3942,
            userBubble = 0xFF005C4B, onUserBubble = 0xFFE9EDEF, aiBubble = 0xFF202C33,
        ),
        corner = 14, bubble = StyleBubble.BUBBLE, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun imessage() = UiStyle(
        id = "imessage",
        name = "iMessage",
        maker = "Apple · вдохновение",
        description = "Синие и серые пузыри, белый экран и SF-ритм.",
        light = light(
            0xFF007AFF, 0xFFFFFFFF, 0xFFF2F2F7,
            onBackground = 0xFF000000, onSurfaceVariant = 0xFF6C6C70, outline = 0xFFD1D1D6,
            secondary = 0xFF34C759, tertiary = 0xFFFF9500, surfaceVariant = 0xFFE9E9EB,
            userBubble = 0xFF007AFF, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFFE9E9EB,
        ),
        dark = dark(
            0xFF0A84FF, 0xFF000000, 0xFF1C1C1E,
            onBackground = 0xFFFFFFFF, onSurfaceVariant = 0xFF8E8E93, outline = 0xFF3A3A3C,
            secondary = 0xFF30D158, tertiary = 0xFFFF9F0A, surfaceVariant = 0xFF2C2C2E,
            userBubble = 0xFF0A84FF, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF262628,
        ),
        corner = 20, bubble = StyleBubble.BUBBLE, background = StyleBackground.SOLID,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun materialYou() = UiStyle(
        id = "material-you",
        name = "Material You",
        maker = "Google · Material 3",
        description = "Мягкие пастельные тона и очень крупные радиусы.",
        light = light(
            0xFF6750A4, 0xFFFEF7FF, 0xFFFFFBFE,
            onBackground = 0xFF1D1B20, onSurfaceVariant = 0xFF49454F, outline = 0xFFCAC4D0,
            secondary = 0xFF625B71, tertiary = 0xFF7D5260, surfaceVariant = 0xFFE7E0EC,
            aiBubble = 0xFFF3EDF7,
        ),
        dark = dark(
            0xFFD0BCFF, 0xFF141218, 0xFF1D1B20,
            onBackground = 0xFFE6E0E9, onSurfaceVariant = 0xFFCAC4D0, outline = 0xFF938F99,
            secondary = 0xFFCCC2DC, tertiary = 0xFFEFB8C8, surfaceVariant = 0xFF49454F,
            userBubble = 0xFFD0BCFF, onUserBubble = 0xFF381E72, aiBubble = 0xFF2B2930,
        ),
        corner = 30, bubble = StyleBubble.SOFT, background = StyleBackground.MESH,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun materialDesign() = UiStyle(
        id = "material-design",
        name = "Material Design",
        maker = "Google · Material 2",
        description = "Базовый Material: фиолетовый primary, белые карточки и тени.",
        light = light(
            0xFF6200EE, 0xFFFAFAFA, 0xFFFFFFFF,
            onBackground = 0xFF212121, onSurfaceVariant = 0xFF757575, outline = 0xFFE0E0E0,
            secondary = 0xFF03DAC6, tertiary = 0xFF018786, surfaceVariant = 0xFFF5F5F5,
            onPrimary = 0xFFFFFFFF, userBubble = 0xFF6200EE, onUserBubble = 0xFFFFFFFF,
        ),
        dark = dark(
            0xFFBB86FC, 0xFF121212, 0xFF1E1E1E,
            onBackground = 0xFFE0E0E0, onSurfaceVariant = 0xFFBDBDBD, outline = 0xFF3A3A3A,
            secondary = 0xFF03DAC6, tertiary = 0xFF3700B3, surfaceVariant = 0xFF2C2C2C,
            userBubble = 0xFFBB86FC, onUserBubble = 0xFF121212, aiBubble = 0xFF1E1E1E,
        ),
        corner = 16, bubble = StyleBubble.ROUNDED, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun materialExpressive() = UiStyle(
        id = "material-expressive",
        name = "Material Expressive 3",
        maker = "Google · M3 Expressive",
        description = "Смелые формы, крупная типографика и живые акценты.",
        light = light(
            0xFF7C4DFF, 0xFFFFF7FF, 0xFFFFFFFF,
            onBackground = 0xFF1C1B1F, onSurfaceVariant = 0xFF5B5367, outline = 0xFFC9BEE0,
            secondary = 0xFF0E9F8A, tertiary = 0xFFFF4081, surfaceVariant = 0xFFEEE7F7,
        ),
        dark = dark(
            0xFFD0BCFF, 0xFF0F0A1A, 0xFF1A1328,
            onBackground = 0xFFF1EFFF, onSurfaceVariant = 0xFFC9C0D8, outline = 0xFF514861,
            secondary = 0xFF6FE7D4, tertiary = 0xFFEFB8C8, surfaceVariant = 0xFF262033,
            userBubble = 0xFFD0BCFF, onUserBubble = 0xFF381E72, aiBubble = 0xFF1A1328,
        ),
        corner = 34, bubble = StyleBubble.ROUNDED, background = StyleBackground.MESH,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun fluent() = UiStyle(
        id = "fluent",
        name = "Fluent",
        maker = "Microsoft · Fluent 2",
        description = "Акриловые слои, синий акцент и спокойные панели.",
        light = light(
            0xFF0067C0, 0xFFF3F3F3, 0xFFFFFFFF,
            onBackground = 0xFF1A1A1A, onSurfaceVariant = 0xFF5C5C5C, outline = 0xFFD6D6D6,
            secondary = 0xFF005FB8, tertiary = 0xFF8A8886, surfaceVariant = 0xFFEAEAEA,
        ),
        dark = dark(
            0xFF4CC2FF, 0xFF202020, 0xFF2B2B2B,
            onBackground = 0xFFF5F5F5, onSurfaceVariant = 0xFFC7C7C7, outline = 0xFF444444,
            secondary = 0xFF60CDFF, tertiary = 0xFFBDBDBD, surfaceVariant = 0xFF323232,
            userBubble = 0xFF0067C0, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF2B2B2B,
        ),
        corner = 8, bubble = StyleBubble.SOFT, background = StyleBackground.GLASS,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun macos() = UiStyle(
        id = "macos",
        name = "macOS",
        maker = "Apple · вдохновение",
        description = "Серое стекло, тонкие линии и системный синий.",
        light = light(
            0xFF007AFF, 0xFFECECEC, 0xFFFFFFFF,
            onBackground = 0xFF1E1E1E, onSurfaceVariant = 0xFF6E6E73, outline = 0xFFD0D0D0,
            secondary = 0xFF34C759, tertiary = 0xFFFF3B30, surfaceVariant = 0xFFE1E1E1,
        ),
        dark = dark(
            0xFF0A84FF, 0xFF1E1E1E, 0xFF2C2C2E,
            onBackground = 0xFFF5F5F7, onSurfaceVariant = 0xFFA1A1A6, outline = 0xFF48484A,
            secondary = 0xFF30D158, tertiary = 0xFFFF453A, surfaceVariant = 0xFF3A3A3C,
            userBubble = 0xFF0A84FF, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF2C2C2E,
        ),
        corner = 12, bubble = StyleBubble.SOFT, background = StyleBackground.GLASS,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun liquidGlass() = UiStyle(
        id = "liquid-glass",
        name = "Liquid Glass",
        maker = "Apple · вдохновение",
        description = "Прозрачные слои, блики и плавные капли.",
        light = light(
            0xFF3B82F6, 0xFFEAF2FF, 0xFFFFFFFF,
            onBackground = 0xFF0F1B33, onSurfaceVariant = 0xFF5D6B85, outline = 0xFFC7D8F0,
            secondary = 0xFF8B5CF6, tertiary = 0xFF22D3EE, surfaceVariant = 0xFFDCE9FF,
        ),
        dark = dark(
            0xFF8AB4FF, 0xFF07111F, 0xFF0E1B2E,
            onBackground = 0xFFEAF2FF, onSurfaceVariant = 0xFFA6B8D4, outline = 0xFF263D5C,
            secondary = 0xFFC4B5FD, tertiary = 0xFF67E8F9, surfaceVariant = 0xFF16263D,
            userBubble = 0xFF4F7CFF, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF0E1B2E,
        ),
        corner = 28, bubble = StyleBubble.SOFT, background = StyleBackground.GLASS,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun glass() = UiStyle(
        id = "glass",
        name = "Glass",
        maker = "DevAI Studio",
        description = "Мягкое стекло, светлые панели и голубой акцент.",
        light = light(
            0xFF4F7CFF, 0xFFEAF2FF, 0xFFFFFFFF,
            onBackground = 0xFF14213D, onSurfaceVariant = 0xFF5D6B85, outline = 0xFFC7D8F0,
            secondary = 0xFF8B5CF6, tertiary = 0xFF22D3EE, surfaceVariant = 0xFFDCE9FF,
        ),
        dark = dark(
            0xFF8AB4FF, 0xFF07111F, 0xFF0E1B2E,
            onBackground = 0xFFEAF2FF, onSurfaceVariant = 0xFFA6B8D4, outline = 0xFF263D5C,
            secondary = 0xFFC4B5FD, tertiary = 0xFF67E8F9, surfaceVariant = 0xFF16263D,
            userBubble = 0xFF4F7CFF, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF0E1B2E,
        ),
        corner = 24, bubble = StyleBubble.SOFT, background = StyleBackground.GLASS,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun terminal() = UiStyle(
        id = "terminal",
        name = "Terminal",
        maker = "DevAI Studio",
        description = "Чёрный экран, зелёный фосфор и моноширинный шрифт.",
        light = light(
            0xFF00FF66, 0xFF0C0C0C, 0xFF111111,
            onBackground = 0xFFD7FFD7, onSurfaceVariant = 0xFF7FBF7F, outline = 0xFF1F3A1F,
            secondary = 0xFF00C853, tertiary = 0xFF7CFFB2, surfaceVariant = 0xFF162016,
            userBubble = 0xFF00FF66, onUserBubble = 0xFF001A00, aiBubble = 0xFF111111,
        ),
        dark = dark(
            0xFF00FF66, 0xFF0C0C0C, 0xFF111111,
            onBackground = 0xFFD7FFD7, onSurfaceVariant = 0xFF7FBF7F, outline = 0xFF1F3A1F,
            secondary = 0xFF00C853, tertiary = 0xFF7CFFB2, surfaceVariant = 0xFF162016,
            userBubble = 0xFF00FF66, onUserBubble = 0xFF001A00, aiBubble = 0xFF111111,
        ),
        corner = 0, bubble = StyleBubble.TERMINAL, background = StyleBackground.TERMINAL,
        nav = StyleNav.TERMINAL, font = StyleFont.MONO, forceDark = true,
    )

    private fun nord() = UiStyle(
        id = "nord",
        name = "Nord",
        maker = "Nord · палитра",
        description = "Арктический сине-серый, снег и спокойствие.",
        light = light(
            0xFF5E81AC, 0xFFECEFF4, 0xFFFFFFFF,
            onBackground = 0xFF2E3440, onSurfaceVariant = 0xFF4C566A, outline = 0xFFD8DEE9,
            secondary = 0xFF88C0D0, tertiary = 0xFFB48EAD, surfaceVariant = 0xFFE5E9F0,
        ),
        dark = dark(
            0xFF88C0D0, 0xFF2E3440, 0xFF3B4252,
            onBackground = 0xFFECEFF4, onSurfaceVariant = 0xFFD8DEE9, outline = 0xFF4C566A,
            secondary = 0xFF81A1C1, tertiary = 0xFFB48EAD, surfaceVariant = 0xFF434C5E,
            userBubble = 0xFF5E81AC, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF3B4252,
        ),
        corner = 8, bubble = StyleBubble.SOFT, background = StyleBackground.SOLID,
        nav = StyleNav.MINIMAL, font = StyleFont.SANS,
    )

    private fun solarized() = UiStyle(
        id = "solarized",
        name = "Solarized",
        maker = "Ethan Schoonover · палитра",
        description = "Тёплая бумага, teal и выверенная контрастность.",
        light = light(
            0xFF268BD2, 0xFFFDF6E3, 0xFFFFFBF0,
            onBackground = 0xFF586E75, onSurfaceVariant = 0xFF657B83, outline = 0xFFD8CFB5,
            secondary = 0xFF2AA198, tertiary = 0xFFB58900, surfaceVariant = 0xFFEEE8D5,
        ),
        dark = dark(
            0xFF2AA198, 0xFF002B36, 0xFF073642,
            onBackground = 0xFFEEE8D5, onSurfaceVariant = 0xFF93A1A1, outline = 0xFF1E5B66,
            secondary = 0xFF268BD2, tertiary = 0xFFB58900, surfaceVariant = 0xFF0B4A56,
            userBubble = 0xFF268BD2, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF073642,
        ),
        corner = 10, bubble = StyleBubble.MINIMAL, background = StyleBackground.PAPER,
        nav = StyleNav.SEGMENTED, font = StyleFont.MONO, forceLight = true,
    )

    private fun dracula() = UiStyle(
        id = "dracula",
        name = "Dracula",
        maker = "Dracula · палитра",
        description = "Фиолетовая ночь с розовым и мятным неоном.",
        light = light(
            0xFFBD93F9, 0xFF282A36, 0xFF343746,
            onBackground = 0xFFF8F8F2, onSurfaceVariant = 0xFFB8B8C8, outline = 0xFF44475A,
            secondary = 0xFFFF79C6, tertiary = 0xFF50FA7B, surfaceVariant = 0xFF44475A,
            userBubble = 0xFF6272A4, onUserBubble = 0xFFF8F8F2, aiBubble = 0xFF343746,
        ),
        dark = dark(
            0xFFBD93F9, 0xFF282A36, 0xFF343746,
            onBackground = 0xFFF8F8F2, onSurfaceVariant = 0xFFB8B8C8, outline = 0xFF44475A,
            secondary = 0xFFFF79C6, tertiary = 0xFF50FA7B, surfaceVariant = 0xFF44475A,
            userBubble = 0xFF6272A4, onUserBubble = 0xFFF8F8F2, aiBubble = 0xFF343746,
        ),
        corner = 10, bubble = StyleBubble.SOFT, background = StyleBackground.SOLID,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS, forceDark = true,
    )

    private fun neoBrutalist() = UiStyle(
        id = "neo-brutalist",
        name = "Neo-Brutalist",
        maker = "DevAI Studio",
        description = "Жёлтый фон, чёрные рамки и нарочито жёсткие углы.",
        light = light(
            0xFF000000, 0xFFFFF9C4, 0xFFFFFFFF,
            onBackground = 0xFF000000, onSurfaceVariant = 0xFF333333, outline = 0xFF000000,
            secondary = 0xFFFF5252, tertiary = 0xFFFFD600, surfaceVariant = 0xFFFFEB3B,
            userBubble = 0xFF000000, onUserBubble = 0xFFFFFFFF,
        ),
        dark = dark(
            0xFF000000, 0xFFFFF9C4, 0xFFFFFFFF,
            onBackground = 0xFF000000, onSurfaceVariant = 0xFF333333, outline = 0xFF000000,
            secondary = 0xFFFF5252, tertiary = 0xFFFFD600, surfaceVariant = 0xFFFFEB3B,
            userBubble = 0xFF000000, onUserBubble = 0xFFFFFFFF,
        ),
        corner = 0, bubble = StyleBubble.BORDER, background = StyleBackground.GRID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.MONO, forceLight = true,
    )

    private fun cyberpunk() = UiStyle(
        id = "cyberpunk",
        name = "Cyberpunk",
        maker = "DevAI Studio",
        description = "Чёрный корпус, циан и маджента, как в Night City.",
        light = light(
            0xFF00F0FF, 0xFF05010D, 0xFF0D0718,
            onBackground = 0xFFE8F9FF, onSurfaceVariant = 0xFF9AA8C7, outline = 0xFF3D1B5C,
            secondary = 0xFFFF00A8, tertiary = 0xFFFFE600, surfaceVariant = 0xFF1A1030,
            userBubble = 0xFFFF00A8, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF0D0718,
        ),
        dark = dark(
            0xFF00F0FF, 0xFF05010D, 0xFF0D0718,
            onBackground = 0xFFE8F9FF, onSurfaceVariant = 0xFF9AA8C7, outline = 0xFF3D1B5C,
            secondary = 0xFFFF00A8, tertiary = 0xFFFFE600, surfaceVariant = 0xFF1A1030,
            userBubble = 0xFFFF00A8, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF0D0718,
        ),
        corner = 2, bubble = StyleBubble.TERMINAL, background = StyleBackground.NEON,
        nav = StyleNav.TERMINAL, font = StyleFont.MONO, forceDark = true,
    )

    private fun paper() = UiStyle(
        id = "paper",
        name = "Paper",
        maker = "DevAI Studio",
        description = "Сепия, книжная типографика и мягкая бумага.",
        light = light(
            0xFF5B4636, 0xFFF4ECD8, 0xFFFFF9EA,
            onBackground = 0xFF3E2F24, onSurfaceVariant = 0xFF736253, outline = 0xFFD2C1A5,
            secondary = 0xFF8B5E3C, tertiary = 0xFFA67C52, surfaceVariant = 0xFFE8DCC4,
            userBubble = 0xFFE8DCC4, onUserBubble = 0xFF3E2F24,
        ),
        dark = dark(
            0xFFD8C3A5, 0xFF241E18, 0xFF2E261E,
            onBackground = 0xFFF3E9DA, onSurfaceVariant = 0xFFB9A994, outline = 0xFF554433,
            secondary = 0xFFB08968, tertiary = 0xFFD0A87A, surfaceVariant = 0xFF3B3025,
            userBubble = 0xFF3B3025, onUserBubble = 0xFFF3E9DA, aiBubble = 0xFF2E261E,
        ),
        corner = 6, bubble = StyleBubble.FLAT, background = StyleBackground.PAPER,
        nav = StyleNav.MINIMAL, font = StyleFont.SERIF, forceLight = true,
    )

    private fun midnight() = UiStyle(
        id = "midnight",
        name = "Midnight",
        maker = "DevAI Studio",
        description = "Полночный синий, золото и мягкий свет.",
        light = light(
            0xFFB8860B, 0xFFF5F7FF, 0xFFFFFFFF,
            onBackground = 0xFF11162E, onSurfaceVariant = 0xFF5A6485, outline = 0xFFD4DBF0,
            secondary = 0xFF3B82F6, tertiary = 0xFF8B5CF6, surfaceVariant = 0xFFE8ECFA,
        ),
        dark = dark(
            0xFFE6C15A, 0xFF0B1026, 0xFF141B38,
            onBackground = 0xFFE8ECFF, onSurfaceVariant = 0xFFA5B0D0, outline = 0xFF33406A,
            secondary = 0xFF7AA2F7, tertiary = 0xFFBB9AF7, surfaceVariant = 0xFF1E2748,
            userBubble = 0xFF7AA2F7, onUserBubble = 0xFF0B1026, aiBubble = 0xFF141B38,
        ),
        corner = 14, bubble = StyleBubble.SOFT, background = StyleBackground.NEON,
        nav = StyleNav.SEGMENTED, font = StyleFont.SERIF, forceDark = true,
    )

    private fun mono() = UiStyle(
        id = "mono",
        name = "Mono",
        maker = "DevAI Studio",
        description = "Строгая монохромная сетка без лишнего цвета.",
        light = light(
            0xFF111111, 0xFFFAFAFA, 0xFFFFFFFF,
            onBackground = 0xFF111111, onSurfaceVariant = 0xFF666666, outline = 0xFFD0D0D0,
            secondary = 0xFF555555, tertiary = 0xFF999999, surfaceVariant = 0xFFEFEFEF,
        ),
        dark = dark(
            0xFFFFFFFF, 0xFF0A0A0A, 0xFF141414,
            onBackground = 0xFFF0F0F0, onSurfaceVariant = 0xFFA0A0A0, outline = 0xFF333333,
            secondary = 0xFFB0B0B0, tertiary = 0xFF808080, surfaceVariant = 0xFF1F1F1F,
            userBubble = 0xFFFFFFFF, onUserBubble = 0xFF000000, aiBubble = 0xFF141414,
        ),
        corner = 0, bubble = StyleBubble.SHARP, background = StyleBackground.GRID,
        nav = StyleNav.MINIMAL, font = StyleFont.MONO,
    )

    private fun sunset() = UiStyle(
        id = "sunset",
        name = "Sunset",
        maker = "DevAI Studio",
        description = "Тёплый закат, розовый и фиолетовый градиент.",
        light = light(
            0xFFF97316, 0xFFFFF1E6, 0xFFFFFFFF,
            onBackground = 0xFF3B1E12, onSurfaceVariant = 0xFF7A5544, outline = 0xFFF5C6A8,
            secondary = 0xFFEC4899, tertiary = 0xFF8B5CF6, surfaceVariant = 0xFFFFE0CC,
        ),
        dark = dark(
            0xFFFB923C, 0xFF1F120C, 0xFF2A1810,
            onBackground = 0xFFFFEDE1, onSurfaceVariant = 0xFFC9A08A, outline = 0xFF5A3320,
            secondary = 0xFFF472B6, tertiary = 0xFFA78BFA, surfaceVariant = 0xFF3A2116,
            userBubble = 0xFFF97316, onUserBubble = 0xFF1F120C, aiBubble = 0xFF2A1810,
        ),
        corner = 22, bubble = StyleBubble.ROUNDED, background = StyleBackground.MESH,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun forest() = UiStyle(
        id = "forest",
        name = "Forest",
        maker = "DevAI Studio",
        description = "Хвойный зелёный, мята и ощущение тишины.",
        light = light(
            0xFF166534, 0xFFF0FDF4, 0xFFFFFFFF,
            onBackground = 0xFF14351F, onSurfaceVariant = 0xFF52705B, outline = 0xFFBBF7D0,
            secondary = 0xFF15803D, tertiary = 0xFFA16207, surfaceVariant = 0xFFDCFCE7,
        ),
        dark = dark(
            0xFF4ADE80, 0xFF052E16, 0xFF0A3D1F,
            onBackground = 0xFFDCFCE7, onSurfaceVariant = 0xFF9BC7A8, outline = 0xFF166534,
            secondary = 0xFF34D399, tertiary = 0xFFFBBF24, surfaceVariant = 0xFF14532D,
            userBubble = 0xFF166534, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF0A3D1F,
        ),
        corner = 12, bubble = StyleBubble.SOFT, background = StyleBackground.SOLID,
        nav = StyleNav.FLAT_BAR, font = StyleFont.SANS,
    )

    private fun ocean() = UiStyle(
        id = "ocean",
        name = "Ocean",
        maker = "DevAI Studio",
        description = "Глубокий циан, морская пена и мягкие волны.",
        light = light(
            0xFF0E7490, 0xFFECFEFF, 0xFFFFFFFF,
            onBackground = 0xFF083344, onSurfaceVariant = 0xFF4F6D75, outline = 0xFFA5F3FC,
            secondary = 0xFF0891B2, tertiary = 0xFF14B8A6, surfaceVariant = 0xFFCFFAFE,
        ),
        dark = dark(
            0xFF22D3EE, 0xFF042F3A, 0xFF063D4A,
            onBackground = 0xFFCFFAFE, onSurfaceVariant = 0xFF93BAC2, outline = 0xFF0E7490,
            secondary = 0xFF5EEAD4, tertiary = 0xFF38BDF8, surfaceVariant = 0xFF0A4E5E,
            userBubble = 0xFF0E7490, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF063D4A,
        ),
        corner = 20, bubble = StyleBubble.SOFT, background = StyleBackground.MESH,
        nav = StyleNav.SEGMENTED, font = StyleFont.SANS,
    )

    private fun rose() = UiStyle(
        id = "rose",
        name = "Rose",
        maker = "DevAI Studio",
        description = "Розовые акценты, мягкий свет и лёгкий характер.",
        light = light(
            0xFFE11D48, 0xFFFFF1F2, 0xFFFFFFFF,
            onBackground = 0xFF4C0519, onSurfaceVariant = 0xFF7A4A57, outline = 0xFFFECDD3,
            secondary = 0xFFBE123C, tertiary = 0xFFF59E0B, surfaceVariant = 0xFFFFE4E6,
        ),
        dark = dark(
            0xFFFB7185, 0xFF2B0712, 0xFF3B0A19,
            onBackground = 0xFFFFE4E6, onSurfaceVariant = 0xFFC99AA5, outline = 0xFF881337,
            secondary = 0xFFFDA4AF, tertiary = 0xFFFCD34D, surfaceVariant = 0xFF4C0519,
            userBubble = 0xFFE11D48, onUserBubble = 0xFFFFFFFF, aiBubble = 0xFF3B0A19,
        ),
        corner = 22, bubble = StyleBubble.ROUNDED, background = StyleBackground.MESH,
        nav = StyleNav.FLOATING_PILL, font = StyleFont.SANS,
    )

    private fun light(
        primary: Long,
        background: Long,
        surface: Long,
        onBackground: Long = 0xFF111111,
        onSurfaceVariant: Long = 0xFF666666,
        outline: Long = 0xFFDDDDDD,
        secondary: Long = primary,
        tertiary: Long = secondary,
        onPrimary: Long = 0xFFFFFFFF,
        surfaceVariant: Long = background,
        userBubble: Long = primary,
        onUserBubble: Long = onPrimary,
        aiBubble: Long = surface,
        onAiBubble: Long = onBackground,
    ) = StylePalette(
        primary = primary, onPrimary = onPrimary,
        secondary = secondary, onSecondary = onPrimary,
        tertiary = tertiary, onTertiary = onPrimary,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        userBubble = userBubble, onUserBubble = onUserBubble,
        aiBubble = aiBubble, onAiBubble = onAiBubble,
    )

    private fun dark(
        primary: Long,
        background: Long,
        surface: Long,
        onBackground: Long = 0xFFF5F5F5,
        onSurfaceVariant: Long = 0xFFB0B0B0,
        outline: Long = 0xFF3A3A3A,
        secondary: Long = primary,
        tertiary: Long = secondary,
        onPrimary: Long = 0xFF111111,
        surfaceVariant: Long = surface,
        userBubble: Long = primary,
        onUserBubble: Long = onPrimary,
        aiBubble: Long = surface,
        onAiBubble: Long = onBackground,
    ) = StylePalette(
        primary = primary, onPrimary = onPrimary,
        secondary = secondary, onSecondary = onPrimary,
        tertiary = tertiary, onTertiary = onPrimary,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        userBubble = userBubble, onUserBubble = onUserBubble,
        aiBubble = aiBubble, onAiBubble = onAiBubble,
    )
}
