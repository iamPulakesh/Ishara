package com.isharaai.isl.core.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.isharaai.isl.feature.camera.CameraScreen
import com.isharaai.isl.feature.chat.ChatScreen
import com.isharaai.isl.feature.chat.ChatViewModel
import com.isharaai.isl.feature.download.DownloadScreen
import com.isharaai.isl.feature.history.HistoryScreen
import com.isharaai.isl.feature.onboarding.OnboardingScreen
import com.isharaai.isl.feature.onboarding.TutorialOverlay
import com.isharaai.isl.feature.onboarding.isOnboardingCompleted
import com.isharaai.isl.feature.onboarding.isTutorialPending
import com.isharaai.isl.feature.onboarding.setTutorialPending
import com.isharaai.isl.feature.settings.SettingsScreen
import com.isharaai.isl.feature.usersigns.UserSignsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Chat     : Screen("chat")
    object Download : Screen("download")
    object Settings : Screen("settings")
    object History  : Screen("history")
    object Camera   : Screen("camera")
    object UserSigns : Screen("user_signs")
}

// Keys for passing data between screens
const val IMAGE_RESULT_KEY = "captured_image_path"
const val SESSION_RESULT_KEY = "load_session_id"

@Composable
fun IsharaAINavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showTutorial by remember { mutableStateOf(isTutorialPending(context)) }

    val startDest = if (isOnboardingCompleted(context)) Screen.Chat.route else Screen.Onboarding.route

    NavHost(navController = navController, startDestination = startDest) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = { wantsTutorial ->
                    showTutorial = wantsTutorial
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

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

            // Tutorial overlay on top of ChatScreen
            if (showTutorial) {
                TutorialOverlay(onFinish = { setTutorialPending(context, false); showTutorial = false })
            }
        }

        composable(Screen.Download.route) {
            DownloadScreen(
                onDownloadComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onMySignsClick = { navController.navigate(Screen.UserSigns.route) },
                onReplayTutorial = {
                    setTutorialPending(context, true)
                    showTutorial = true
                    navController.popBackStack()
                }
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
                }
            )
        }

        composable(Screen.UserSigns.route) {
            UserSignsScreen(onBack = { navController.popBackStack() })
        }
    }
}

