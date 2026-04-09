package com.example.urbaneye.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * UrbanEye Design System — Refined "Bankme" Aesthetic
 * Updated to match the soft pastel and high-contrast dark theme in the provided image.
 */
object UrbanEyeColors {

    // ── Brand Pastels (from the light screens) ────────────────
    val Lavender      = Color(0xFFE5E0F2) // Main background color
    val Mint          = Color(0xFFB8E1D8) // Teal circle
    val PaleViolet    = Color(0xFFD6CDEB) // Bill card background
    val SoftBlue      = Color(0xFFD0E3F2) // Alternative card background

    // ── Primary Brand ──────────────────────────────────────────
    val BankBlack     = Color(0xFF000000) // Primary text & Dark mode background
    val PureWhite     = Color(0xFFFFFFFF) // Light mode surfaces

    // ── Status & Functional ────────────────────────────────────
    val SuccessGreen  = Color(0xFF22C55E)
    val ErrorRed      = Color(0xFFEF4444)
    val PendingGold   = Color(0xFFFBBF24)

    // ── Neutral Grays (Subtle) ─────────────────────────────────
    val Gray100       = Color(0xFFF5F5F5)
    val Gray200       = Color(0xFFEEEEEE)
    val Gray400       = Color(0xFFAAAAAA)
    val Gray600       = Color(0xFF666666)
    val Gray800       = Color(0xFF222222)

    // ── Dark Mode Specifics (Deep Black Experience) ───────────
    val DeepBlack     = Color(0xFF050505)
    val SurfaceDark   = Color(0xFF121212)
    val CardDark      = Color(0xFF1A1A1A)

    // ── Accents ───────────────────────────────────────────────
    // Representing the holographic/gradient effect in the UI
    val HoloPurple    = Color(0xFFA78BFA)
    val HoloTeal      = Color(0xFF5EEAD4)

    // ── Professional Neutrals (Slate & Zinc influence) ─────────
// ── 1. The "Cyber" Tones (High energy, great for Dark Mode) ──
    val ElectricCyan    = Color(0xFF06B6D4) // Sharp, technical, high-contrast
    val NeonLime        = Color(0xFF84CC16) // "Active" status, energy, movement
    val VividOrange     = Color(0xFFF97316) // Warm, friendly, but pops hard on black

    // ── 2. The "Luxury" Tones (Sophisticated & Expensive) ──────
    val RoyalEmerald    = Color(0xFF10B981) // Trustworthy, financial, smooth
    val SunsetCrimson   = Color(0xFFE11D48) // Sophisticated alternative to "Error Red"
    val DeepAmber       = Color(0xFFF59E0B) // Premium features, warnings, gold feel

    // ── 3. The "Modern Tech" Tones (Clean & Balanced) ──────────
    val SkyBlue         = Color(0xFF0EA5E9) // Very "SaaS" or "App" focused
    val IndigoPunch     = Color(0xFF4F46E5) // Slightly deeper than purple, very stable
    val MagentaPop      = Color(0xFFD946EF) // Creative, bold, high-fashion vibe
    val SlateHighlight  = Color(0xFF334155) // For "Subtle" focus (Dark grey-blue)
}