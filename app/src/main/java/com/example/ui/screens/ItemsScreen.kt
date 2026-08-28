package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.ItemEntity
import com.example.ui.AppViewModel
import com.example.utils.NepaliDateConverter
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    navController: NavController,
    viewModel: AppViewModel
) {
    val items by viewModel.items.collectAsState()
    var isAddingOrEditing by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    
    // UI state for Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAddingOrEditing) (if (itemToEdit != null) "Edit Item" else "Add Item") else "Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAddingOrEditing) {
                            isAddingOrEditing = false
                            itemToEdit = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isAddingOrEditing) {
                        IconButton(onClick = { navController.navigate("barcode_report") }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode Report")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAddingOrEditing) {
                FloatingActionButton(onClick = {
                    itemToEdit = null
                    isAddingOrEditing = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimatedContent(
            targetState = isAddingOrEditing,
            label = "ItemScreenAnim",
            modifier = Modifier.padding(padding)
        ) { editing ->
            if (editing) {
                ItemForm(
                    initialItem = itemToEdit,
                    viewModel = viewModel,
                    onSaved = { msg ->
                        isAddingOrEditing = false
                        itemToEdit = null
                    },
                    onCancel = {
                        isAddingOrEditing = false
                        itemToEdit = null
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        ItemCard(
                            item = item,
                            onEdit = {
                                itemToEdit = item
                                isAddingOrEditing = true
                            },
                            onDelete = {
                                viewModel.deleteItem(item.id, onComplete = {}, onError = {})
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemForm(
    initialItem: ItemEntity?,
    viewModel: AppViewModel,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    var name by remember { mutableStateOf(initialItem?.itemName ?: "") }
    var hsCode by remember { mutableStateOf(initialItem?.hsCode ?: "") }
    var barcode by remember { mutableStateOf(initialItem?.barcode ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "") }
    var brand by remember { mutableStateOf(initialItem?.brand ?: "") }
    var purchasePrice by remember { mutableStateOf(if (initialItem != null) initialItem.purchasePrice.toString() else "") }
    var salesPrice by remember { mutableStateOf(if (initialItem != null) initialItem.salesPrice.toString() else "") }
    var wholesalePrice by remember { mutableStateOf(if (initialItem != null) initialItem.wholesalePrice.toString() else "") }
    var taxRate by remember { mutableStateOf(if (initialItem != null) initialItem.taxRate.toString() else "") }
    var stock by remember { mutableStateOf(if (initialItem != null) initialItem.stockQuantity.toString() else "") }
    var uom by remember { mutableStateOf(initialItem?.uom ?: "Pcs") }
    var batchNumber by remember { mutableStateOf(initialItem?.batchNumber ?: "") }
    var status by remember { mutableStateOf(initialItem?.status ?: "ACTIVE") }
    
    var formError by remember { mutableStateOf<String?>(null) }
    
    // Expiry Date
    var expiryDateMillis by remember { mutableStateOf(initialItem?.expiryDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Image
    var imageUri by remember { mutableStateOf<Uri?>(initialItem?.imageUri?.let { Uri.parse(it) }) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
            imageBitmap = null
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            imageUri = null
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expiryDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    expiryDateMillis = datePickerState.selectedDateMillis 
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Image Picker Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Need Coil for Uri or just show a placeholder, but we can't add coil dependency easily if it's not there.
            // For now, if we have a bitmap, show it.
            if (imageBitmap != null) {
                Image(bitmap = imageBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else if (imageUri != null) {
                // Without coil, displaying Uri is hard in raw compose, so we just show an icon indicating image selected
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Image Selected", modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
            } else {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { cameraLauncher.launch(null) }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                }
                SmallFloatingActionButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                }
            }
        }

        OutlinedTextField(
            value = name, 
            onValueChange = { 
                name = it
                formError = null 
            }, 
            label = { Text("Product Name *") }, 
            modifier = Modifier.fillMaxWidth(),
            isError = formError != null && name.isBlank()
        )
        
        OutlinedTextField(
            value = hsCode,
            onValueChange = { hsCode = it },
            label = { Text("HS Code") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = barcode, 
                onValueChange = { barcode = it }, 
                label = { Text("Barcode") }, 
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { barcode = UUID.randomUUID().toString().substring(0, 8).uppercase() }) {
                Text("Auto")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Purchase Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = wholesalePrice, onValueChange = { wholesalePrice = it }, label = { Text("Wholesale Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = salesPrice, onValueChange = { salesPrice = it }, label = { Text("Retail Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = taxRate, onValueChange = { taxRate = it }, label = { Text("Tax Rate (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Initial Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = uom, onValueChange = { uom = it }, label = { Text("UOM (e.g. Pcs, Kg)") }, modifier = Modifier.weight(1f))
        }
        
        OutlinedTextField(value = batchNumber, onValueChange = { batchNumber = it }, label = { Text("Batch Number") }, modifier = Modifier.fillMaxWidth())

        // Expiry Date (AD and BS)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Expiry Date", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (expiryDateMillis != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = expiryDateMillis!! }
                    val adDateStr = "${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH)+1).toString().padStart(2, '0')}-${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
                    val bsDateStr = NepaliDateConverter.getBsDate(expiryDateMillis!!)
                    
                    Text("AD: $adDateStr", fontWeight = FontWeight.Bold)
                    Text("BS: $bsDateStr (Auto-Generated)", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Tap to select Expiry Date")
                }
            }
        }

        // Status
        var statusExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("Product Status") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                DropdownMenuItem(text = { Text("ACTIVE") }, onClick = { status = "ACTIVE"; statusExpanded = false })
                DropdownMenuItem(text = { Text("INACTIVE") }, onClick = { status = "INACTIVE"; statusExpanded = false })
            }
        }
        
        if (formError != null) {
            Text(
                text = formError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (name.isBlank()) {
                        formError = "Product Name is required."
                        return@Button
                    }
                    val finalUom = if (uom.trim().isBlank()) "Pcs" else uom.trim()
                    
                    if (initialItem != null) {
                        viewModel.updateItem(
                            item = initialItem.copy(
                                itemName = name.trim(),
                                hsCode = hsCode.trim(),
                                barcode = barcode.trim(),
                                category = category.trim(),
                                brand = brand.trim(),
                                uom = finalUom,
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
                                salesPrice = salesPrice.toDoubleOrNull() ?: 0.0,
                                taxRate = taxRate.toDoubleOrNull() ?: 0.0,
                                stockQuantity = stock.toDoubleOrNull() ?: 0.0,
                                batchNumber = batchNumber.trim(),
                                status = status,
                                expiryDate = expiryDateMillis,
                                imageUri = imageUri?.toString()
                            ),
                            onComplete = { onSaved("Item updated successfully.") },
                            onError = { }
                        )
                    } else {
                        viewModel.addItem(
                            name = name.trim(),
                            hsCode = hsCode.trim(),
                            uom = finalUom,
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                            salesPrice = salesPrice.toDoubleOrNull() ?: 0.0,
                            initialStock = stock.toDoubleOrNull() ?: 0.0,
                            barcode = barcode.trim(),
                            category = category.trim(),
                            brand = brand.trim(),
                            wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
                            taxRate = taxRate.toDoubleOrNull() ?: 0.0,
                            batchNumber = batchNumber.trim(),
                            status = status,
                            expiryDate = expiryDateMillis,
                            imageUri = imageUri?.toString(),
                            onComplete = { onSaved("Item added successfully.") },
                            onError = { }
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ItemCard(item: ItemEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val stockColor = if (item.stockQuantity <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusColor = if (item.status == "ACTIVE") Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category: ${if(item.category.isNotBlank()) item.category else "N/A"} | Brand: ${if(item.brand.isNotBlank()) item.brand else "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.barcode.isNotBlank()) {
                        Text(
                            text = "Barcode: ${item.barcode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.1f),
                    ) {
                        Text(
                            text = item.status,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = stockColor.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "${item.stockQuantity} ${item.uom}",
                            color = stockColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Wholesale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("रू ${formatter.format(item.wholesalePrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("Retail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("रू ${formatter.format(item.salesPrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
