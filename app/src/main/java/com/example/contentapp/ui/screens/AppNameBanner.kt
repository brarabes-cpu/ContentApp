package com.example.contentapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contentapp.R
import com.example.contentapp.ui.theme.AccentBlue

/**
 * S1.T0 — плашка с названием приложения. Во всю ширину экрана, от самого верха
 * до нижней границы заголовка "Что хочу" на S1 (на S2 просто самый верхний блок).
 * Используется и на S1 (MainScreen), и на S2 (ResultScreen) — один и тот же компонент,
 * чтобы не расходиться в стиле при правках. Название временное (см. strings.xml,
 * app_banner_title) — придумать позже.
 */
@Composable
fun AppNameBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentBlue)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.app_banner_title),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
