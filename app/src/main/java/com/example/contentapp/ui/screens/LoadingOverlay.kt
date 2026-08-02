package com.example.contentapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.contentapp.R
import com.example.contentapp.ui.theme.AccentBlue

/**
 * Оверлей ожидания — показывается поверх текущего экрана (в т.ч. поверх
 * открытого окна "Я правильно поняла?"), не заменяет его.
 */
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = AccentBlue)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.loading_title))
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.loading_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
