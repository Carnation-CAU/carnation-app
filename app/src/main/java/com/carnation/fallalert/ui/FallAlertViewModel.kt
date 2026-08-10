package com.carnation.fallalert.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carnation.fallalert.data.AssetsFallEventSource
import com.carnation.fallalert.data.FallEventRepository
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.push.FallAlertNotifier
import com.carnation.fallalert.push.MockPushGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 목록 화면 상태. 화면 상태는 sealed 로 분기한다. (명세 4장) */
sealed interface AlertListUiState {
    data object Loading : AlertListUiState
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
    data class Ready(val record: EventRecord) : EventDetailUiState
}

class FallAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = FallEventRepository(AssetsFallEventSource(appContext))
    private val mockPush = MockPushGenerator()

    private val loading = MutableStateFlow(true)

    val listState: StateFlow<AlertListUiState> =
        combine(repository.records, loading) { records, isLoading ->
            when {
                isLoading -> AlertListUiState.Loading
                records.isEmpty() -> AlertListUiState.Empty
                else -> AlertListUiState.Ready(
                    unconfirmed = records.filter { it.isUnconfirmed },
                    history = records.filterNot { it.isUnconfirmed },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertListUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            repository.refresh()
            loading.value = false
        }
    }

    /** 확인 결과가 바뀌면 상세/완료 화면도 함께 갱신되도록 flow 로 노출한다. */
    fun detailState(windowId: String): StateFlow<EventDetailUiState> =
        combine(repository.records, loading) { records, isLoading ->
            when (val record = records.firstOrNull { it.id == windowId }) {
                null -> if (isLoading) EventDetailUiState.Loading else EventDetailUiState.NotFound
                else -> EventDetailUiState.Ready(record)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState.Loading)

    fun confirm(windowId: String, confirmation: Confirmation) {
        repository.confirm(windowId, confirmation)
    }

    /**
     * 개발용: FCM 없이 새 이벤트가 도착한 상황을 재현한다.
     * 서버 연동 후에는 실제 푸시가 같은 [FallEventRepository.upsert] 경로를 탄다.
     */
    fun simulateIncomingPush() {
        val event = mockPush.next()
        repository.upsert(event)
        FallAlertNotifier.notify(appContext, event)
    }
}
