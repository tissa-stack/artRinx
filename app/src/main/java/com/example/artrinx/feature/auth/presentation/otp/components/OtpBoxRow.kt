package com.example.artrinx.feature.auth.presentation.otp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing

@Composable
fun OtpBoxRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 6,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            // fillMaxWidth so boxes share all available space, regardless of screen size
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusRequester.requestFocus() },
                ),
        ) {
            repeat(otpLength) { index ->
                val char = otp.getOrNull(index)
                val isFocused = index == otp.length

                Box(
                    modifier = Modifier
                        .weight(1f)          // each box gets an equal share of the width
                        .aspectRatio(1f)     // height = width → always square
                        .clip(RoundedCornerShape(Spacing.md))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (isFocused) Modifier.border(
                                2.dp, BrandPrimary, RoundedCornerShape(Spacing.md),
                            ) else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = char?.toString() ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        BasicTextField(
            value = otp,
            onValueChange = { new ->
                onOtpChange(new.filter { it.isDigit() }.take(otpLength))
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .matchParentSize()   // overlay the boxes exactly; do NOT expand the row's height
                .alpha(0.005f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
        )
    }
}
