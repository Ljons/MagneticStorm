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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.magneticstorm.app.widget.WidgetUpdateHelper
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.magneticstorm.app.ui.screen.HomeScreen
import com.magneticstorm.app.ui.screen.LocationPickerScreen
import com.magneticstorm.app.ui.screen.SettingsScreen
import com.magneticstorm.app.ui.theme.MagneticStormTheme

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
            LaunchedEffect(uiState.currentKp) {
                uiState.currentKp?.let { kp ->
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
            HomeScreen(
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
