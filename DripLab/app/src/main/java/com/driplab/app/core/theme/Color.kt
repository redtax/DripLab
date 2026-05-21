package com.driplab.app.core.theme

import androidx.compose.ui.graphics.Color

enum class DripTheme(val displayName: String) {
    CLASSIC("经典暖棕"),
    DARK_ROAST("深焙暗黑"),
    MINT_COLD_BREW("薄荷冷萃"),
    OAT_LATTE("燕麦拿铁"),
    MOCHA_GRADIENT("摩卡渐变")
}

data class DripColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color,
    val outline: Color,
    val accent: Color,
    val cardBackground: Color,
    val timerActive: Color,
    val timerInactive: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

object DripColorPalette {

    val classic = DripColors(
        primary = Color(0xFF8B5A2B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC2),
        onPrimaryContainer = Color(0xFF2E1600),
        secondary = Color(0xFF755845),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDCC2),
        onSecondaryContainer = Color(0xFF2A1708),
        tertiary = Color(0xFFD4A574),
        onTertiary = Color(0xFF2E1600),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF3D2914),
        surface = Color(0xFFF5E6D3),
        onSurface = Color(0xFF3D2914),
        surfaceVariant = Color(0xFFF2DFCE),
        onSurfaceVariant = Color(0xFF51443A),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF837469),
        accent = Color(0xFFD4A574),
        cardBackground = Color(0xFFF5E6D3),
        timerActive = Color(0xFF8B5A2B),
        timerInactive = Color(0xFFD4A574),
        gradientStart = Color(0xFF8B5A2B),
        gradientEnd = Color(0xFFD4A574)
    )

    val darkRoast = DripColors(
        primary = Color(0xFF1A1A2E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF16213E),
        onPrimaryContainer = Color(0xFFEAEAEA),
        secondary = Color(0xFFE94560),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF3D1A2B),
        onSecondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFFE94560),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFF0F0F23),
        onBackground = Color(0xFFEAEAEA),
        surface = Color(0xFF16213E),
        onSurface = Color(0xFFEAEAEA),
        surfaceVariant = Color(0xFF1A2540),
        onSurfaceVariant = Color(0xFFC4C6D0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        outline = Color(0xFF8E9099),
        accent = Color(0xFFE94560),
        cardBackground = Color(0xFF16213E),
        timerActive = Color(0xFFE94560),
        timerInactive = Color(0xFF4A4A6A),
        gradientStart = Color(0xFF1A1A2E),
        gradientEnd = Color(0xFFE94560)
    )

    val mintColdBrew = DripColors(
        primary = Color(0xFF2C5F5D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB1EBE7),
        onPrimaryContainer = Color(0xFF00201E),
        secondary = Color(0xFF5BA8A3),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCF8F4),
        onSecondaryContainer = Color(0xFF051F1E),
        tertiary = Color(0xFF5BA8A3),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFAFDFB),
        onBackground = Color(0xFF1A3C3A),
        surface = Color(0xFFE8F4F2),
        onSurface = Color(0xFF1A3C3A),
        surfaceVariant = Color(0xFFDAE5E3),
        onSurfaceVariant = Color(0xFF3F4948),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF6F7978),
        accent = Color(0xFF5BA8A3),
        cardBackground = Color(0xFFE8F4F2),
        timerActive = Color(0xFF2C5F5D),
        timerInactive = Color(0xFF9ECFCC),
        gradientStart = Color(0xFF2C5F5D),
        gradientEnd = Color(0xFF5BA8A3)
    )

    val oatLatte = DripColors(
        primary = Color(0xFF6B5B4F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF5DED1),
        onPrimaryContainer = Color(0xFF241911),
        secondary = Color(0xFFC4A77D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEBDFCF),
        onSecondaryContainer = Color(0xFF1F1B18),
        tertiary = Color(0xFFC4A77D),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF3D3630),
        surface = Color(0xFFF7F3EF),
        onSurface = Color(0xFF3D3630),
        surfaceVariant = Color(0xFFF0E0D8),
        onSurfaceVariant = Color(0xFF50453D),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF81756C),
        accent = Color(0xFFC4A77D),
        cardBackground = Color(0xFFF7F3EF),
        timerActive = Color(0xFF6B5B4F),
        timerInactive = Color(0xFFD4C4B5),
        gradientStart = Color(0xFF6B5B4F),
        gradientEnd = Color(0xFFC4A77D)
    )

    val mochaGradient = DripColors(
        primary = Color(0xFF4A2C2A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD4),
        onPrimaryContainer = Color(0xFF3B0909),
        secondary = Color(0xFFC06C4F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD1),
        onSecondaryContainer = Color(0xFF3B0909),
        tertiary = Color(0xFFE8A87C),
        onTertiary = Color(0xFF3B0909),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF2D1B18),
        surface = Color(0xFFFDF8F3),
        onSurface = Color(0xFF2D1B18),
        surfaceVariant = Color(0xFFF5DDD8),
        onSurfaceVariant = Color(0xFF534341),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF857370),
        accent = Color(0xFFE8A87C),
        cardBackground = Color(0xFFFDF8F3),
        timerActive = Color(0xFFC06C4F),
        timerInactive = Color(0xFFF0C8A8),
        gradientStart = Color(0xFFC06C4F),
        gradientEnd = Color(0xFFE8A87C)
    )

    fun fromTheme(theme: DripTheme): DripColors = when (theme) {
        DripTheme.CLASSIC -> classic
        DripTheme.DARK_ROAST -> darkRoast
        DripTheme.MINT_COLD_BREW -> mintColdBrew
        DripTheme.OAT_LATTE -> oatLatte
        DripTheme.MOCHA_GRADIENT -> mochaGradient
    }
}