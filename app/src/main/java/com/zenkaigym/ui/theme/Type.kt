package com.zenkaigym.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ZenTypography = Typography(
    displayMedium = TextStyle(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 34.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
)
