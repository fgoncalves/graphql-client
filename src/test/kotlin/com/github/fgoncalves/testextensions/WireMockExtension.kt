package com.github.fgoncalves.testextensions

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.common.Notifier
import com.github.tomakehurst.wiremock.common.ProxySettings
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

private val DEFAULT_WIREMOCK_CONFIG = WireMockConfiguration.wireMockConfig()

class WireMockExtension(
    port: Int = DEFAULT_WIREMOCK_CONFIG.portNumber(),
    httpsPort: Int? = DEFAULT_WIREMOCK_CONFIG.httpsSettings().port(),
    fileSource: FileSource? = DEFAULT_WIREMOCK_CONFIG.filesRoot(),
    enableBrowserProxying: Boolean = DEFAULT_WIREMOCK_CONFIG.browserProxyingEnabled(),
    proxySettings: ProxySettings? = DEFAULT_WIREMOCK_CONFIG.proxyVia(),
    notifier: Notifier? = DEFAULT_WIREMOCK_CONFIG.notifier(),
) : WireMockServer(
    port,
    httpsPort,
    fileSource,
    enableBrowserProxying,
    proxySettings,
    notifier,
), BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext?) {
        start()
    }

    override fun afterEach(context: ExtensionContext?) {
        stop()
        resetAll()
    }
}