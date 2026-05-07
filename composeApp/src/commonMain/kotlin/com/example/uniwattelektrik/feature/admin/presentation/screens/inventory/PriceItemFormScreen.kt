package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.SetStatusBar
// ─── Design tokens — backed by enterprise system ────────────────────────────
private val ScreenBg     = AppTheme.Bg
private val CardBg       = AppTheme.Surface
private val InkPrimary   = AppTheme.Ink900
private val InkSecondary = AppTheme.Ink500
private val InkMuted     = AppTheme.Ink300
private val Brand        = AppTheme.Brand
private val BrandDeep    = AppTheme.Brand700
private val Brand50      = AppTheme.Brand50
private val Success      = AppTheme.Success
private val SuccessBg    = AppTheme.SuccessBg
private val Warning      = AppTheme.Warning
private val WarningBg    = AppTheme.WarningBg
private val Danger       = AppTheme.Danger
private val DangerBg     = AppTheme.DangerBg
private val DividerSoft  = AppTheme.Ink100
private val ShadowSoft   = AppTheme.ShadowMd
private val InputBg      = AppTheme.SurfaceMuted
private val DotBorder    = AppTheme.Ink300
private val Divider      = AppTheme.Ink100


/* ── Local design tokens ─────────────────────────────────────────────── */

