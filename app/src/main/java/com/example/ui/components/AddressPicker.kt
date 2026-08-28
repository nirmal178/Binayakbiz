package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.District
import com.example.data.NepalAddressData
import com.example.data.Palika
import com.example.data.Province

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPicker(
    selectedAddress: String,
    onAddressChange: (String) -> Unit
) {
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedDistrict by remember { mutableStateOf<District?>(null) }
    var selectedPalika by remember { mutableStateOf<Palika?>(null) }
    var wardNo by remember { mutableStateOf("") }
    var toleOrStreet by remember { mutableStateOf("") }

    // Dropdown states
    var provinceExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var palikaExpanded by remember { mutableStateOf(false) }

    // Parse incoming selectedAddress if it exists on initial load
    LaunchedEffect(selectedAddress) {
        if (selectedProvince == null && selectedDistrict == null && selectedAddress.isNotBlank()) {
            val parts = selectedAddress.split(",").map { it.trim() }
            // Try to match province from parts
            val foundProv = NepalAddressData.provinces.firstOrNull { prov ->
                parts.any { it.equals(prov.name, ignoreCase = true) }
            }
            if (foundProv != null) {
                selectedProvince = foundProv
                val foundDist = NepalAddressData.districts.filter { it.provinceId == foundProv.id }.firstOrNull { dist ->
                    parts.any { it.equals(dist.name, ignoreCase = true) }
                }
                if (foundDist != null) {
                    selectedDistrict = foundDist
                    val foundPalika = NepalAddressData.palikas.filter { it.districtId == foundDist.id }.firstOrNull { pal ->
                        parts.any { it.equals(pal.name, ignoreCase = true) }
                    }
                    if (foundPalika != null) {
                        selectedPalika = foundPalika
                    }
                }
            }
            // Check for Ward
            val wardPart = parts.firstOrNull { it.startsWith("Ward", ignoreCase = true) }
            if (wardPart != null) {
                val digits = wardPart.filter { it.isDigit() }
                if (digits.isNotEmpty()) {
                    wardNo = digits
                }
            }
        }
    }

    val districts = remember(selectedProvince) {
        if (selectedProvince != null) {
            NepalAddressData.districts.filter { it.provinceId == selectedProvince!!.id }
        } else {
            emptyList()
        }
    }

    val palikas = remember(selectedDistrict) {
        if (selectedDistrict != null) {
            NepalAddressData.palikas.filter { it.districtId == selectedDistrict!!.id }
        } else {
            emptyList()
        }
    }

    // Update parent when selections change
    fun updateFinalAddress(
        prov: Province?,
        dist: District?,
        pal: Palika?,
        ward: String,
        tole: String
    ) {
        if (prov != null && dist != null && pal != null) {
            val parts = mutableListOf<String>()
            if (tole.isNotBlank()) {
                parts.add(tole.trim())
            }
            if (ward.isNotBlank()) {
                parts.add("Ward ${ward.trim()}")
            }
            parts.add(pal.name)
            parts.add(dist.name)
            parts.add(prov.name)
            
            val formatted = parts.joinToString(", ")
            onAddressChange(formatted)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Province Dropdown
        ExposedDropdownMenuBox(
            expanded = provinceExpanded,
            onExpandedChange = { provinceExpanded = !provinceExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedProvince?.name ?: "",
                placeholder = { Text("Select Province *") },
                onValueChange = {},
                readOnly = true,
                label = { Text("Province *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = provinceExpanded,
                onDismissRequest = { provinceExpanded = false }
            ) {
                NepalAddressData.provinces.forEach { prov ->
                    DropdownMenuItem(
                        text = { Text(prov.name) },
                        onClick = {
                            selectedProvince = prov
                            selectedDistrict = null
                            selectedPalika = null
                            provinceExpanded = false
                            updateFinalAddress(prov, null, null, wardNo, toleOrStreet)
                        }
                    )
                }
            }
        }

        // District Dropdown
        ExposedDropdownMenuBox(
            expanded = districtExpanded,
            onExpandedChange = { 
                if (selectedProvince != null) districtExpanded = !districtExpanded 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDistrict?.name ?: "",
                placeholder = { Text("Select District *") },
                label = { Text("District *") },
                onValueChange = {},
                readOnly = true,
                enabled = selectedProvince != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = districtExpanded,
                onDismissRequest = { districtExpanded = false }
            ) {
                districts.forEach { dist ->
                    DropdownMenuItem(
                        text = { Text(dist.name) },
                        onClick = {
                            selectedDistrict = dist
                            selectedPalika = null
                            districtExpanded = false
                            updateFinalAddress(selectedProvince, dist, null, wardNo, toleOrStreet)
                        }
                    )
                }
            }
        }

        // Palika (Municipality / Rural Municipality)
        ExposedDropdownMenuBox(
            expanded = palikaExpanded,
            onExpandedChange = { 
                if (selectedDistrict != null) palikaExpanded = !palikaExpanded 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedPalika?.name ?: "",
                placeholder = { Text("Select Municipality / RM *") },
                label = { Text("Palika (Municipality/RM) *") },
                onValueChange = {},
                readOnly = true,
                enabled = selectedDistrict != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = palikaExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = palikaExpanded,
                onDismissRequest = { palikaExpanded = false }
            ) {
                palikas.forEach { pal ->
                    DropdownMenuItem(
                        text = { Text(pal.name) },
                        onClick = {
                            selectedPalika = pal
                            palikaExpanded = false
                            updateFinalAddress(selectedProvince, selectedDistrict, pal, wardNo, toleOrStreet)
                        }
                    )
                }
            }
        }

        // Ward No. & Tole/Street Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = wardNo,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(3)
                    wardNo = filtered
                    updateFinalAddress(selectedProvince, selectedDistrict, selectedPalika, filtered, toleOrStreet)
                },
                label = { Text("Ward No. *") },
                placeholder = { Text("e.g. 4") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = toleOrStreet,
                onValueChange = { input ->
                    toleOrStreet = input
                    updateFinalAddress(selectedProvince, selectedDistrict, selectedPalika, wardNo, input)
                },
                label = { Text("Tole / Street") },
                placeholder = { Text("e.g. New Road") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
        }

        if (selectedAddress.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Full Address: $selectedAddress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
