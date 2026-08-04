package com.sentinelle.app.ui.viewmodel.labels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.NumberLabelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MyLabelsUiState(
    val labels: List<NumberLabelEntity> = emptyList(),
    val selectedCategory: String? = null,
)

class MyLabelsViewModel(
    private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyLabelsUiState())
    val uiState: StateFlow<MyLabelsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun setCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun deleteLabel(phoneNumber: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).numberLabelDao().deleteByPhoneNumber(phoneNumber)
            }
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val labels =
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(context).numberLabelDao().getAll()
                }
            _uiState.value = _uiState.value.copy(labels = labels)
        }
    }
}
