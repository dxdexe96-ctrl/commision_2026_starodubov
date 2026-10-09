package com.example.starodubovpavel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starodubovpavel.PlanRules
import com.example.starodubovpavel.WeatherApp
import com.example.starodubovpavel.data.AppSettings
import com.example.starodubovpavel.data.CityEntity
import com.example.starodubovpavel.data.CityPlanEntity
import com.example.starodubovpavel.data.LoadResult
import com.example.starodubovpavel.data.SettingsRepository
import com.example.starodubovpavel.data.Weather
import com.example.starodubovpavel.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Content(val cities: List<CityEntity>, val offline: Boolean) : SearchState
    data class Error(val message: String) : SearchState
}

sealed interface WeatherState {
    data object Idle : WeatherState
    data object Loading : WeatherState
    data class Content(val weather: Weather) : WeatherState
    data class Error(val message: String) : WeatherState
}

class MainViewModel(
    private val repository: WeatherRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search = _search.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityEntity?>(null)
    val selectedCity = _selectedCity.asStateFlow()

    private val _weather = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weather = _weather.asStateFlow()

    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plans = repository.plans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun changeQuery(value: String) {
        _query.value = value
    }

    fun search() {
        viewModelScope.launch {
            _search.value = SearchState.Loading
            _search.value = when (val result = repository.search(_query.value)) {
                is LoadResult.Success -> SearchState.Content(result.data, result.fromCache)
                is LoadResult.Error -> SearchState.Error(result.message)
            }
        }
    }

    fun openCity(city: CityEntity) {
        _selectedCity.value = city
        viewModelScope.launch {
            repository.openCity(city)
            loadWeather(city)
        }
    }

    fun closeCity() {
        _selectedCity.value = null
        _weather.value = WeatherState.Idle
    }

    fun refreshWeather() {
        _selectedCity.value?.let { city ->
            viewModelScope.launch { loadWeather(city, force = true) }
        }
    }

    private suspend fun loadWeather(city: CityEntity, force: Boolean = false) {
        _weather.value = WeatherState.Loading
        _weather.value = when (val result = repository.weather(city, settings.value.cacheHours, force)) {
            is LoadResult.Success -> WeatherState.Content(result.data)
            is LoadResult.Error -> WeatherState.Error(result.message)
        }
    }

    fun addPlan(city: CityEntity, title: String, note: String, date: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addPlan(city, title, note, date) }
    }

    fun cyclePlanStatus(plan: CityPlanEntity) {
        viewModelScope.launch { repository.updatePlan(plan.copy(status = PlanRules.nextStatus(plan.status))) }
    }

    fun updatePlan(plan: CityPlanEntity, title: String, note: String, date: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updatePlan(plan.copy(title = title.trim(), note = note.trim(), targetDate = date.trim()))
        }
    }

    fun deletePlan(plan: CityPlanEntity) {
        viewModelScope.launch { repository.deletePlan(plan) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch { repository.deleteHistory(historyId) }
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(value) }
    }

    fun setFahrenheit(value: Boolean) {
        viewModelScope.launch { settingsRepository.setFahrenheit(value) }
    }

    fun setCacheHours(value: Int) {
        viewModelScope.launch { settingsRepository.setCacheHours(value) }
    }

    companion object {
        fun factory(app: WeatherApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(app.repository, app.settingsRepository) as T
            }
        }
    }
}
