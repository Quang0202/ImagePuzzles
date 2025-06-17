package com.imagepuzzles.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MultipleSelectChip() {
    var expanded by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(listOf<String>()) }
    val allItems = listOf("Chip", "Van Henry", "Oliver Hansen", "April Tucker", "Ralph Hubbard", "Omar Alexander", "Carlos Abbott")
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.Blue else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color.White)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    scope.launch {
                        focusRequester.requestFocus()
                    }
                    expanded = true
                }
                .onFocusChanged { isFocused = it.isFocused }
                .focusRequester(focusRequester)
                .focusable()
                .padding(8.dp)
        ) {
            Column {
                if (selectedItems.isEmpty()) {
                    Text("Chip", color = Color.Gray, fontSize = 16.sp)
                } else {
                    FlowRow {
                        selectedItems.forEach { item ->
                            ChipItem(item) { selectedItems = selectedItems - item }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        ) {
                allItems.forEach(){ item ->
                    DropdownMenuItem(
                        onClick = {
                            if (item in selectedItems) {
                                selectedItems = selectedItems - item
                            } else {
                                selectedItems = selectedItems + item
                            }
                            expanded = false
                        }
                    ) {
                        Text(
                            text = item,
                            fontSize = 16.sp,
                            color = if (item in selectedItems) Color.Blue else Color.Black
                        )
                    }
                }

        }
    }
}

@Composable
fun ChipItem(text: String, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 4.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.LightGray
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = text, fontSize = 14.sp)
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}