package com.lumen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.ui.components.ExpressiveAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunitySheet(onDismiss: () -> Unit, onOpenLink: (String) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // عنوان منو
            Text(
                text = "پشتیبانی / Support", 
                style = MaterialTheme.typography.headlineLarge
            )

            // فقط دکمه پشتیبانی تلگرام با لینک اختصاصی شما
            ExpressiveAction(
                "پشتیبانی تلگرام | Support", 
                { onOpenLink("https://t.me/DNS_GAMINGX") }, 
                Modifier.fillMaxWidth(), 
                Icons.AutoMirrored.Filled.Send
            )
        }
    }
}
