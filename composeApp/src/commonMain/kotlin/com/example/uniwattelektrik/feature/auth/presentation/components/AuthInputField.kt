package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors

/**
 * Reusable rounded input used by every auth screen.
 *
 * - 56dp height, 16dp corner radius
 * - light grey container (`AuthColors.InputBg`)
 * - leading icon, optional trailing icon (e.g. password visibility toggle)
 */
@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = AuthColors.TextLight) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = AuthColors.TextLight)
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AuthColors.InputBg,
            unfocusedContainerColor = AuthColors.InputBg,
            disabledContainerColor = AuthColors.InputBg,
            focusedBorderColor = AuthColors.PrimaryBlue,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            cursorColor = AuthColors.PrimaryBlue,
            focusedTextColor = AuthColors.TextDark,
            unfocusedTextColor = AuthColors.TextDark,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    )
}

