package com.sentinelle.app.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsTest {
    private val gson = Gson()

    @Test
    fun reportRequestSerializesWithSnakeCaseFields() {
        // deviceId/apiKey/countryCode are sent as HTTP headers by NetworkService,
        // not as body fields — see NetworkServiceTest for that coverage.
        val request =
            ReportRequest(
                phone = "33612345678",
                isGood = false,
            )
        val json = gson.toJson(request)
        assertTrue(json.contains("\"phone\":\"33612345678\""))
        assertTrue(json.contains("\"is_good\":false"))
    }

    @Test
    fun reportRequestRoundTrips() {
        val request =
            ReportRequest(
                phone = "33612345678",
                isGood = true,
            )
        val out = gson.fromJson(gson.toJson(request), ReportRequest::class.java)
        assertEquals(request, out)
    }

    @Test
    fun listSummaryDeserializesFromApiPayload() {
        val json =
            """
            {
              "id": 1,
              "name": "French list",
              "description": "Prefixes ARCEP",
              "license": "ODBL",
              "is_enabled_by_default": true,
              "priority": 80,
              "channel": "phone",
              "type": "block",
              "download_url": "https://app.saracroche.org/api/v2/lists/1",
              "version": "2026-01-04T18:07:00.000000Z"
            }
            """.trimIndent()
        val summary = gson.fromJson(json, ListSummary::class.java)
        assertEquals(1L, summary.id)
        assertEquals("French list", summary.name)
        assertEquals("Prefixes ARCEP", summary.description)
        assertEquals("ODBL", summary.license)
        assertEquals(true, summary.isEnabledByDefault)
        assertEquals(80, summary.priority)
        assertEquals("phone", summary.channel)
        assertEquals("block", summary.type)
        assertEquals("https://app.saracroche.org/api/v2/lists/1", summary.downloadUrl)
        assertEquals("2026-01-04T18:07:00.000000Z", summary.version)
    }

    @Test
    fun listSummaryHandlesNullOptionalFields() {
        val json = """{"id":2,"name":"Be","download_url":"https://x/y.jsonl","version":"v1"}"""
        val summary = gson.fromJson(json, ListSummary::class.java)
        assertEquals(2L, summary.id)
        assertEquals("Be", summary.name)
        assertNull(summary.description)
        assertNull(summary.license)
        assertNull(summary.isEnabledByDefault)
        assertNull(summary.priority)
        assertNull(summary.channel)
        assertNull(summary.type)
    }

    @Test
    fun listSummaryArrayDeserializes() {
        val json =
            """
            [
              {"id":1,"name":"A","download_url":"u1","version":"v1"},
              {"id":2,"name":"B","download_url":"u2","version":"v2"}
            ]
            """.trimIndent()
        val type = object : TypeToken<List<ListSummary>>() {}.type
        val list: List<ListSummary> = gson.fromJson(json, type)
        assertEquals(2, list.size)
        assertEquals("A", list[0].name)
        assertEquals("B", list[1].name)
    }

    @Test
    fun listPatternInfoDeserializesFromJsonlLine() {
        val line = """{"name":"Prefixe demarchage ARCEP","pattern":"33162######"}"""
        val info = gson.fromJson(line, ListPatternInfo::class.java)
        assertEquals("Prefixe demarchage ARCEP", info.name)
        assertEquals("33162######", info.pattern)
    }

    @Test
    fun errorResponseParsesJsonApi404() {
        val json = """{"errors":[{"status":404,"title":"Not Found","detail":"List not found."}]}"""
        val resp = gson.fromJson(json, ErrorResponse::class.java)
        assertNull(resp.message)
        assertNotNull(resp.errors)
        val first = resp.errors!!.first()
        assertEquals(404, first.status)
        assertEquals("Not Found", first.title)
        assertEquals("List not found.", first.detail)
    }

    @Test
    fun errorResponseParsesMessageOnly() {
        val json = """{"message":"Too many requests."}"""
        val resp = gson.fromJson(json, ErrorResponse::class.java)
        assertEquals("Too many requests.", resp.message)
        assertNull(resp.errors)
    }
}
