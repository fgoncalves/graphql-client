package com.github.fgoncalves.adapters

import com.github.fgoncalves.exceptions.NoBodyException
import com.squareup.moshi.Moshi
import okhttp3.Response

class MoshiResponseAdapter(
    private val moshi: Moshi = Moshi.Builder().build(),
) : ResponseAdapter {
    override fun <R> adapt(clazz: Class<R>, response: Response): R {
        val body = response.body ?: throw NoBodyException

        val deserialized = moshi.adapter(clazz)
            .fromJson(body.source())
        return requireNotNull(deserialized) {
            "Cannot deserialize null body"
        }
    }
}