package xyz.d1n0.ui.boilerplate

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerFormat() = DatePickerDefaults.dateFormatter(
	selectedDateSkeleton = "yyMMd"
)