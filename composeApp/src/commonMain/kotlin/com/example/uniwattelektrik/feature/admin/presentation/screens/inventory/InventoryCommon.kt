package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme

data class Department(val id: String, val name: String)
data class Equipment(val id: String, val name: String, val departmentId: String)
data class SpareItem(
    val id: String,
    val category: String,
    val name: String,
    val make: String = "",
    val size: String = "",
    val core: String = "",
    val currentRating: String = "",
    val noOfPoles: String = "",
    val unit: String = "",
    val price: Double,
    val stockQty: Int,
    val hsn: String,
    val vendorName1: String = "",
    val vendorGst1: String = "",
    val vendorContact1: String = "",
    val vendorAddress1: String = "",
    val vendorName2: String = "",
    val vendorGst2: String = "",
    val vendorContact2: String = "",
    val vendorAddress2: String = "",
    val vendorLocation: String = "",
)

val sampleDepartments = listOf(
    Department("1", "Electrical"),
    Department("2", "Utility"),
    Department("3", "Production"),
    Department("4", "Packaging"),
    Department("5", "WSF"),
    Department("6", "Admin"),
    Department("7", "Farm"),
    Department("8", "SCADA"),
    Department("9", "IT"),
)

val sampleEquipment = listOf(
    // 1 Electrical
    Equipment("e1", "Metering Panel", "1"),
    Equipment("e2", "VCB Panel - 1", "1"),
    Equipment("e3", "VCB Panel CSS", "1"),
    Equipment("e4", "Transformer", "1"),
    Equipment("e5", "MV Panel", "1"),
    Equipment("e6", "SSB Panel", "1"),
    Equipment("e7", "Vertical PDB", "1"),
    Equipment("e8", "7 segment PDB", "1"),
    Equipment("e9", "MLSB", "1"),
    Equipment("e10", "LSSB", "1"),
    Equipment("e11", "LPDB", "1"),

    // 2 Utility
    Equipment("e12", "Air Dryer - 1", "2"),
    Equipment("e13", "Air Dryer - 2", "2"),
    Equipment("e14", "Screw Compressor", "2"),
    Equipment("e15", "Reciprocating Compressor", "2"),
    Equipment("e16", "RO Plant-1", "2"),
    Equipment("e17", "RO Plant-2", "2"),
    Equipment("e18", "UV Sterilizer", "2"),
    Equipment("e19", "ETP", "2"),
    Equipment("e20", "STP", "2"),
    Equipment("e21", "Boiler Gas", "2"),
    Equipment("e22", "Cooling Tower", "2"),

    // 3 Production
    Equipment("e23", "Process Reactor-1", "3"),
    Equipment("e24", "Process Reactor-2", "3"),
    Equipment("e25", "2 KL Reactor-1", "3"),
    Equipment("e26", "3 KL Reactor-2", "3"),
    Equipment("e27", "4 KL Reactor-3", "3"),
    Equipment("e28", "Centrifuge-1", "3"),
    Equipment("e29", "Centrifuge-2", "3"),
    Equipment("e30", "Ultra Sonicator", "3"),
    Equipment("e31", "Sonic Amalgamator", "3"),
    Equipment("e32", "Borewell Motor-1", "3"),
    Equipment("e33", "Borewell Motor-2", "3"),

    // 4 Packaging
    Equipment("e34", "Air Jet Vacuum Machine", "4"),
    Equipment("e35", "UV Light Conveyor-1", "4"),
    Equipment("e36", "UV Light Conveyor-2", "4"),
    Equipment("e37", "Bottle Filling Machine-1 8 Nozzle", "4"),
    Equipment("e38", "Bottle Filling Machine-2 4 Nozzle", "4"),
    Equipment("e39", "Bottle Filling Machine", "4"),
    Equipment("e40", "Inner Plug Machine-3 8 Nozzle Volumetric", "4"),
    Equipment("e41", "Capping Machine-1", "4"),
    Equipment("e42", "Capping Machine-2", "4"),
    Equipment("e43", "Capping Machine-3 Rotary Wheel", "4"),
    Equipment("e44", "Printing Machine-1", "4"),
    Equipment("e45", "Printing Machine-2", "4"),
    Equipment("e46", "Induction Sealing Machine Single Phase", "4"),
    Equipment("e47", "Induction Sealing Machine Three Phase", "4"),
    Equipment("e48", "Strapping Machine-1 Semi", "4"),
    Equipment("e49", "Strapping Machine-2 Automatic", "4"),
    Equipment("e50", "Strapping Machine-3 Automatic", "4"),
    Equipment("e51", "Tunnel Wrapping Machine", "4"),

    // 5 WSF
    Equipment("e52", "Crusher-1", "5"),
    Equipment("e53", "Crusher-2", "5"),
    Equipment("e54", "Crusher-3", "5"),
    Equipment("e55", "Blender", "5"),
    Equipment("e56", "Packing Machine-1 5-25kg", "5"),
    Equipment("e57", "Packing Machine-2 1kg", "5"),
    Equipment("e58", "Sealing Machine", "5"),
    Equipment("e59", "Stitching Machine-1", "5"),
    Equipment("e60", "Stitching Machine-2", "5"),
    Equipment("e61", "Dust Collector", "5"),
    Equipment("e62", "Hydraulic Lift", "5"),
    Equipment("e63", "Duct AC", "5"),

    // 6 Admin
    Equipment("e64", "Fan", "6"),
    Equipment("e65", "VRF AC-1", "6"),
    Equipment("e66", "VRF AC-2", "6"),
    Equipment("e67", "VRF AC-3", "6"),
    Equipment("e68", "VRF AC-4", "6"),
    Equipment("e69", "Duct AC-1", "6"),
    Equipment("e70", "Duct AC-2", "6"),
    Equipment("e71", "Tower AC", "6"),
    Equipment("e72", "Induction Stove", "6"),
    Equipment("e73", "Coffee Machine", "6"),

    // 7 Farm
    Equipment("e74", "Borewell Motor", "7"),
    Equipment("e75", "Solar Fencing", "7"),

    // 8 SCADA
    Equipment("e76", "Process Reactor Control Panel", "8"),
    Equipment("e77", "Monitoring Panel", "8"),
    Equipment("e78", "Modbus Controller", "8"),

    // 9 IT
    Equipment("e79", "PC-1", "9"),
    Equipment("e80", "PC-2", "9"),
    Equipment("e81", "PC-3", "9"),
    Equipment("e82", "PC-4", "9"),
    Equipment("e83", "PC-5", "9"),
    Equipment("e84", "PC-6", "9"),
    Equipment("e85", "Fire Wall", "9"),
    Equipment("e86", "Router-1", "9"),
    Equipment("e87", "Router-2", "9"),
    Equipment("e88", "Router-3", "9"),
    Equipment("e89", "Router-4", "9"),
    Equipment("e90", "Router-5", "9"),
    Equipment("e91", "CCTV", "9"),
    Equipment("e92", "Fingerprint ID Access", "9"),
    Equipment("e93", "Face ID Access", "9"),
)

