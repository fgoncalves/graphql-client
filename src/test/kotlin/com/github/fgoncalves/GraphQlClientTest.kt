package com.github.fgoncalves

import com.github.fgoncalves.adapters.MoshiResponseAdapter
import com.github.fgoncalves.annotations.Query
import com.github.fgoncalves.annotations.Var
import com.github.fgoncalves.exceptions.HttpException
import com.github.fgoncalves.exceptions.MissingQueryException
import com.github.fgoncalves.queries.QUERY_WITH_VARS
import com.github.fgoncalves.queries.SIMPLE_QUERY
import com.github.fgoncalves.queries.toJson
import com.github.fgoncalves.testextensions.WireMockExtension
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.Notifier
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

internal class GraphQlClientTest {
    @JvmField
    @RegisterExtension
    val server = WireMockExtension(
        port = 9999,
        notifier = object : Notifier {
            override fun info(message: String?) {
                println("INFO: $message")
            }

            override fun error(message: String?) {
                println("ERROR: $message")
            }

            override fun error(message: String?, t: Throwable?) {
                println("ERROR: $message")
                t?.printStackTrace()
            }
        },
    )

    private val graphQlClient = GraphQlClient(
        host = "http://127.0.0.1:9999/graphql",
        responseAdapter = MoshiResponseAdapter(),
    )
    private val service = graphQlClient.create<Service>()
    private val serviceCoroutine = graphQlClient.create<ServiceCoroutine>()

    @Test
    fun `should pass through object class methods`() {
        assertThat(service.equals(123)).isFalse()
        assertThat(service.hashCode()).isNotNull()
        assertThat(service.toString()).isNotNull()
    }

    @Test
    fun `should fail when there's no query annotation`() {
        assertThrows<MissingQueryException> {
            service.noQueryAnnotation()
        }
    }

    @Test
    fun `should execute simple queries`() {
        mockWithSuccess(SIMPLE_QUERY.toJson())

        val result = service.simpleQuery()

        assertThat(result).isEqualTo("SUCCESS")
    }

    @Test
    fun `should propagate http exceptions`() {
        mockWithNotFound(SIMPLE_QUERY.toJson())

        val actual = assertThrows<HttpException> {
            service.simpleQuery()
        }

        assertThat(actual.response.code).isEqualTo(404)
        assertThat(actual.message).isEqualTo("404 Not Found")
    }

    @Test
    fun `should execute queries with vars`() {
        mockWithSuccess(
            QUERY_WITH_VARS.toJson(
                mapOf(
                    "episode" to "foo",
                    "id" to 123,
                )
            )
        )

        val result = service.withVars("foo", 123)

        assertThat(result).isEqualTo("SUCCESS")
    }

    @Test
    fun `should execute simple queries in coroutines too`() = runBlocking {
        mockWithSuccess(SIMPLE_QUERY.toJson())

        val result = serviceCoroutine.simpleQuery()

        assertThat(result).isEqualTo("SUCCESS")
    }

    @Test
    fun `should propagate http exceptions in coroutines too`() = runBlocking {
        mockWithNotFound(SIMPLE_QUERY.toJson())

        val actual = assertThrows<HttpException> {
            serviceCoroutine.simpleQuery()
        }

        assertThat(actual.response.code).isEqualTo(404)
        assertThat(actual.message).isEqualTo("404 Not Found")
    }

    @Test
    fun `should execute queries with vars in coroutines too`() = runBlocking {
        mockWithSuccess(
            QUERY_WITH_VARS.toJson(
                mapOf(
                    "episode" to "foo",
                    "id" to 123,
                )
            )
        )

        val result = serviceCoroutine.withVars("foo", 123)

        assertThat(result).isEqualTo("SUCCESS")
    }

    private fun mockWithSuccess(queryJson: String) {
        server.stubFor(
            post("/graphql")
                .withRequestBody(equalTo(queryJson))
                .willReturn(ok().withBody("\"SUCCESS\""))
        )
    }

    private fun mockWithNotFound(queryJson: String) {
        server.stubFor(
            post("/graphql")
                .withRequestBody(equalTo(queryJson))
                .willReturn(notFound())
        )
    }

    interface Service {
        fun noQueryAnnotation()

        @Query(SIMPLE_QUERY)
        fun simpleQuery(): String

        @Query(QUERY_WITH_VARS)
        fun withVars(
            @Var(name = "episode") episode: String,
            @Var(name = "id") id: Int,
        ): String
    }

    interface ServiceCoroutine {
        @Query(SIMPLE_QUERY)
        suspend fun simpleQuery(): String

        @Query(QUERY_WITH_VARS)
        suspend fun withVars(
            @Var(name = "episode") episode: String,
            @Var(name = "id") id: Int,
        ): String
    }
}