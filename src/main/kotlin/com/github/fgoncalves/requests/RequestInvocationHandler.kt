package com.github.fgoncalves.requests

import com.github.fgoncalves.adapters.ResponseAdapter
import com.github.fgoncalves.annotations.Var
import com.github.fgoncalves.exceptions.HttpException
import com.github.fgoncalves.exceptions.MissingQueryException
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.rawType
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction
import com.github.fgoncalves.annotations.Query as QueryAnnotation


internal class RequestInvocationHandler(
    private val host: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val responseAdapter: ResponseAdapter,
) : InvocationHandler {
    // internal instance to deal with proper json encoding and escaping
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        // If the method is a method from Object then defer to normal invocation.
        if (method.declaringClass === Any::class.java) {
            return if (args != null)
                method.invoke(this, args)
            else
                method.invoke(this)
        }

        val continuation = args?.lastOrNull()
        val call = buildCall(method, args)

        // For coroutines the last argument is a continuation
        return if (continuation is Continuation<*>) {
            val kotlinFunction = requireNotNull(method.kotlinFunction) {
                "Picked up a suspend function but there's no kotlinFunction for it."
            }
            val returnType = kotlinFunction.returnType.javaType.rawType
            invokeCoroutine(continuation, call, returnType)
            COROUTINE_SUSPENDED
        } else {
            val returnType = method.returnType
            invokeSync(call, returnType)
        }
    }

    private fun invokeCoroutine(
        continuation: Any?,
        call: Call,
        returnType: Class<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        (continuation as Continuation<Any>).let {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (returnType != Unit::class.java && returnType != Nothing::class.java) {
                        if (response.isSuccessful)
                            it.resumeWith(Result.success(response.toType(returnType)))
                        else
                            it.resumeWith(Result.failure(HttpException(response)))
                    }
                }
            })
        }
    }

    private fun invokeSync(call: Call, returnType: Class<*>): Any {
        val response = call.execute()
        return if (response.isSuccessful)
            response.toType(returnType)
        else
            throw HttpException(response)
    }

    private fun <T> Response.toType(clazz: Class<T>): T =
        responseAdapter.adapt(clazz, this)

    private fun buildCall(method: Method, args: Array<out Any>?): Call {
        val query = loadQuery(method)
        val variables = collectVariables(method, args)

        val request = Request.Builder()
            .url(host.toHttpUrl())
            .post(requestBody(query, variables))
            .build()

        return client.newCall(request)
    }

    private fun requestBody(query: String, variables: Map<String, Any>): RequestBody {
        val json = moshi.adapter(Query::class.java)
            .toJson(Query(query, variables.ifEmpty { null }))

        return json.toRequestBody(
            "application/json".toMediaType(),
        )
    }

    private fun collectVariables(
        method: Method,
        args: Array<out Any>?,
    ): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()
        method.parameters.forEachIndexed { index, parameter ->
            val varAnnotation = parameter.annotations.find {
                it is Var
            }?.let {
                it as Var
            }
            if (varAnnotation != null)
                variables[varAnnotation.name] = args!![index]
        }
        return variables
    }

    private fun loadQuery(method: Method): String =
        method.annotations.firstOrNull {
            it is QueryAnnotation
        }?.let {
            (it as QueryAnnotation).query
        } ?: throw MissingQueryException(method)

    internal data class Query(
        @Json(name = "query")
        val query: String,
        @Json(name = "variables")
        val variables: Map<String, Any>? = null,
    )
}