package com.github.fgoncalves

import com.github.fgoncalves.adapters.ResponseAdapter
import com.github.fgoncalves.requests.RequestInvocationHandler
import okhttp3.OkHttpClient
import java.lang.reflect.Proxy

class GraphQlClient(
    private val host: String,
    private val responseAdapter: ResponseAdapter,
    private val client: OkHttpClient = OkHttpClient(),
) {

    @Suppress("UNCHECKED_CAST")
    fun <T> create(clazz: Class<T>): T =
        Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz),
            RequestInvocationHandler(
                host,
                client,
                responseAdapter,
            )
        ) as T
}

inline fun <reified T> GraphQlClient.create() = create(T::class.java)