@Composable
fun PriceItemFormScreen(
    initial: SpareItem?,
    onBack: () -> Unit,
    onSave: (SpareItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("PriceItemFormScreen")
    SetStatusBar(color = Brand, darkIcons = false)

    val departments = remember { sampleDepartments }
    val allEquipment = remember { sampleEquipment }

    var name      by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var make      by remember(initial) { mutableStateOf(initial?.make ?: "") }
    var size      by remember(initial) { mutableStateOf(initial?.size ?: "") }
    var core      by remember(initial) { mutableStateOf(initial?.core ?: "") }
    var rating    by remember(initial) { mutableStateOf(initial?.currentRating ?: "") }
    var poles     by remember(initial) { mutableStateOf(initial?.noOfPoles ?: "") }
    var price     by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock     by remember(initial) { mutableStateOf(initial?.stockQty?.toString() ?: "") }
    var hsn       by remember(initial) { mutableStateOf(initial?.hsn ?: "") }

    var vName1    by remember(initial) { mutableStateOf(initial?.vendorName1 ?: "") }
    var vGst1     by remember(initial) { mutableStateOf(initial?.vendorGst1 ?: "") }
    var vContact1 by remember(initial) { mutableStateOf(initial?.vendorContact1 ?: "") }
    var vAddr1    by remember(initial) { mutableStateOf(initial?.vendorAddress1 ?: "") }
    var vLoc      by remember(initial) { mutableStateOf(initial?.vendorLocation ?: "") }

    var nameError by remember { mutableStateOf("") }

    var selDept  by remember(initial) { mutableStateOf(departments.first()) }
    var selEquip by remember(initial) {
        mutableStateOf(allEquipment.firstOrNull { it.departmentId == selDept.id } ?: allEquipment.first())
    }

    val equipForDept = allEquipment.filter { it.departmentId == selDept.id }

    /* ── Accordion expansion state ────────────────────────────────── */
    val expanded = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>(0 to true, 1 to true) }

    val canSubmit = name.isNotBlank() && price.isNotBlank() && stock.isNotBlank()

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GradientHeader(
                    title = if (initial == null) "New Price Item" else "Edit Price Item",
                    onBack = onBack
                )
                Spacer(Modifier.height(12.dp))
            }

            /* 1. Basic Information */
            item {
                AccordionSection(
                    index = 0, title = "Basic Information",
                    icon = Icons.Filled.Description,
                    tint = Brand, bg = Brand50,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Item Name", required = true)
                    DottedField(
                        value = name, onChange = { name = it; nameError = "" },
                        leadingIcon = Icons.Filled.Bolt,
                        leadingTint = Warning, leadingBg = WarningBg,
                        placeholder = "e.g. 2.5 Sq.mm PVC Wire",
                    )
                    if (nameError.isNotEmpty()) {
                        Text(nameError, color = Danger, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Make (Brand)")
                    DottedField(
                        value = make, onChange = { make = it },
                        leadingIcon = Icons.Filled.LocalOffer,
                        leadingTint = Brand, leadingBg = Brand50,
                        placeholder = "e.g. Havells / Polycab",
                    )
                }
            }

            /* 2. Specifications */
            item {
                AccordionSection(
                    index = 1, title = "Specifications",
                    icon = Icons.Filled.Settings,
                    tint = Warning, bg = WarningBg,
                    expandedMap = expanded,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Size")
                            DottedField(
                                value = size, onChange = { size = it },
                                placeholder = "e.g. 2.5 mm",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Core")
                            DottedField(
                                value = core, onChange = { core = it },
                                placeholder = "e.g. 1C / 3C",
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Current Rating")
                            DottedField(
                                value = rating, onChange = { rating = it },
                                placeholder = "e.g. 10A / 20A",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("No. of Poles")
                            DottedField(
                                value = poles, onChange = { poles = it },
                                placeholder = "e.g. SP / DP / TP",
                            )
                        }
                    }
                }
            }

            /* 3. Pricing & Stock */
            item {
                AccordionSection(
                    index = 2, title = "Pricing & Stock",
                    icon = Icons.Filled.LocalOffer,
                    tint = Success, bg = SuccessBg,
                    expandedMap = expanded,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Unit Price", required = true)
                            DottedField(
                                value = price, onChange = { price = it },
                                leadingIcon = null,
                                trailing = "₹",
                                keyboard = KeyboardType.Decimal,
                                placeholder = "0.00",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Stock Qty", required = true)
                            DottedField(
                                value = stock, onChange = { stock = it },
                                keyboard = KeyboardType.Number,
                                placeholder = "0",
                            )
                        }
                    }
                }
            }

            /* 4. Categorization */
            item {
                AccordionSection(
                    index = 3, title = "Categorization",
                    icon = Icons.Filled.Category,
                    tint = Brand, bg = Brand50,
                    expandedMap = expanded,
                ) {
                    FieldLabel("HSN Code")
                    DottedField(
                        value = hsn, onChange = { hsn = it },
                        placeholder = "e.g. 8544",
                    )

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Department")
                    DottedSelect(
                        value = selDept.name,
                        onChange = { dName ->
                            selDept = departments.find { it.name == dName } ?: selDept
                            selEquip = allEquipment.filter { it.departmentId == selDept.id }.firstOrNull() ?: allEquipment.first()
                        },
                        options = departments.map { it.name },
                        leadingIcon = Icons.Filled.Category
                    )

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Equipment")
                    DottedSelect(
                        value = selEquip.name,
                        onChange = { eName ->
                            selEquip = allEquipment.find { it.name == eName } ?: selEquip
                        },
                        options = equipForDept.map { it.name },
                        leadingIcon = Icons.Filled.Settings
                    )
                }
            }

            /* 5. Vendor Information */
            item {
                AccordionSection(
                    index = 4, title = "Vendor Information",
                    icon = Icons.Filled.Storefront,
                    tint = Warning, bg = WarningBg,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Vendor Location")
                    DottedField(
                        value = vLoc, onChange = { vLoc = it },
                        placeholder = "City / Region",
                    )

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Primary Vendor Name")
                    DottedField(
                        value = vName1, onChange = { vName1 = it },
                        leadingIcon = Icons.Filled.Storefront,
                        leadingTint = Warning, leadingBg = WarningBg,
                        placeholder = "e.g. ABC Electricals",
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("GST Number")
                            DottedField(
                                value = vGst1, onChange = { vGst1 = it },
                                placeholder = "27AAAAA0000A1Z5",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Contact No.")
                            DottedField(
                                value = vContact1, onChange = { vContact1 = it },
                                keyboard = KeyboardType.Phone,
                                placeholder = "+91…",
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Vendor Address")
                    DottedField(
                        value = vAddr1, onChange = { vAddr1 = it },
                        minHeight = 80.dp,
                        placeholder = "Full address details…",
                    )
                }
            }
        }

        /* ── Sticky bottom action bar ───────────────────────────────── */
        BottomActions(
            canSubmit = canSubmit,
            onSaveDraft = { /* TODO */ },
            onPreview   = { /* TODO */ },
            onCreate    = {
                if (canSubmit) {
                    onSave(SpareItem(
                        id             = initial?.id ?: "",
                        category       = initial?.category ?: "Spare Component",
                        name           = name.trim(),
                        make           = make.trim(),
                        size           = size.trim(),
                        core           = core.trim(),
                        currentRating  = rating.trim(),
                        noOfPoles      = poles.trim(),
                        price          = price.toDoubleOrNull() ?: 0.0,
                        stockQty       = stock.toIntOrNull() ?: 0,
                        hsn            = hsn.trim(),
                        vendorName1    = vName1.trim(),
                        vendorGst1     = vGst1.trim(),
                        vendorContact1 = vContact1.trim(),
                        vendorAddress1 = vAddr1.trim(),
                        vendorLocation = vLoc.trim(),
                    ))
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun GradientHeader(title: String, onBack: () -> Unit) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )

                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White,
                         fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("INVENTORY MANAGEMENT",
                         color = Color(0xCCFFFFFF), fontSize = 11.sp,
                         fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  ACCORDION
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun AccordionSection(
    index: Int,
    title: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    expandedMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Boolean>,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val open = expandedMap[index] ?: false
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "chev",
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedMap[index] = !open }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("SECTION ${index + 1}",
                     color = InkMuted, fontSize = 10.sp,
                     fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
                Text(title, color = InkPrimary, fontSize = 15.sp,
                     fontWeight = FontWeight.Bold)
            }

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(22.dp).rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit  = fadeOut(tween(120)) + shrinkVertically(tween(180)),
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Box(modifier = Modifier
                    .fillMaxWidth().height(1.dp).background(Divider))
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  FORM PRIMITIVES
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(text, color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (required) Text(" *", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
}

private fun Modifier.dottedBorder(
    color: Color = DotBorder,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.5.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
        ),
    )
}

@Composable
private fun DottedField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    leadingTint: Color = Brand,
    leadingBg: Color = Brand50,
    trailing: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp).clip(RoundedCornerShape(10.dp)).background(leadingBg),
                contentAlignment = Alignment.Center,
            ) { Icon(leadingIcon, null, tint = leadingTint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = InkMuted, fontSize = 13.sp)
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp,
                                      fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(trailing, color = InkMuted, fontSize = 12.sp,
                 fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DottedSelect(
    value: String,
    onChange: (String) -> Unit,
    options: List<String>,
    leadingIcon: ImageVector,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .dottedBorder()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brand50),
                contentAlignment = Alignment.Center,
            ) { Icon(leadingIcon, null, tint = Brand, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(value, color = InkPrimary, fontSize = 13.sp,
                 fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null, tint = InkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        if (open) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, Divider, RoundedCornerShape(12.dp)),
            ) {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange(opt); open = false }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(opt, color = InkPrimary, fontSize = 13.sp,
                             fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (value == opt) {
                            Icon(Icons.Filled.Check, null,
                                 tint = Brand, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  BOTTOM ACTION BAR
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun BottomActions(
    canSubmit: Boolean,
    onSaveDraft: () -> Unit,
    onPreview: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg)
            .shadow(20.dp, spotColor = ShadowSoft)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedAction(label = "Save Draft", onClick = onSaveDraft,
                       modifier = Modifier.weight(1f))
        OutlinedAction(label = "Preview",    onClick = onPreview,
                       modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .weight(1.4f).height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (canSubmit)
                        Brush.horizontalGradient(listOf(Brand, BrandDeep))
                    else SolidColor(Color(0xFFB6CDEF)),
                )
                .clickable(enabled = canSubmit, onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Text("Save →", color = Color.White, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutlinedAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = InkSecondary, fontSize = 13.sp,
             fontWeight = FontWeight.SemiBold)
    }
}
