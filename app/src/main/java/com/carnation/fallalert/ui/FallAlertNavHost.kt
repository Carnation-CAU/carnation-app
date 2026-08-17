package com.carnation.fallalert.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carnation.fallalert.ui.screen.AlertListScreen
import com.carnation.fallalert.ui.screen.AllAlertsScreen
import com.carnation.fallalert.ui.screen.ConfirmedListScreen
import com.carnation.fallalert.ui.screen.EventDetailScreen
import com.carnation.fallalert.ui.screen.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val CONFIRMED = "confirmed"
    const val ALL_ALERTS = "alerts"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{windowId}"

    // window_id 는 "trial-01:7" 처럼 ':' 를 포함하므로 반드시 인코딩한다.
    fun detail(windowId: String) = "detail/${Uri.encode(windowId)}"
}

private const val ARG_WINDOW_ID = "windowId"

/**
 * 확인을 기록한 뒤 "확인한 알림"으로 넘어간다.
 *
 * 별도의 완료 화면을 두지 않는다. 방금 처리한 건이 확인한 알림 목록 맨 위에 실제로 올라와
 * 있는 걸 보는 게, "접수했어요"라는 문장보다 확실한 확인이다.
 * 상세 화면은 스택에서 걷어내 뒤로 가기가 홈으로 향하게 한다.
 */
private fun goToConfirmed(navController: NavHostController) {
    navController.navigate(Routes.CONFIRMED) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * 하단 탭은 두지 않는다.
 *
 * 탭은 "둘 다 자주 오간다"는 신호인데, 확인한 알림은 사실 가끔 되짚어 보는 이력이다.
 * 탭으로 두면 화면 아래를 늘 차지하면서 홈의 대응 버튼과 자리를 다툰다.
 * 그래서 홈 안에서 들어가는 한 방향 동선으로 바꿨다.
 */
@Composable
fun FallAlertNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // AndroidViewModel(Application) 은 기본 팩토리가 그대로 만들어 준다.
    val viewModel: FallAlertViewModel = viewModel()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                val confirmed by viewModel.confirmedRecords.collectAsStateWithLifecycle()

                AlertListScreen(
                    state = state,
                    connection = connection,
                    pendingSyncCount = pendingCount,
                    inactivityMinutes = inactivity,
                    parentProfile = profile,
                    confirmedCount = confirmed.size,
                    onEventClick = { navController.navigate(Routes.detail(it)) },
                    onSeeAll = { navController.navigate(Routes.ALL_ALERTS) },
                    onOpenConfirmed = { navController.navigate(Routes.CONFIRMED) },
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
                    onBack = { navController.popBackStack() },
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
                    onHelpNeeded = {
                        viewModel.markHelpNeeded(windowId)
                        goToConfirmed(navController)
                    },
                    onNoHelpNeeded = { reasons ->
                        viewModel.markNoHelpNeeded(windowId, reasons)
                        goToConfirmed(navController)
                    },
                    onBackToList = { navController.popBackStack() },
                )
            }
        }
    }
}

