package com.example.starodubovpavel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starodubovpavel.data.AppSettings
import com.example.starodubovpavel.data.CityEntity
import com.example.starodubovpavel.data.CityPlanEntity
import com.example.starodubovpavel.data.HistoryWithCity
import com.example.starodubovpavel.data.PlanStatus
import com.example.starodubovpavel.data.PlanWithCity
import com.example.starodubovpavel.ui.MainViewModel
import com.example.starodubovpavel.ui.SearchState
import com.example.starodubovpavel.ui.WeatherState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(application as WeatherApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WeatherCompanionApp(viewModel) }
    }
}

private enum class Section(val title: String, val icon: ImageVector) {
    SEARCH("Поиск", Icons.Default.Search),
    HISTORY("История", Icons.Default.History),
    PLANS("Планы", Icons.Default.ListAlt),
    SETTINGS("Настройки", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherCompanionApp(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    var sectionName by rememberSaveable { mutableStateOf(Section.SEARCH.name) }
    val section = Section.valueOf(sectionName)
    val colors = if (settings.darkTheme) androidx.compose.material3.darkColorScheme()
    else androidx.compose.material3.lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        if (selectedCity != null) {
            DetailScreen(
                city = selectedCity!!,
                viewModel = viewModel,
                settings = settings,
                onBack = viewModel::closeCity
            )
        } else {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Weather Companion") }) },
                bottomBar = {
                    NavigationBar {
                        Section.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == section,
                                onClick = { sectionName = item.name },
                                icon = { Icon(item.icon, null) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (section) {
                        Section.SEARCH -> SearchScreen(viewModel)
                        Section.HISTORY -> HistoryScreen(viewModel)
                        Section.PLANS -> PlansScreen(viewModel)
                        Section.SETTINGS -> SettingsScreen(viewModel, settings)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: MainViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.search.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::changeQuery,
            label = { Text("Название города") },
            placeholder = { Text("Например: Владивосток") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::search, enabled = query.trim().length >= 2, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, null)
            Text(" Найти")
        }
        Spacer(Modifier.height(12.dp))
        when (val current = state) {
            SearchState.Idle -> Hint("Найдите город. Результаты сохраняются и доступны без сети.")
            SearchState.Loading -> CenterLoading()
            is SearchState.Error -> Hint(current.message)
            is SearchState.Content -> {
                if (current.offline) {
                    Text("Офлайн-результаты из локального кэша", color = MaterialTheme.colorScheme.primary)
                }
                if (current.cities.isEmpty()) Hint("Ничего не найдено")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(current.cities, key = { it.id }) { city ->
                        CityCard(city, onClick = { viewModel.openCity(city) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CityCard(city: CityEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(city.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(listOf(city.region, city.country).filter { it.isNotBlank() }.joinToString(", "))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    city: CityEntity,
    viewModel: MainViewModel,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    var showPlanDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(city.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = viewModel::refreshWeather) { Icon(Icons.Default.Refresh, "Обновить") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(listOf(city.region, city.country).filter { it.isNotBlank() }.joinToString(", "))
            when (val current = weather) {
                WeatherState.Idle, WeatherState.Loading -> CenterLoading()
                is WeatherState.Error -> Hint(current.message)
                is WeatherState.Content -> {
                    Text(
                        TemperatureFormatter.format(current.weather.temperature, settings.useFahrenheit),
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text("Ветер: ${current.weather.windSpeed} км/ч")
                    Text("Код погоды: ${current.weather.weatherCode}")
                    Text("Наблюдение: ${current.weather.observedAt}")
                    if (current.weather.isFromCache) Text("Офлайн: показаны последние сохранённые данные")
                }
            }
            FilledTonalButton(onClick = { showPlanDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Text(" Добавить личный план")
            }
        }
    }
    if (showPlanDialog) {
        PlanDialog(
            heading = "Новый план для ${city.name}",
            onDismiss = { showPlanDialog = false },
            onSave = { title, note, date ->
                viewModel.addPlan(city, title, note, date)
                showPlanDialog = false
            }
        )
    }
}

@Composable
private fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Недавно просмотренные", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::clearHistory, enabled = history.isNotEmpty()) { Text("Очистить") }
        }
        if (history.isEmpty()) Hint("История появится после открытия города.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.history.id }) { item ->
                HistoryCard(
                    item = item,
                    onClick = { viewModel.openCity(item.city) },
                    onDelete = { viewModel.deleteHistory(item.history.id) }
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryWithCity, onClick: () -> Unit, onDelete: () -> Unit) {
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.history.openedAt))
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onClick).padding(16.dp)) {
                Text(item.city.name, fontWeight = FontWeight.Bold)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить из истории")
            }
        }
    }
}

@Composable
private fun PlansScreen(viewModel: MainViewModel) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingItem = plans.firstOrNull { it.plan.id == editing }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Мои погодные планы", style = MaterialTheme.typography.titleLarge)
        Text("Создавайте заметки к поездкам и управляйте их статусами.")
        Spacer(Modifier.height(12.dp))
        if (plans.isEmpty()) Hint("Откройте город и добавьте первый план.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plans, key = { it.plan.id }) { item ->
                PlanCard(
                    item = item,
                    onOpenCity = { viewModel.openCity(item.city) },
                    onEdit = { editing = item.plan.id },
                    onCycle = { viewModel.cyclePlanStatus(item.plan) },
                    onDelete = { viewModel.deletePlan(item.plan) }
                )
            }
        }
    }
    if (editingItem != null) {
        PlanDialog(
            heading = "Изменить план",
            initial = editingItem.plan,
            onDismiss = { editing = null },
            onSave = { title, note, date ->
                viewModel.updatePlan(editingItem.plan, title, note, date)
                editing = null
            }
        )
    }
}

@Composable
private fun PlanCard(
    item: PlanWithCity,
    onOpenCity: () -> Unit,
    onEdit: () -> Unit,
    onCycle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${item.city.name} • ${item.plan.targetDate}")
            if (item.plan.note.isNotBlank()) Text(item.plan.note)
            Text(
                when (item.plan.status) {
                    PlanStatus.DONE.name -> "Выполнено"
                    PlanStatus.CANCELLED.name -> "Отменено"
                    else -> "Запланировано"
                },
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onOpenCity) { Text("Погода") }
                TextButton(onClick = onEdit) { Text("Изменить") }
                IconButton(onClick = onCycle) { Icon(Icons.Default.CheckCircle, "Сменить статус") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить") }
            }
        }
    }
}

@Composable
private fun PlanDialog(
    heading: String,
    initial: CityPlanEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initial?.targetDate.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") })
                OutlinedTextField(note, { note = it }, label = { Text("Заметка") })
                OutlinedTextField(date, { date = it }, label = { Text("Дата или период") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, note, date) }, enabled = title.isNotBlank()) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SettingsScreen(viewModel: MainViewModel, settings: AppSettings) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.titleLarge)
        SettingSwitch("Тёмная тема", settings.darkTheme, viewModel::setDarkTheme)
        SettingSwitch("Температура в °F", settings.useFahrenheit, viewModel::setFahrenheit)
        Text("Срок актуальности офлайн-кэша: ${settings.cacheHours} ч")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6, 12, 24, 48).forEach { hours ->
                if (hours == settings.cacheHours) {
                    Button(onClick = { viewModel.setCacheHours(hours) }) { Text("$hours ч") }
                } else {
                    OutlinedButton(onClick = { viewModel.setCacheHours(hours) }) { Text("$hours ч") }
                }
            }
        }
        Hint("Настройки сохраняются через DataStore. Погода для городов из личных планов обновляется в фоне каждые 6 часов при наличии сети.")
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CenterLoading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
