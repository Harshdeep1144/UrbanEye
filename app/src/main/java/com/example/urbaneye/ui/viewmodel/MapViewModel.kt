package com.example.urbaneye.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.RoadRating
import com.example.urbaneye.domain.repository.PotholeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val potholes: List<Pothole> = emptyList(),
    val roadRatings: List<RoadRating> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: PotholeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getPotholes().collect { potholes ->
                _uiState.value = _uiState.value.copy(potholes = potholes)
            }
            
            repository.getRoadRatings().collect { ratings ->
                _uiState.value = _uiState.value.copy(roadRatings = ratings)
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}