package com.carnation.fallalert

import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import java.io.File
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `fall_event.schema.json` 이 Kotlin 모델과 어긋나지 않는지 확인한다.
 *
 * 스키마 파일은 다른 언어(모델·CSI 팀은 Python)에서 검증하라고 내주는 사본이다.
 * 사본은 반드시 낡는다 — 필드를 Kotlin 에만 추가하고 스키마를 안 고치면,
 * 모델팀은 통과했다고 믿는데 서버는 거부하는 상황이 된다. 그걸 여기서 막는다.
 */
class SchemaFileTest {

    private val schema = Json.parseToJsonElement(
        File(System.getProperty("user.dir"), "fall_event.schema.json").readText()
    ).jsonObject

    private fun SerialDescriptor.names(): Set<String> =
        (0 until elementsCount).map { getElementName(it) }.toSet()

    private fun SerialDescriptor.requiredNames(): Set<String> =
        (0 until elementsCount).filterNot { isElementOptional(it) }.map { getElementName(it) }.toSet()

    private fun properties(node: kotlinx.serialization.json.JsonObject): Set<String> =
        node["properties"]!!.jsonObject.keys

    private fun required(node: kotlinx.serialization.json.JsonObject): Set<String> =
        node["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()

    @Test
    fun `최상위 필드가 Kotlin 모델과 일치한다`() {
        val descriptor = FallEvent.serializer().descriptor
        assertEquals(descriptor.names(), properties(schema))
        assertEquals(descriptor.requiredNames(), required(schema))
    }

    @Test
    fun `evidence 필드가 Kotlin 모델과 일치한다`() {
        val node = schema["properties"]!!.jsonObject["evidence"]!!.jsonObject
        val descriptor = Evidence.serializer().descriptor

        assertEquals(descriptor.names(), properties(node))
        // no_recovery_sec 은 기본값이 있는 선택 필드라 required 에 없어야 한다.
        assertEquals(descriptor.requiredNames(), required(node))
        assertTrue("no_recovery_sec" !in required(node))
    }

    @Test
    fun `presence_state 열거값이 일치한다`() {
        val enumValues = schema["properties"]!!.jsonObject["evidence"]!!.jsonObject["properties"]!!
            .jsonObject["presence_state"]!!.jsonObject["enum"]!!
            .jsonArray.map { it.jsonPrimitive.content }.toSet()

        assertEquals(PresenceState.serializer().descriptor.names(), enumValues)
    }

    @Test
    fun `스키마의 고정값이 계약 검증과 일치한다`() {
        val props = schema["properties"]!!.jsonObject
        assertEquals("1.0", props["schema_version"]!!.jsonObject["const"]!!.jsonPrimitive.content)
        assertEquals(
            "fall_suspected",
            props["event_type"]!!.jsonObject["const"]!!.jsonPrimitive.content,
        )
    }
}
