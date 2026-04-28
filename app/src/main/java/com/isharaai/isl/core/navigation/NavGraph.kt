package com.isharaai.isl.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.isharaai.isl.feature.camera.CameraScreen
import com.isharaai.isl.feature.chat.ChatScreen
import com.isharaai.isl.feature.chat.ChatViewModel
import com.isharaai.isl.feature.download.SplashDownloadScreen
import com.isharaai.isl.feature.history.HistoryScreen
import com.isharaai.isl.feature.settings.SettingsScreen
import com.isharaai.isl.feature.video.ISLVideoScreen

sealed class Screen(val route: String) {
    object Chat     : Screen("chat")
    object Download : Screen("download")
    object Settings : Screen("settings")
    object History  : Screen("history")
    object Camera   : Screen("camera")
    object Video    : Screen("isl_video/{signId}") {
        fun withSignId(id: String) = "isl_video/$id"
    }
}

// Keys for passing data between screens
const val IMAGE_RESULT_KEY = "captured_image_path"
const val SESSION_RESULT_KEY = "load_session_id"

@Composable
fun IsharaAINavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Chat.route) {

        composable(Screen.Chat.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val chatViewModel: ChatViewModel = hiltViewModel()

            // One-shot image result from Camera
            LaunchedEffect(Unit) {
                savedStateHandle.getStateFlow<String?>(IMAGE_RESULT_KEY, null)
                    .collect { imagePath ->
                        imagePath?.let {
                            chatViewModel.sendImage(it)
                            savedStateHandle.remove<String>(IMAGE_RESULT_KEY)
                        }
                    }
            }

            // One-shot session load from History
            LaunchedEffect(Unit) {
                savedStateHandle.getStateFlow<String?>(SESSION_RESULT_KEY, null)
                    .collect { sessionId ->
                        sessionId?.let {
                            chatViewModel.loadSession(it)
                            savedStateHandle.remove<String>(SESSION_RESULT_KEY)
                        }
                    }
            }
        
            ChatScreen(
                onCameraClick   = { navController.navigate(Screen.Camera.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onDownloadClick = { navController.navigate(Screen.Download.route) },
                viewModel       = chatViewModel
            )
        }

        composable(Screen.Download.route) {
            SplashDownloadScreen(
                onDownloadComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onHistoryClick = { navController.navigate(Screen.History.route) }
            )
        }


        composable(Screen.History.route) {
            // Get the active session ID from ChatViewModel so History can protect it
            val chatEntry = navController.getBackStackEntry(Screen.Chat.route)
            val chatViewModel: ChatViewModel = hiltViewModel(chatEntry)
            val currentSessionId = chatViewModel.uiState.collectAsState().value.sessionId

            HistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    // Set the session ID on Chat's savedStateHandle and navigate back
                    navController.getBackStackEntry(Screen.Chat.route)
                        .savedStateHandle
                        .set(SESSION_RESULT_KEY, sessionId)
                    navController.popBackStack(Screen.Chat.route, false)
                },
                currentSessionId = currentSessionId
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoCaptured = { imagePath ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(IMAGE_RESULT_KEY, imagePath)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Video.route,
            arguments = listOf(navArgument("signId") { type = NavType.StringType })
        ) { backStackEntry ->
            val signId = backStackEntry.arguments?.getString("signId") ?: "HELP"
            ISLVideoScreen(
                signId    = signId,
                onBack    = { navController.popBackStack(Screen.Chat.route, false) },
                onPlayAgain = {
                    navController.navigate(Screen.Video.withSignId(signId)) {
                        popUpTo(Screen.Video.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
