package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors
import org.jetbrains.compose.resources.painterResource
import uniwattelektrik.composeapp.generated.resources.Res
import uniwattelektrik.composeapp.generated.resources.app_logo3
/** Brand block: app_logo tile + dual-tone "UniWatt ElekTrik" wordmark. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(Res.drawable.app_logo3),
            contentDescription = "UniWatt ElekTrik logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(130.dp),
        )
        Text(
            "WORKFORCE  MANAGEMENT",
            color = AuthColors.TextLight,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
