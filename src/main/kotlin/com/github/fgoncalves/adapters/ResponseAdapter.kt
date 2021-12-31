package com.github.fgoncalves.adapters

import okhttp3.Response
import okio.IOException

interface ResponseAdapter {
    @Throws(IOException::class)
    fun <R> adapt(clazz: Class<R>, response: Response): R
}