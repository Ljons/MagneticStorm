package com.magneticstorm.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.magneticstorm.app.R
import com.magneticstorm.app.data.model.KpRecord
import com.magneticstorm.app.data.model.KpScale
import com.magneticstorm.app.data.util.formatKp
import com.magneticstorm.app.data.model.SavedLocation
import com.magneticstorm.app.data.model.StormCategory
import com.magneticstorm.app.ui.theme.KpExtreme
import com.magneticstorm.app.ui.theme.KpMinor
import com.magneticstorm.app.ui.theme.KpModerate
import com.magneticstorm.app.ui.theme.KpQuiet
import com.magneticstorm.app.ui.theme.KpSevere
import com.magneticstorm.app.ui.theme.KpStrong
import com.magneticstorm.app.ui.viewmodel.MainUiState
import com.magneticstorm.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit = { padding ->
        HomeScreenContent(
            viewModel = viewModel,
            onOpenLocation = onOpenLocation,
            modifier = Modifier.padding(padding)
        )
    }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onOpenLocation) {
                        Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.location))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        content(padding)
    }
}

/** Контент головного екрану (список Kp). Використовується всередині свайпу з графіком. */
@Composable
fun HomeScreenContent(
    viewModel: MainViewModel,
    onOpenLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.forecast.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            state.error != null && state.forecast.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        state.error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = { viewModel.loadKpData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry))
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.currentKp?.let { kp ->
                        item {
                            val todayRecords = state.forecastByDay[state.todayForLocation] ?: emptyList()
                            CurrentKpCard(
                                kp = kp,
                                timeZoneId = state.location.timeZoneId,
                                hourlyRecords = todayRecords,
                                location = state.location,
                                onLocationClick = onOpenLocation
                            )
                        }
                    }
                    if (state.forecastByDay.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.forecast_days),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            state.forecastByDay.entries.toList().let { entries ->
                                val today = state.todayForLocation ?: ""
                                val (future, todayAndPast) = entries.partition { it.key > today }
                                future.sortedBy { it.key } + todayAndPast.sortedByDescending { it.key }
                            },
                            key = { it.key }
                        ) { entry ->
                            DayForecastCard(
                                dayKey = entry.key,
                                records = entry.value,
                                timeZoneId = state.location.timeZoneId,
                                isForecast = state.todayForLocation?.let { today -> entry.key > today } ?: false
                            )
                        }
                    }
                }
                if (state.isLoading && state.forecast.isNotEmpty()) {
                    CircularProgressIndicator(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationBlock(location: SavedLocation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(R.string.your_location),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                location.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.size(6.dp))
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CurrentKpCard(
    kp: KpRecord,
    timeZoneId: String,
    hourlyRecords: List<KpRecord>,
    location: SavedLocation,
    onLocationClick: () -> Unit
) {
    val category = KpScale.category(kp.kp)
    val color = kpColorForCategory(category)
    val displaySlots = (0 until 8).map { index -> hourlyRecords.getOrNull(index) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        stringResource(R.string.today_level),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        kp.formatTime(timeZoneId, "dd-MM-yyyy"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
                LocationBlock(
                    location = location,
                    onClick = onLocationClick
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            formatKp(kp.kp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(
                            "Kp ${formatKp(kp.kp)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        kp.noaaScale?.let { scale ->
                            Text(
                                KpScale.label(scale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    kp.formatTime(timeZoneId, "HH:mm"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (displaySlots.any { it != null }) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displaySlots.forEach { record ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (record != null) kpColorForCategory(KpScale.category(record.kp)).copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                        ) {
                            Text(
                                text = record?.kp?.let { formatKp(it) } ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (record != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displaySlots.forEach { record ->
                        Text(
                            text = record?.formatTime(timeZoneId, "HH:mm") ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayForecastCard(
    dayKey: String,
    records: List<KpRecord>,
    timeZoneId: String,
    isForecast: Boolean = false
) {
    val maxKp = records.maxOfOrNull { it.kp } ?: 0.0
    val category = KpScale.category(maxKp)
    val color = kpColorForCategory(category)
    // Завжди 8 слотів: якщо для дня менше даних (наприклад останній день прогнозу NOAA) — порожні слоты
    val displaySlots = (0 until 8).map { index -> records.getOrNull(index) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isForecast) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isForecast) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDateDdMmYyyy(dayKey),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isForecast) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.forecast_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            formatKp(maxKp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Kp max",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displaySlots.forEach { record ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (record != null) kpColorForCategory(KpScale.category(record.kp)).copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                    ) {
                        Text(
                            text = record?.kp?.let { formatKp(it) } ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (record != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displaySlots.forEach { record ->
                    Text(
                        text = record?.formatTime(timeZoneId, "HH:mm") ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Конвертує ключ дня yyyy-MM-dd у формат відображення dd-MM-yyyy */
private fun formatDateDdMmYyyy(ymd: String): String {
    val parts = ymd.split("-")
    return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else ymd
}

@Composable
private fun kpColorForCategory(category: StormCategory) = when (category) {
    StormCategory.QUIET -> KpQuiet
    StormCategory.MINOR -> KpMinor
    StormCategory.MODERATE -> KpModerate
    StormCategory.STRONG -> KpStrong
    StormCategory.SEVERE -> KpSevere
    StormCategory.EXTREME -> KpExtreme
}
