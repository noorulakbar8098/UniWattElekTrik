package com.example.uniwattelektrik.feature.user.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.OperationsHeaderSurface
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue

/**
 * Read-only Personal Information screen for the employee.
 *
 * Shows the employee's full profile — photo, name, role, and every field
 * stored in [EmployeeRecord] — in a non-editable, scrollable layout.
 * Opened when the employee taps "Personal Information" in the Settings section.
 */
@Composable
fun EmployeePersonalInfoScreen(
    user: User,
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
) {
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val rec: EmployeeRecord? = remember(employees, user.id) {
        employees.firstOrNull { it.id == user.id }
    }

    val name = rec?.name?.takeIf { it.isNotBlank() }
        ?: user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email.substringBefore("@")

    val role = rec?.role?.takeIf { it.isNotBlank() } ?: "Employee"

    val gradient = remember(user.id) { avatarGradientPInfo(user.id) }
    val initials  = name.split(" ", "_", "-").filter { it.isNotBlank() }
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifEmpty { name.take(2).uppercase() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Bg),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        // ── Gradient header ────────────────────────────────────────────────
        item {
            OperationsHeaderSurface(bottomPadding = 32.dp) {
                    // Top bar
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = Color.White,
                                modifier           = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Personal Information",
                            color      = Color.White,
                            fontSize   = 24.sp,                    // canonical header title size
                            fontWeight = FontWeight.ExtraBold,
                            modifier   = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Photo + name/role
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val photoUrl = rec?.photoUrl ?: ""
                        if (photoUrl.isNotBlank()) {
                            coil3.compose.AsyncImage(
                                model              = photoUrl,
                                contentDescription = name,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .size(88.dp)
                                    .shadow(16.dp, RoundedCornerShape(24.dp),
                                            spotColor = Color(0x55000000))
                                    .clip(RoundedCornerShape(24.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .shadow(16.dp, RoundedCornerShape(24.dp),
                                            spotColor = Color(0x55000000))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Brush.linearGradient(gradient)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initials,
                                    color      = Color.White,
                                    fontSize   = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name,
                                color      = Color.White,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                listOfNotNull(
                                    role.takeIf { it.isNotBlank() },
                                    rec?.department?.takeIf { it.isNotBlank() },
                                ).joinToString(" · ").ifBlank { "Employee" },
                                color      = Color.White.copy(alpha = 0.78f),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                            )
                            if (rec != null) {
                                Spacer(Modifier.height(8.dp))
                                val statusColor = when (rec.status) {
                                    "Active"  -> Color(0xFF22C55E)
                                    "OnLeave" -> Color(0xFFF59E0B)
                                    else      -> Color.White.copy(alpha = 0.60f)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(statusColor.copy(alpha = 0.18f))
                                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        rec.status.uppercase().replace("ONLEAVE", "ON LEAVE")
                                            .ifBlank { "ACTIVE" },
                                        color         = statusColor,
                                        fontSize      = 10.sp,
                                        fontWeight    = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // ── Personal Information ───────────────────────────────────────────
        item {
            PInfoSection("Personal Information") {
                PInfoRow(Icons.Outlined.Person, "Full Name",
                    rec?.name?.takeIf { it.isNotBlank() } ?: name, AppTheme.Brand50, AppTheme.Brand)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Email, "Email Address",
                    rec?.email?.takeIf { it.isNotBlank() } ?: user.email, AppTheme.Brand50, AppTheme.Brand)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Phone, "Phone Number",
                    rec?.phone?.takeIf { it.isNotBlank() } ?: "—", AppTheme.SuccessBg, AppTheme.Success)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Badge, "Gender",
                    rec?.gender?.takeIf { it.isNotBlank() } ?: "—", Color(0xFFF0F0FF), AppTheme.Brand)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Cake, "Date of Birth",
                    rec?.dateOfBirthMs?.let { pInfoFormatDate(it) } ?: "—", AppTheme.WarningBg, AppTheme.Warning)
                PInfoDivider()
                PInfoRow(Icons.Outlined.LocationOn, "Address",
                    rec?.address?.takeIf { it.isNotBlank() } ?: "—", AppTheme.WarningBg, AppTheme.Warning)
            }
        }

        // ── Work Details ───────────────────────────────────────────────────
        item {
            PInfoSection("Work Details") {
                PInfoRow(Icons.Outlined.Badge, "Role / Title",
                    rec?.role?.takeIf { it.isNotBlank() } ?: role, AppTheme.Brand100, AppTheme.Brand700)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Work, "Employment Type",
                    rec?.employmentType?.takeIf { it.isNotBlank() } ?: "—", AppTheme.Brand50, AppTheme.Brand)
                PInfoDivider()
                PInfoRow(Icons.Outlined.Group, "Department",
                    rec?.department?.takeIf { it.isNotBlank() } ?: "—", AppTheme.SuccessBg, AppTheme.Success)
                PInfoDivider()
                PInfoRow(Icons.Outlined.LocationOn, "Zone",
                    rec?.zone?.takeIf { it.isNotBlank() } ?: "—", AppTheme.WarningBg, AppTheme.Warning)
                PInfoDivider()
                PInfoRow(Icons.Outlined.SupervisorAccount, "Reporting To",
                    rec?.reportingTo?.takeIf { it.isNotBlank() } ?: "—", Color(0xFFF0F0FF), AppTheme.Brand)
                PInfoDivider()
                PInfoRow(
                    Icons.Outlined.Schedule, "Shift",
                    when (rec?.shift) {
                        "Shift1" -> "Shift 1  (09:00 – 18:00)"
                        "Shift2" -> "Shift 2  (13:00 – 23:00)"
                        else     -> rec?.shift?.takeIf { it.isNotBlank() } ?: "—"
                    },
                    AppTheme.Brand50, AppTheme.Brand,
                )
                PInfoDivider()
                PInfoRow(Icons.Outlined.CalendarMonth, "Joining Date",
                    rec?.joiningDateMs?.let { pInfoFormatDate(it) } ?: "—", AppTheme.SuccessBg, AppTheme.Success)
                PInfoDivider()
                // Status badge row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(when (rec?.status) {
                                "Active"  -> AppTheme.SuccessBg
                                "OnLeave" -> AppTheme.WarningBg
                                else      -> AppTheme.Ink50
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null,
                            tint = when (rec?.status) {
                                "Active"  -> AppTheme.Success
                                "OnLeave" -> AppTheme.Warning
                                else      -> AppTheme.Ink500
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Status", color = AppTheme.Ink500, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    val st = rec?.status ?: "—"
                    val sc = when (st) {
                        "Active"   -> AppTheme.Success
                        "OnLeave"  -> AppTheme.Warning
                        "Inactive" -> AppTheme.Danger
                        else       -> AppTheme.Ink500
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sc.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(st, color = sc, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Emergency Contact ──────────────────────────────────────────────
        if (rec != null && (rec.emergencyName.isNotBlank() || rec.emergencyPhone.isNotBlank())) {
            item {
                PInfoSection("Emergency Contact") {
                    if (rec.emergencyName.isNotBlank()) {
                        PInfoRow(Icons.Outlined.ContactPhone, "Contact Name",
                            buildString {
                                append(rec.emergencyName)
                                if (rec.emergencyRelation.isNotBlank()) append("  ·  ${rec.emergencyRelation}")
                            },
                            AppTheme.DangerBg, AppTheme.Danger,
                        )
                    }
                    if (rec.emergencyPhone.isNotBlank()) {
                        if (rec.emergencyName.isNotBlank()) PInfoDivider()
                        PInfoRow(Icons.Outlined.Phone, "Contact Phone",
                            rec.emergencyPhone, AppTheme.DangerBg, AppTheme.Danger)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PInfoSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
        Text(
            title.uppercase(),
            color         = AppTheme.Ink500,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier      = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = AppTheme.ShadowSm)
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.Surface),
            content = content,
        )
    }
}

@Composable
private fun PInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconBg: Color,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, color = AppTheme.Ink500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color      = AppTheme.Ink900,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PInfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(AppTheme.Ink100),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun pInfoFormatDate(ms: Long): String = try {
    val local  = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec")
    "${local.dayOfMonth.toString().padStart(2,'0')} ${months[local.monthNumber-1]} ${local.year}"
} catch (_: Exception) { "—" }

private fun avatarGradientPInfo(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFFB28A), Color(0xFFEC8552)),
        listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
        listOf(Color(0xFFA78BFA), Color(0xFF6D28D9)),
        listOf(Color(0xFF34D399), Color(0xFF047857)),
        listOf(Color(0xFFF472B6), Color(0xFFBE185D)),
    )
    return palettes[(seed.hashCode().absoluteValue) % palettes.size]
}
