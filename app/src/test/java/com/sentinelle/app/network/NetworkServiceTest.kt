package com.sentinelle.app.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: NetworkService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val base = server.url("/api/v2").toString().trimEnd('/')
        val host = server.url("/").toString().trimEnd('/')
        service = NetworkService(baseUrl = base, apiHost = host)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun jsonResponse(
        code: Int,
        body: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun jsonlResponse(
        code: Int,
        body: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(body)

    // ---------- POST /api/v2/reports ----------

    @Test
    fun reportPhoneNumber_public_sendsCountryHeaderAndBody() =
        runBlocking {
            server.enqueue(jsonResponse(201, """{"success":true,"message":"Report received"}"""))

            service.reportPhoneNumber("+33612345678", false, "dev-id", "FR", null)

            val req = server.takeRequest()
            assertEquals("POST", req.method)
            assertEquals("/api/v2/reports", req.path)
            assertEquals("application/json", req.getHeader("Content-Type"))
            assertEquals("dev-id", req.getHeader("X-Device-Id"))
            assertEquals("FR", req.getHeader("X-Country-Code"))
            assertNull(req.getHeader("X-API-Key"))
            val body = req.body.readUtf8()
            assertTrue(body.contains("\"phone\":\"+33612345678\""))
            assertTrue(body.contains("\"is_good\":false"))
            assertTrue(!body.contains("device_id"))
            assertTrue(!body.contains("country_code"))
            assertTrue(!body.contains("api_key"))
        }

    @Test
    fun reportPhoneNumber_enterprise_sendsApiKeyAndOmitsCountryHeader() =
        runBlocking {
            server.enqueue(jsonResponse(201, """{"success":true}"""))

            service.reportPhoneNumber("+33612345678", false, "dev-id", "FR", "org-key")

            val req = server.takeRequest()
            assertEquals("dev-id", req.getHeader("X-Device-Id"))
            assertEquals("org-key", req.getHeader("X-API-Key"))
            // Point 2: enterprise mode must NOT send X-Country-Code header
            assertNull(req.getHeader("X-Country-Code"))
            val body = req.body.readUtf8()
            assertTrue(body.contains("\"phone\":\"+33612345678\""))
            assertTrue(body.contains("\"is_good\":false"))
            assertTrue(!body.contains("device_id"))
            assertTrue(!body.contains("api_key"))
            assertTrue(!body.contains("country_code"))
        }

    @Test
    fun reportPhoneNumber_422_throwsValidationError() {
        server.enqueue(
            jsonResponse(
                422,
                """{"message":"The phone field is required.","errors":{"phone":["The phone field is required."]}}""",
            ),
        )
        val err =
            assertThrows(NetworkError.ValidationError::class.java) {
                runBlocking { service.reportPhoneNumber("", false, "dev-id", "FR", null) }
            }
        assertTrue(err.errors.containsKey("phone"))
    }

    @Test
    fun reportPhoneNumber_429_throwsTooManyRequests() {
        server.enqueue(jsonResponse(429, """{"message":"Too many requests."}"""))
        assertThrows(NetworkError.TooManyRequests::class.java) {
            runBlocking { service.reportPhoneNumber("+33612345678", false, "dev-id", "FR", null) }
        }
    }

    @Test
    fun reportPhoneNumber_401_throwsUnauthorized() {
        server.enqueue(jsonResponse(401, """{"message":"Unauthorized."}"""))
        assertThrows(NetworkError.Unauthorized::class.java) {
            runBlocking { service.reportPhoneNumber("+33612345678", false, "dev-id", "FR", "bad-key") }
        }
    }

    // ---------- POST /api/v2/health-check ----------

    @Test
    fun healthCheck_success_on204() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(204))

            service.healthCheck("dev-id", "org-key")

            val req = server.takeRequest()
            assertEquals("POST", req.method)
            assertEquals("/api/v2/health-check", req.path)
            assertEquals("dev-id", req.getHeader("X-Device-Id"))
            assertEquals("org-key", req.getHeader("X-API-Key"))
            val body = req.body.readUtf8()
            assertEquals("{}", body)
            assertTrue(!body.contains("device_id"))
            assertTrue(!body.contains("api_key"))
        }

    @Test
    fun healthCheck_401_throwsUnauthorized() {
        server.enqueue(jsonResponse(401, """{"message":"Unauthorized. Organization API key required."}"""))
        assertThrows(NetworkError.Unauthorized::class.java) {
            runBlocking { service.healthCheck("dev-id", "bad-key") }
        }
    }

    @Test
    fun healthCheck_422_throwsValidationError() {
        server.enqueue(
            jsonResponse(
                422,
                """{"message":"The device ID field is required.","errors":{"device_id":["The device ID field is required."]}}""",
            ),
        )
        val err =
            assertThrows(NetworkError.ValidationError::class.java) {
                runBlocking { service.healthCheck("", "org-key") }
            }
        assertTrue(err.errors.containsKey("device_id"))
    }

    // ---------- GET /api/v2/lists ----------

    @Test
    fun fetchAvailableLists_public_sendsCountryHeaderAndParsesArray() =
        runBlocking {
            server.enqueue(
                jsonResponse(
                    200,
                    """[{"id":1,"name":"FR list","description":null,"license":"ODBL","is_enabled_by_default":true,"priority":80,"channel":"phone","type":"block","download_url":"https://app.saracroche.org/storage/fr.jsonl","version":"2026-01-04T18:07:00.000000Z"}]""",
                ),
            )

            val lists = service.fetchAvailableLists("FR", null, null)

            val req = server.takeRequest()
            assertEquals("GET", req.method)
            assertEquals("/api/v2/lists", req.path)
            assertEquals("FR", req.getHeader("X-Country-Code"))
            assertNull(req.getHeader("X-Device-Id"))
            assertNull(req.getHeader("X-API-Key"))
            assertEquals(1, lists.size)
            assertEquals("FR list", lists[0].name)
            assertEquals(80, lists[0].priority)
            assertEquals("block", lists[0].type)
        }

    @Test
    fun fetchAvailableLists_enterprise_sendsDeviceAndApiKey() =
        runBlocking {
            server.enqueue(
                jsonResponse(200, """[{"id":2,"name":"Org list","download_url":"/api/v2/lists/2","version":"v2"}]"""),
            )

            service.fetchAvailableLists("FR", "dev-id", "org-key")

            val req = server.takeRequest()
            assertEquals("dev-id", req.getHeader("X-Device-Id"))
            assertEquals("org-key", req.getHeader("X-API-Key"))
            assertNull(req.getHeader("X-Country-Code"))
        }

    @Test
    fun fetchAvailableLists_422_throwsValidationError() {
        server.enqueue(
            jsonResponse(
                422,
                """{"message":"The country code field is required.","errors":{"country_code":["The country code field is required."]}}""",
            ),
        )
        val err =
            assertThrows(NetworkError.ValidationError::class.java) {
                runBlocking { service.fetchAvailableLists("", null, null) }
            }
        assertTrue(err.errors.containsKey("country_code"))
    }

    @Test
    fun fetchAvailableLists_429_throwsTooManyRequests() {
        server.enqueue(jsonResponse(429, """{"message":"Too many requests."}"""))
        assertThrows(NetworkError.TooManyRequests::class.java) {
            runBlocking { service.fetchAvailableLists("FR", null, null) }
        }
    }

    // ---------- GET /api/v2/lists/{id} (and public static JSONL) ----------

    @Test
    fun downloadListPatterns_orgList_parsesJsonlAndSendsAuth() =
        runBlocking {
            server.enqueue(
                jsonlResponse(
                    200,
                    """{"name":"Prefixe demarchage ARCEP","pattern":"33162######"}
{"name":"Another blocked prefix","pattern":"339#######"}""",
                ),
            )

            val patterns = service.downloadListPatterns("/api/v2/lists/1", "dev-id", "org-key")

            val req = server.takeRequest()
            assertEquals("GET", req.method)
            assertEquals("/api/v2/lists/1", req.path)
            assertEquals("application/x-ndjson", req.getHeader("Accept"))
            assertEquals("dev-id", req.getHeader("X-Device-Id"))
            assertEquals("org-key", req.getHeader("X-API-Key"))
            assertEquals(2, patterns.size)
            assertEquals("Prefixe demarchage ARCEP", patterns[0].name)
            assertEquals("33162######", patterns[0].pattern)
            assertEquals("Another blocked prefix", patterns[1].name)
            assertEquals("339#######", patterns[1].pattern)
        }

    @Test
    fun downloadListPatterns_publicUrl_omitsAuthHeaders() =
        runBlocking {
            val publicUrl = server.url("/storage/fr.jsonl").toString()
            server.enqueue(jsonlResponse(200, """{"name":"P","pattern":"331######"}"""))

            val patterns = service.downloadListPatterns(publicUrl, "dev-id", "org-key")

            val req = server.takeRequest()
            assertEquals("/storage/fr.jsonl", req.path)
            assertNull(req.getHeader("X-Device-Id"))
            assertNull(req.getHeader("X-API-Key"))
            assertEquals(1, patterns.size)
            assertEquals("P", patterns[0].name)
        }

    @Test
    fun downloadListPatterns_404_throwsServerErrorWithDetail() {
        server.enqueue(
            jsonlResponse(404, """{"errors":[{"status":404,"title":"Not Found","detail":"List not found."}]}"""),
        )
        val err =
            assertThrows(NetworkError.ServerError::class.java) {
                runBlocking { service.downloadListPatterns("/api/v2/lists/999", "dev-id", "org-key") }
            }
        assertEquals(404, err.code)
        assertEquals("List not found.", err.serverMessage)
    }

    @Test
    fun downloadListPatterns_429_throwsTooManyRequests() {
        server.enqueue(jsonlResponse(429, """{"message":"Too many requests."}"""))
        assertThrows(NetworkError.TooManyRequests::class.java) {
            runBlocking { service.downloadListPatterns("/api/v2/lists/1", "dev-id", "org-key") }
        }
    }
}
