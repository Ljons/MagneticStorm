package com.magneticstorm.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.magneticstorm.app.widget.WidgetUpdateHelper
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.magneticstorm.app.ui.screen.ChartScreen
import com.magneticstorm.app.ui.screen.HomeScreen
import com.magneticstorm.app.ui.screen.HomeScreenContent
import com.magneticstorm.app.ui.screen.LocationPickerScreen
import com.magneticstorm.app.ui.screen.SettingsScreen
import com.magneticstorm.app.ui.theme.MagneticStormTheme
import com.magneticstorm.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MagneticStormApp
        setContent {
            val uiState by app.mainViewModel.uiState.collectAsState()
            val themeMode = uiState.themeMode
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(uiState.currentKp, uiState.todayForLocation, uiState.forecastByDay, uiState.location) {
                val kp = uiState.currentKp
                val todayRecords = uiState.todayForLocation?.let { uiState.forecastByDay[it] }.orEmpty()
                if (kp != null && todayRecords.isNotEmpty()) {
                    val state = WidgetUpdateHelper.buildCardState(kp, todayRecords, uiState.location.timeZoneId, uiState.location.displayName)
                    WidgetUpdateHelper.saveAndUpdateWidget(app.applicationContext, state)
                } else if (kp != null) {
                    WidgetUpdateHelper.saveAndUpdateWidget(app.applicationContext, kp.kp)
                }
            }
            MagneticStormTheme(darkTheme = darkTheme) {
                AppNavHost(
                    viewModel = app.mainViewModel,
                    app = app,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenWithPager(
    viewModel: MainViewModel,
    onOpenLocation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    HomeScreen(
        viewModel = viewModel,
        onOpenLocation = onOpenLocation,
        onOpenSettings = onOpenSettings,
        content = { contentPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(contentPadding)
            ) { page ->
                when (page) {
                    0 -> HomeScreenContent(viewModel = viewModel, onOpenLocation = onOpenLocation)
                    1 -> ChartScreen(viewModel = viewModel)
                }
            }
        }
    )
}

@Composable
private fun AppNavHost(
    viewModel: com.magneticstorm.app.ui.viewmodel.MainViewModel,
    app: MagneticStormApp,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreenWithPager(
                viewModel = viewModel,
                onOpenLocation = { navController.navigate("location") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("location") {
            LocationPickerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onRefreshModeChanged = { app.scheduleKpSyncIfNeeded() },
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}
