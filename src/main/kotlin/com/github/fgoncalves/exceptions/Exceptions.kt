package com.github.fgoncalves.exceptions

import okhttp3.Response
import java.lang.reflect.Method

object NoBodyException : RuntimeException("Body is null. Cannot adapt a body that doesn't exist.")

class MissingQueryException(method: Method) :
    RuntimeException("No Query annotation for method $method")

class HttpException(
    val response: Response,
) : RuntimeException("${response.code} ${response.message}")
