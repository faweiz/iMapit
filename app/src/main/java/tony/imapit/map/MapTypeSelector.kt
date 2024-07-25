// ##START 070-experimental-api
@file:OptIn(ExperimentalMaterial3Api::class)
// ##END

package tony.imapit.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.maps.android.compose.MapType
import tony.imapit.R

@Composable
fun MapTypeSelector(
    currentValue: MapType,
    modifier: Modifier,
    onMapTypeClick: (MapType) -> Unit,
) {
    // ##START 070-expanded-state-local
    var expanded by remember {
        mutableStateOf(false)
    }
    // ##END

    ExposedDropdownMenuBox(
        // ##START 070-dropdown
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        // ##END
        modifier = modifier,
    ) {
        // ##START 070-text-field
        TextField(
            value = currentValue.name,
            label = {
                Text(text = stringResource(id = R.string.map_type))
            },
            readOnly = true,    // don't allow user to type
            onValueChange = {}, // unused
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        // ##END
        // ##START 070-menu
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            MapType.entries.forEach {
                DropdownMenuItem(
                    text = { Text(text = it.name) },
                    onClick = {
                        onMapTypeClick(it)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
        // ##END
    }
}
