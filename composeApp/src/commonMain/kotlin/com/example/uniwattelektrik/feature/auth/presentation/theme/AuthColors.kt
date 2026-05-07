package com.example.uniwattelektrik.feature.auth.presentation.theme

import com.example.uniwattelektrik.core.theme.AppTheme

/** Brand palette used by the Auth feature — delegates to enterprise design tokens. */
internal object AuthColors {
    val PrimaryBlue get() = AppTheme.Brand
    val DarkBlue    get() = AppTheme.Navy
    val BgTop       get() = AppTheme.Bg
    val BgBottom    get() = AppTheme.Surface
    val TextDark    get() = AppTheme.Ink900
    val TextLight   get() = AppTheme.Ink500
    val InputBg     get() = AppTheme.SurfaceMuted
    val Divider     get() = AppTheme.Ink100
}

