package com.carnation.fallalert.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.ui.screen.AlertListScreen
import com.carnation.fallalert.ui.screen.ConfirmationDoneScreen
import com.carnation.fallalert.ui.screen.EventDetailScreen

private object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{windowId}"
    const val DONE = "done/{windowId}/{confirmation}"

    // window_id 는 "trial-01:7" 처럼 ':' 를 포함하므로 반드시 인코딩한다.
    fun detail(windowId: String) = "detail/${Uri.encode(windowId)}"
    fun done(windowId: String, confirmation: Confirmation) =
        "done/${Uri.encode(windowId)}/${confirmation.name}"
}

private const val ARG_WINDOW_ID = "windowId"
private const val ARG_CONFIRMATION = "confirmation"

@Composable
fun FallAlertNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // AndroidViewModel(Application) 은 기본 팩토리가 그대로 만들어 준다.
    val viewModel: FallAlertViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
        modifier = modifier,
    ) {
        composable(Routes.LIST) {
            val state by viewModel.listState.collectAsStateWithLifecycle()
            AlertListScreen(
                state = state,
                onEventClick = { windowId -> navController.navigate(Routes.detail(windowId)) },
                onSimulatePush = viewModel::simulateIncomingPush,
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(ARG_WINDOW_ID) { type = NavType.StringType }),
        ) { entry ->
            val windowId = entry.arguments?.getString(ARG_WINDOW_ID).orEmpty()
            val detailFlow = remember(windowId) { viewModel.detailState(windowId) }
            val state by detailFlow.collectAsStateWithLifecycle()

            EventDetailScreen(
                state = state,
                onConfirm = { confirmation ->
                    viewModel.confirm(windowId, confirmation)
                    navController.navigate(Routes.done(windowId, confirmation))
                },
                onBackToList = { navController.popBackStack(Routes.LIST, inclusive = false) },
            )
        }

        composable(
            route = Routes.DONE,
            arguments = listOf(
                navArgument(ARG_WINDOW_ID) { type = NavType.StringType },
                navArgument(ARG_CONFIRMATION) { type = NavType.StringType },
            ),
        ) { entry ->
            val confirmation = entry.arguments
                ?.getString(ARG_CONFIRMATION)
                ?.let { runCatching { Confirmation.valueOf(it) }.getOrNull() }
                ?: Confirmation.NORMAL

            ConfirmationDoneScreen(
                confirmation = confirmation,
                onBackToList = {
                    navController.popBackStack(Routes.LIST, inclusive = false)
                },
            )
        }
    }
}
