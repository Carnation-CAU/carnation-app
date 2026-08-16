package com.carnation.fallalert.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carnation.fallalert.BuildConfig
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.data.ConfirmationOutbox
import com.carnation.fallalert.data.FallEventRepository
import com.carnation.fallalert.data.LoadState
import com.carnation.fallalert.data.remote.CarnationApi
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.push.FallAlertNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 목록 화면 상태. (명세 4장 — 화면 상태는 sealed 로) */
sealed interface AlertListUiState {
    data object Loading : AlertListUiState
    data class Failed(val reason: String) : AlertListUiState
    data object Empty : AlertListUiState
    data class Ready(
        val unconfirmed: List<EventRecord>,
        val history: List<EventRecord>,
    ) : AlertListUiState
}

/** 상세 화면 상태. */
sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Ready(val record: EventRecord, val pendingSync: Boolean) : EventDetailUiState
}

class FallAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val api = CarnationApi(BuildConfig.SERVER_BASE_URL)
    private val outbox = ConfirmationOutbox(appContext)

    private val repository = FallEventRepository(
        api = api,
        outbox = outbox,
        clientId = deviceId(),
    )

    val connection: StateFlow<ConnectionState> = repository.connection

    /** 아직 서버에 못 보낸 확인 결과들. 화면 C 가 window_id 로 조회한다. */
    val pendingSync: StateFlow<List<ConfirmationReport>> = repository.pendingSync

    /** 아직 서버에 못 보낸 확인 결과 수. 0 이면 화면에 아무것도 띄우지 않는다. */
    val pendingSyncCount: StateFlow<Int> = repository.pendingSync
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val listState: StateFlow<AlertListUiState> =
        combine(repository.records, repository.loadState) { records, load ->
            when {
                records.isNotEmpty() -> AlertListUiState.Ready(
                    unconfirmed = records.filter { it.isUnconfirmed },
                    history = records.filterNot { it.isUnconfirmed },
                )
                load is LoadState.Loading -> AlertListUiState.Loading
                load is LoadState.Failed -> AlertListUiState.Failed(load.reason)
                else -> AlertListUiState.Empty
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertListUiState.Loading)

    init {
        repository.start(viewModelScope)

        // 새 이벤트가 스트림으로 들어오면 알림을 띄운다.
        // 앱이 완전히 종료된 동안은 못 받는다 — 그건 FCM 이 필요하다 (push/FCM_SETUP.md).
        viewModelScope.launch {
            repository.newEvents.collect { event ->
                FallAlertNotifier.notify(appContext, event)
            }
        }
    }

    fun detailState(windowId: String): StateFlow<EventDetailUiState> =
        combine(repository.records, repository.loadState, repository.pendingSync) { records, load, pending ->
            when (val record = records.firstOrNull { it.id == windowId }) {
                null -> if (load is LoadState.Loading) {
                    EventDetailUiState.Loading
                } else {
                    EventDetailUiState.NotFound
                }
                else -> EventDetailUiState.Ready(
                    record = record,
                    pendingSync = pending.any { it.windowId == windowId },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState.Loading)

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
            repository.flushOutbox()
        }
    }

    fun confirm(windowId: String, confirmation: Confirmation) {
        viewModelScope.launch { repository.confirm(windowId, confirmation) }
    }

    /**
     * 개발용: 서버에 모의 이벤트를 만들라고 요청한다.
     * 결과는 WebSocket 으로 되돌아오므로 **실제 수신 경로를 그대로 검증**한다.
     */
    fun requestSimulatedEvent() {
        viewModelScope.launch { repository.requestSimulatedEvent() }
    }

    /** 서버가 어느 기기의 보고인지 구분할 수 있도록. 인증이 붙기 전까지의 임시 식별자. */
    @Suppress("HardwareIds")
    private fun deviceId(): String =
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
}
