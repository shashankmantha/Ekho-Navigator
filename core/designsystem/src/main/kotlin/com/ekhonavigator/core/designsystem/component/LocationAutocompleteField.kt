package com.ekhonavigator.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.icon.EkhoIcons

data class LocationSuggestion(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val subtitle: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (LocationSuggestion) -> Unit,
    onCustomLocation: (String) -> Unit,
    suggestions: List<LocationSuggestion>,
    modifier: Modifier = Modifier,
    label: String = "Location",
    placeholder: String = "Room, building, or marker name",
    maxResults: Int = 6,
) {
    var expanded by remember { mutableStateOf(false) }

    val filtered = remember(value, suggestions) {
        val query = value.trim()
        if (query.isEmpty()) {
            // Show user's custom markers first when the field is empty —
            // their own places are higher-signal than the campus list.
            suggestions.filter { it.isCustom }.take(maxResults)
        } else {
            suggestions
                .asSequence()
                .filter { it.matches(query) }
                .sortedBy { if (it.startsWith(query)) 0 else 1 }
                .take(maxResults)
                .toList()
        }
    }

    val showCustomFallback = value.isNotBlank() && filtered.none { it.name.equals(value, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { typed ->
                onValueChange(typed)
                if (!expanded) expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = EkhoIcons.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )

        if (filtered.isNotEmpty() || showCustomFallback) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                filtered.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { SuggestionRow(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                        },
                    )
                }
                if (showCustomFallback) {
                    DropdownMenuItem(
                        text = { CustomFallbackRow(value) },
                        onClick = {
                            onCustomLocation(value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: LocationSuggestion) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = if (suggestion.isCustom) EkhoIcons.BookmarkBorder else EkhoIcons.Place,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (suggestion.isCustom) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (suggestion.subtitle.isNotBlank()) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CustomFallbackRow(typed: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = EkhoIcons.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Use \u201C$typed\u201D as custom location",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LocationSuggestion.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) || subtitle.contains(query, ignoreCase = true)

private fun LocationSuggestion.startsWith(query: String): Boolean =
    name.startsWith(query, ignoreCase = true)
