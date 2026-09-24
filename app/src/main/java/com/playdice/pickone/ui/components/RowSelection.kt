package com.playdice.pickone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> RowSelection(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    NeuCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                SelectionPill(
                    text = label(option),
                    selected = isSelected,
                    onClick = { onSelectionChange(option) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 11.dp),
                    depth = if (isSelected) NeuDepth.SHALLOW else NeuDepth.FLAT,
                    selectedSurface = false,
                )
            }
        }
    }
}
