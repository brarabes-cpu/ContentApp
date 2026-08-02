package com.example.contentapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Крупные, легко читаемые размеры шрифта — под требование доступности
// для пользователей разного возраста с минимальными навыками пользования приложениями
val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
)
