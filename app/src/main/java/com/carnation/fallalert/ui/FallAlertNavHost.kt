package com.carnation.fallalert.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.ui.screen.AlertListScreen
import com.carnation.fallalert.ui.screen.AllAlertsScreen
import com.carnation.fallalert.ui.screen.ConfirmationDoneScreen
import com.carnation.fallalert.ui.screen.ConfirmedListScreen
import com.carnation.fallalert.ui.screen.EventDetailScreen
import com.carnation.fallalert.ui.screen.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val CONFIRMED = "confirmed"
    const val ALL_ALERTS = "alerts"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{windowId}"
    const val DONE = "done/{windowId}/{confirmation}"

    // window_id 는 "trial-01:7" 처럼 ':' 를 포함하므로 반드시 인코딩한다.
    fun detail(windowId: String) = "detail/${Uri.encode(windowId)}"
    fun done(windowId: String, confirmation: Confirmation) =
        "done/${Uri.encode(windowId)}/${confirmation.name}"
}

private const val ARG_WINDOW_ID = "windowId"
private const val ARG_CONFIRMATION = "confirmation"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "홈", Icons.Filled.Home),
    CONFIRMED(Routes.CONFIRMED, "확인한 알림", Icons.Filled.TaskAlt),
}

@Composable
fun FallAlertNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // AndroidViewModel(Application) 은 기본 팩토리가 그대로 만들어 준다.
    val viewModel: FallAlertViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 탭은 최상위 두 화면에서만. 상세·설정처럼 파고든 화면에서는 뒤로 가기가 주 동선이다.
    val showTabs = currentRoute == Routes.HOME || currentRoute == Routes.CONFIRMED

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showTabs) BottomTabs(navController, currentRoute)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                val state by viewModel.listState.collectAsStateWithLifecycle()
                val connection by viewModel.connection.collectAsStateWithLifecycle()
                val pendingCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
                val inactivity by viewModel.inactivityMinutes.collectAsStateWithLifecycle()
                val profile by viewModel.parentProfile.collectAsStateWithLifecycle()

                AlertListScreen(
                    state = state,
                    connection = connection,
                    pendingSyncCount = pendingCount,
                    inactivityMinutes = inactivity,
                    parentProfile = profile,
                    onEventClick = { navController.navigate(Routes.detail(it)) },
                    onSeeAll = { navController.navigate(Routes.ALL_ALERTS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onSimulatePush = viewModel::requestSimulatedEvent,
                    onRetry = viewModel::refresh,
                )
            }

            composable(Routes.CONFIRMED) {
                val records by viewModel.confirmedRecords.collectAsStateWithLifecycle()
                ConfirmedListScreen(
                    records = records,
                    onEventClick = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(Routes.ALL_ALERTS) {
                val records by viewModel.unconfirmedRecords.collectAsStateWithLifecycle()
                AllAlertsScreen(
                    records = records,
                    onEventClick = { navController.navigate(Routes.detail(it)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val profile by viewModel.parentProfile.collectAsStateWithLifecycle()
                SettingsScreen(
                    profile = profile,
                    onSave = {
                        viewModel.updateParentProfile(it)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(ARG_WINDOW_ID) { type = NavType.StringType }),
            ) { entry ->
                val windowId = entry.arguments?.getString(ARG_WINDOW_ID).orEmpty()
                val detailFlow = remember(windowId) { viewModel.detailState(windowId) }
                val state by detailFlow.collectAsStateWithLifecycle()
                val profile by viewModel.parentProfile.collectAsStateWithLifecycle()

                EventDetailScreen(
                    state = state,
                    parentProfile = profile,
                    onConfirm = { confirmation ->
                        viewModel.confirm(windowId, confirmation)
                        navController.navigate(Routes.done(windowId, confirmation))
                    },
                    onBackToList = { navController.popBackStack() },
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
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Tab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            // 탭을 오갈 때 백스택이 쌓이지 않게 한다.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = MaterialTheme.typography.bodyMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
