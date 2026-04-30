package com.example.uniwattelektrik.core.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared card surface used across **every admin-app screen**.
 *
 * A subtle 40 %-grey-blend vertical gradient that gives every card on the
 * admin shell the same calm, premium "smoke" look. Apply via:
 *
 *   Modifier.background(brush = AdminCardBrush)
 *
 * The colours are intentionally cool-toned (slate family) so they read as a
 * unified surface against any of the admin backgrounds (home gradient,
 * Kanban grey, attendance map, etc.).
 *
 * The user app retains its existing white card style — these tokens are
 * only used inside `feature/admin/...`.
 */
val AdminCardTop:    Color = Color(0xFFF1F5F9)   // slate-100, soft white-grey
val AdminCardMid:    Color = Color(0xFFE7ECF3)   // 40 % grey blend
val AdminCardBottom: Color = Color(0xFFDCE3EC)   // slate-200/300 mix

val AdminCardBrush: Brush = Brush.verticalGradient(
    listOf(AdminCardTop, AdminCardMid, AdminCardBottom),
)

/** Border used to crisp the rounded corners on cards painted with [AdminCardBrush]. */
val AdminCardBorder: Color = Color(0x1A0F172A)   // 10 % slate-900 outline
