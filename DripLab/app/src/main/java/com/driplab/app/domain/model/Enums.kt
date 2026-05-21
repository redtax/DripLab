package com.driplab.app.domain.model

enum class BrewMethod(val displayName: String, val iconName: String) {
    POUR_OVER("手冲咖啡", "coffee"),
    FRENCH_PRESS("法压壶", "coffee_maker"),
    AERO_PRESS("爱乐压", "espresso"),
    MOKA_POT("摩卡壶", "kettle"),
    COLD_BREW("冷萃", "ac_unit"),
    SIPHON("虹吸壶", "science")
}

enum class BrewPhase {
    IDLE,
    BLOOM,
    POUR,
    WAIT,
    STEEP,
    PRESS,
    COMPLETE
}

enum class AlertMode(val displayName: String, val hasSound: Boolean, val hasVibration: Boolean) {
    SILENT("静音", false, false),
    GENTLE("轻柔", true, true),
    STANDARD("标准", true, true),
    SOLO("独享", false, true)
}