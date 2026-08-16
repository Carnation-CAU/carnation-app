package com.carnation.fallalert

import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.data.ConfirmationOutbox
import com.carnation.fallalert.data.OutboxStorage
import com.carnation.fallalert.data.remote.EventStreamClient
import com.carnation.fallalert.model.Confirmation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 디스크를 흉내 내는 저장소. 프로세스 재시작은 같은 raw 로 새 Outbox 를 만들어 재현한다. */
private class FakeStorage(var raw: String? = null) : OutboxStorage {
    var writes = 0
    override fun read(): String? = raw
    override fun write(raw: String) {
        this.raw = raw
        writes += 1
    }
    override fun clear() {
        raw = null
    }
}

class ConfirmationOutboxTest {

    private fun report(windowId: String, confirmation: Confirmation) = ConfirmationReport(
        windowId = windowId,
        confirmation = confirmation,
        reportedAt = "2026-08-15T19:45:00+09:00",
        clientId = "test-device",
    )

    @Test
    fun `넣은 보고가 대기 목록에 뜬다`() {
        val outbox = ConfirmationOutbox(FakeStorage())
        outbox.enqueue(report("w:1", Confirmation.HELP_NEEDED))

        assertTrue(outbox.isPending("w:1"))
        assertFalse(outbox.isPending("w:2"))
        assertEquals(1, outbox.snapshot().size)
    }

    @Test
    fun `앱을 재시작해도 미전송 보고가 남는다`() {
        val storage = FakeStorage()
        ConfirmationOutbox(storage).enqueue(report("w:1", Confirmation.HELP_NEEDED))

        // 같은 저장소로 새 인스턴스 = 프로세스 재시작
        val restarted = ConfirmationOutbox(storage)

        assertEquals(1, restarted.snapshot().size)
        assertEquals(Confirmation.HELP_NEEDED, restarted.snapshot().first().confirmation)
    }

    @Test
    fun `같은 이벤트를 다시 확인하면 마지막 것만 남는다`() {
        val outbox = ConfirmationOutbox(FakeStorage())
        outbox.enqueue(report("w:1", Confirmation.NORMAL))
        outbox.enqueue(report("w:1", Confirmation.HELP_NEEDED))

        assertEquals(1, outbox.snapshot().size)
        assertEquals(Confirmation.HELP_NEEDED, outbox.snapshot().first().confirmation)
    }

    @Test
    fun `전송 완료된 보고는 디스크에서도 사라진다`() {
        val storage = FakeStorage()
        val outbox = ConfirmationOutbox(storage)
        outbox.enqueue(report("w:1", Confirmation.NORMAL))
        outbox.enqueue(report("w:2", Confirmation.NORMAL))

        outbox.remove("w:1")

        assertEquals(listOf("w:2"), outbox.snapshot().map { it.windowId })
        assertEquals(listOf("w:2"), ConfirmationOutbox(storage).snapshot().map { it.windowId })
    }

    @Test
    fun `저장 형식이 깨져 있어도 앱이 뜬다`() {
        val storage = FakeStorage(raw = "{이건 예전 형식}")
        val outbox = ConfirmationOutbox(storage)

        assertTrue(outbox.snapshot().isEmpty())
        assertEquals(null, storage.raw)
    }

    @Test
    fun `재연결 백오프는 1초에서 시작해 30초에서 멈춘다`() {
        val delays = (1..8).map { EventStreamClient.backoffSeconds(it) }
        assertEquals(listOf(1L, 2L, 4L, 8L, 16L, 30L, 30L, 30L), delays)
    }
}