val sampleSpares = listOf(
    // ── 1. CABLES ──────────────────────────────────────────────────────────
    SpareItem("s1", "Cable", "1.5 Sq.mm PVC Wire", make = "Havells", size = "1.5 mm", core = "1C", unit = "Coil", price = 1250.0, stockQty = 15, hsn = "8544"),
    SpareItem("s2", "Cable", "2.5 Sq.mm PVC Wire", make = "Havells", size = "2.5 mm", core = "1C", unit = "Coil", price = 1850.0, stockQty = 12, hsn = "8544"),
    SpareItem("s3", "Cable", "4 Sq.mm PVC Wire",   make = "Polycab", size = "4 mm",   core = "1C", unit = "Coil", price = 2850.0, stockQty = 8, hsn = "8544"),
    SpareItem("s4", "Cable", "6 Sq.mm PVC Wire",   make = "Polycab", size = "6 mm",   core = "1C", unit = "Coil", price = 4200.0, stockQty = 5, hsn = "8544"),
    SpareItem("s5", "Cable", "10 Sq.mm PVC Wire",  make = "Anchor",  size = "10 mm",  core = "1C", unit = "Coil", price = 6800.0, stockQty = 3, hsn = "8544"),
    SpareItem("s6", "Cable", "3 Core Flat Cable",  make = "Finolex", size = "2.5 mm", core = "3C", unit = "Mtr",  price = 85.0,   stockQty = 100, hsn = "8544"),

    // ── 2. SPARE COMPONENTS ────────────────────────────────────────────────
    SpareItem("s7", "Spare Component", "MCB 32A C-Curve", make = "Schneider", currentRating = "32A", noOfPoles = "1", price = 450.0, stockQty = 25, hsn = "8536"),
    SpareItem("s8", "Spare Component", "MCB 63A DP",      make = "Legrand",   currentRating = "63A", noOfPoles = "2", price = 1200.0, stockQty = 15, hsn = "8536"),
    SpareItem("s9", "Spare Component", "Contactor 22A",   make = "Siemens",   currentRating = "22A", noOfPoles = "3", price = 2800.0, stockQty = 10, hsn = "8536"),
    SpareItem("s10", "Spare Component", "Oil Filter",      make = "Atlas Copco", size = "Standard", price = 3500.0, stockQty = 8, hsn = "8421"),
    SpareItem("s11", "Spare Component", "Pressure Gauge",  make = "Wika",        size = "0-10 Bar", price = 850.0,  stockQty = 12, hsn = "9026"),
    SpareItem("s12", "Spare Component", "Mechanical Seal", make = "Burgmann",    size = "45mm",     price = 18000.0, stockQty = 2, hsn = "8484"),
    SpareItem("s13", "Spare Component", "Bearing 6205",    make = "SKF",         size = "25x52x15", price = 350.0,  stockQty = 30, hsn = "8482"),
)

@Composable
fun AddButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.Brand)
            .premiumPress(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.1f))
            .premiumPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Custom modifier for premium press effect: scale down on touch.
 */
@Composable
fun Modifier.premiumPress(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "pressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Entrance animation for list items.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    delayPerItem: Int = 40,
    content: @Composable () -> Unit
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * delayPerItem).toLong())
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animatable.value
            translationY = (1f - animatable.value) * 16.dp.toPx()
        }
    ) {
        content()
    }
}

/* StockStatus / formatRupees / MinimalSpareCard now live in SpareItemCards.kt */
