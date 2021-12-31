package com.github.fgoncalves.queries

import com.github.fgoncalves.requests.RequestInvocationHandler
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

const val SIMPLE_QUERY = """query HeroNameAndFriends {
                              hero {
                                name
                                friends {
                                  name
                                }
                              }
                            }
                        """

const val QUERY_WITH_VARS = """
    query HeroNameAndFriends(${'$'}episode: String, ${'S'}id: Integer) {
  hero(episode: ${'$'}episode, id: ${'S'}id) {
    name
    friends {
      name
    }
  }
}
"""

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

internal fun String.toJson(variables: Map<String, Any>? = null) =
    moshi.adapter(RequestInvocationHandler.Query::class.java)
        .toJson(RequestInvocationHandler.Query(this, variables))