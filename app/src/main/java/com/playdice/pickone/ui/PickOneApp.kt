package com.playdice.pickone.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playdice.pickone.ui.screens.AboutScreen
import com.playdice.pickone.ui.screens.EditorScreen
import com.playdice.pickone.ui.screens.HomeScreen
import com.playdice.pickone.ui.screens.PlayScreen
import com.playdice.pickone.ui.screens.SettingsScreen
import com.playdice.pickone.ui.theme.PickOneTheme
import com.playdice.pickone.ui.viewmodel.AppViewModel
import com.playdice.pickone.ui.components.NeuDialog
import com.playdice.pickone.R

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{sceneId}"
    const val PLAY = "play/{sceneId}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun PickOneApp(viewModel: AppViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val update by viewModel.availableUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadedUpdate by viewModel.downloadedUpdate.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(downloadedUpdate) {
        downloadedUpdate?.let { apk ->
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            viewModel.clearDownloadedUpdate()
        }
    }
    PickOneTheme(settings) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onAdd = { navController.navigate("editor/new") },
                        onEdit = { navController.navigate("editor/$it") },
                        onPlay = { navController.navigate("play/$it") },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onAbout = { navController.navigate(Routes.ABOUT) },
                    )
                }
                composable(
                    route = Routes.EDITOR,
                    arguments = listOf(navArgument("sceneId") { type = NavType.StringType }),
                ) {
                    EditorScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.PLAY,
                    arguments = listOf(navArgument("sceneId") { type = NavType.StringType }),
                ) {
                    PlayScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
            update?.let { available ->
                val details = buildString {
                    if (available.description.isNotBlank()) append(available.description)
                    if (available.date.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(context.getString(R.string.update_date, available.date))
                    }
                    if (downloadError) {
                        if (isNotEmpty()) append("\n")
                        append(context.getString(R.string.update_failed))
                    } else if (downloadProgress != null) {
                        if (isNotEmpty()) append("\n")
                        append(context.getString(R.string.update_downloading, downloadProgress ?: 0))
                    }
                }
                NeuDialog(
                    title = context.getString(R.string.update_available, available.version),
                    text = details,
                    confirmText = context.getString(if (downloadProgress == null) R.string.update_now else R.string.update_downloading_button),
                    dismissText = context.getString(R.string.update_later),
                    onConfirm = viewModel::downloadUpdate,
                    onDismiss = viewModel::dismissUpdate,
                    confirmEnabled = downloadProgress == null,
                )
            }
        }
    }
}
