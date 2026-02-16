package com.magneticstorm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magneticstorm.app.data.model.KpRecord
import com.magneticstorm.app.data.model.SavedLocation
import com.magneticstorm.app.data.preferences.PreferencesManager
import com.magneticstorm.app.data.repository.KpRepository
import com.magneticstorm.app.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class MainUiState(
    val location: SavedLocation = SavedLocation.DEFAULT,
    val forecast: List<KpRecord> = emptyList(),
    val forecastByDay: Map<String, List<KpRecord>> = emptyMap(),
    /** Дні поточного місяця та середній Kp за день для графіка (день to середнє). */
    val monthChartData: List<Pair<Int, Double>> = emptyList(),
    val currentKp: KpRecord? = null,
    val todayForLocation: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val themeMode: String = "system",
    val refreshMode: String = "on_open",
    val notificationEnabled: Boolean = false,
    val notificationKpThreshold: Int = 5
)

sealed class LocationSearchState {
    data object Idle : LocationSearchState()
    data object Loading : LocationSearchState()
    data class Results(val list: List<SavedLocation>) : LocationSearchState()
    data class Error(val message: String) : LocationSearchState()
}

class MainViewModel(
    private val kpRepository: KpRepository,
    private val locationRepository: LocationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _locationSearchState = MutableStateFlow<LocationSearchState>(LocationSearchState.Idle)
    val locationSearchState: StateFlow<LocationSearchState> = _locationSearchState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                location = preferencesManager.getSavedLocationSync(),
                themeMode = preferencesManager.getThemeModeSync(),
                refreshMode = preferencesManager.getRefreshModeSync(),
                notificationEnabled = preferencesManager.getNotificationEnabledSync(),
                notificationKpThreshold = preferencesManager.getNotificationKpThresholdSync()
            )
        }
        viewModelScope.launch {
            preferencesManager.savedLocation.collect { loc ->
                _uiState.update { state ->
                    val tz = loc.timeZoneId
                    val todayStr = kpRepository.getTodayString(tz)
                    state.copy(
                        location = loc,
                        todayForLocation = todayStr,
                        forecastByDay = if (state.forecast.isEmpty()) emptyMap()
                        else kpRepository.groupByDay(state.forecast, tz, todayStr, daysBack = 4, daysForward = 3),
                        monthChartData = if (state.forecast.isEmpty()) emptyList()
                        else kpRepository.getCurrentMonthDailyAverages(state.forecast, tz)
                    )
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesManager.refreshMode.collect { mode ->
                _uiState.update { it.copy(refreshMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesManager.notificationEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.notificationKpThreshold.collect { threshold ->
                _uiState.update { it.copy(notificationKpThreshold = threshold) }
            }
        }
        loadKpData()
    }

    fun loadKpData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val forecastResult = kpRepository.getForecast()
            val currentResult = kpRepository.getCurrent()
            forecastResult.fold(
                onSuccess = { list ->
                    val location = _uiState.value.location
                    val todayStr = kpRepository.getTodayString(location.timeZoneId)
                    val forecastTimeTags = list.map { it.timeTag.take(19) }.toSet()
                    val currentList = currentResult.getOrNull().orEmpty()
                    val currentOnly = currentList.filter { it.timeTag.take(19) !in forecastTimeTags }
                    val listForMonth = list + currentOnly
                    val byDay = kpRepository.groupByDay(list, location.timeZoneId, todayStr, daysBack = 4, daysForward = 3)
                    val current = kpRepository.currentKpForNow(list)
                    val monthData = kpRepository.getCurrentMonthDailyAverages(listForMonth, location.timeZoneId)
                    _uiState.update {
                        it.copy(
                            forecast = list,
                            forecastByDay = byDay,
                            monthChartData = monthData,
                            currentKp = current,
                            todayForLocation = todayStr,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Помилка завантаження"
                        )
                    }
                }
            )
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _locationSearchState.value = LocationSearchState.Idle
            return
        }
        viewModelScope.launch {
            _locationSearchState.value = LocationSearchState.Loading
            locationRepository.searchLocation(query).fold(
                onSuccess = { list ->
                    _locationSearchState.value = LocationSearchState.Results(list)
                },
                onFailure = { e ->
                    _locationSearchState.value = LocationSearchState.Error(e.message ?: "Помилка пошуку")
                }
            )
        }
    }

    fun clearLocationSearch() {
        _locationSearchState.value = LocationSearchState.Idle
    }

    fun selectLocation(location: SavedLocation) {
        viewModelScope.launch {
            preferencesManager.setSavedLocation(location)
            val forecast = _uiState.value.forecast
            if (forecast.isNotEmpty()) {
                val todayStr = kpRepository.getTodayString(location.timeZoneId)
                _uiState.update {
                    it.copy(
                        location = location,
                        todayForLocation = todayStr,
                        forecastByDay = kpRepository.groupByDay(forecast, location.timeZoneId, todayStr, daysBack = 4, daysForward = 3),
                        monthChartData = kpRepository.getCurrentMonthDailyAverages(forecast, location.timeZoneId)
                    )
                }
            }
            _locationSearchState.value = LocationSearchState.Idle
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setRefreshMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setRefreshMode(mode)
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationEnabled(enabled)
        }
    }

    fun setNotificationKpThreshold(threshold: Int) {
        viewModelScope.launch {
            preferencesManager.setNotificationKpThreshold(threshold.coerceIn(1, 9))
        }
    }
}
