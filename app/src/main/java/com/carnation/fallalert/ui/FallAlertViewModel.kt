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
import com.carnation.fallalert.model.NormalReason
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.push.FallAlertNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * 장시간 무동작 시간(분). **목업이다.**
     *
     * 서버 계약(`FallEvent`)에는 무동작을 나타내는 필드가 없다. 계약을 건드리지 않기로 했으므로
     * 값을 여기에 고정해 두고 화면만 먼저 만든다.
     * TODO: 팀이 무동작 이벤트 형식을 정하면 이 값을 서버 실시간 값으로 교체한다.
     */
    private val _inactivityMinutes = MutableStateFlow(192) // 3시간 12분
    val inactivityMinutes: StateFlow<Int> = _inactivityMinutes.asStateFlow()

    /**
     * 부모님 정보. 설정 화면에서 수정하며, 지금은 메모리에만 산다.
     * TODO: DataStore 영구 저장 (현관 비밀번호가 들어가므로 암호화 검토).
     */
    private val _parentProfile = MutableStateFlow(ParentProfile.MOCK)
    val parentProfile: StateFlow<ParentProfile> = _parentProfile.asStateFlow()

    fun updateParentProfile(profile: ParentProfile) {
        _parentProfile.value = profile
    }

    /** 확인한 알림 탭. 최신순은 저장소가 이미 보장한다. */
    val confirmedRecords: StateFlow<List<EventRecord>> = repository.records
        .map { records -> records.filterNot { it.isUnconfirmed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 홈의 "더 보기"에서 여는 전체 미확인 목록. */
    val unconfirmedRecords: StateFlow<List<EventRecord>> = repository.records
        .map { records -> records.filter { it.isUnconfirmed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** 119 로 정보를 보내기로 한 순간 — 그게 곧 "도움 필요" 판단이다. */
    fun markHelpNeeded(windowId: String) {
        viewModelScope.launch { repository.confirm(windowId, Confirmation.HELP_NEEDED) }
    }

    /**
     * "도움은 필요 없어요" + 실제 상황. [reasons] 는 비어 있을 수 있다 —
     * 입력을 강제하면 급한 사람이 아무거나 찍고, 그 데이터가 정확도를 오히려 망친다.
     */
    fun markNoHelpNeeded(windowId: String, reasons: List<NormalReason>) {
        viewModelScope.launch {
            repository.confirm(windowId, Confirmation.NORMAL, reasons.map { it.code })
        }
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
