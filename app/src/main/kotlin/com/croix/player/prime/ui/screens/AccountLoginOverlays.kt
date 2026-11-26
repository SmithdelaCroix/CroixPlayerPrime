/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.engine.SpotifyTasteClient

// Clean Chrome Mobile UA without "; wv)" so Google/Apple OAuth popups work cleanly inside WebView
private const val LOGIN_MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Mobile Safari/537.36"

private const val YTMUSIC_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

/**
 * Spotify login overlay with full OAuth popup window support (Google / Apple / Facebook)
 * and multi-domain cookie capture (open.spotify.com, accounts.spotify.com, spotify.com)
 * plus inline sp_dc quick paste.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun SpotifyLoginOverlay(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }
    var spDcManual by remember { mutableStateOf("") }
    val blocker = remember { MutableInteractionSource() }

    DisposableEffect(Unit) {
        onDispose {
            activeWebView?.destroyCleanly()
            mainWebView?.takeIf { it !== activeWebView }?.destroyCleanly()
            activeWebView = null
            mainWebView = null
        }
    }

    BackHandler {
        val active = activeWebView
        val root = mainWebView
        when {
            active?.canGoBack() == true -> active.goBack()
            active != null && root != null && active !== root -> {
                active.destroyCleanly()
                activeWebView = root
            }
            else -> onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(interactionSource = blocker, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CroixGlassCard(cornerRadius = 50.dp, onClick = onDismiss) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close, tint = Color.White)
                }
            }
            Column {
                Text(strings.connectSpotify, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    if (strings.isSpanish) "Inicia sesión para sincronizar tu algoritmo de Spotify" else "Sign in to sync your Spotify taste & playlists",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        // Quick manual sp_dc paste bar (same convenience pattern for power users)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = spDcManual,
                onValueChange = { spDcManual = it },
                placeholder = { Text("sp_dc token (opcional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = spDcManual.trim().isNotBlank(),
                onClick = {
                    val tok = spDcManual.trim()
                    if (tok.isNotBlank() && !captured) {
                        captured = true
                        onCookiesCaptured(tok, "")
                    }
                },
            ) { Text("OK") }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val container = FrameLayout(ctx)
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.removeAllCookies(null)
                    cm.flush()

                    fun tryCapture(url: String?): Boolean {
                        if (captured) return true
                        val cookies = readSpotifyCookies(cm, url)
                        val spDc = cookies["sp_dc"].orEmpty()
                        if (spDc.isBlank()) return false
                        captured = true
                        cm.flush()
                        onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                        return true
                    }

                    val rootView = WebView(ctx).apply {
                        configureAuthWebView(cm)
                        webViewClient = createSpotifyWebViewClient(::tryCapture)
                        webChromeClient = createPopupWebChromeClient(
                            container = container,
                            parentWebView = this,
                            cm = cm,
                            tryCapture = ::tryCapture,
                            onActiveChanged = { activeWebView = it },
                        )
                        mainWebView = this
                        activeWebView = this
                        loadUrl(SpotifyTasteClient.LOGIN_URL)
                    }
                    container.addView(
                        rootView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    container
                },
                update = {
                    if (activeWebView == null) activeWebView = mainWebView
                },
            )
        }
    }
}

enum class YtMusicAuthFlowMode {
    SIGN_IN,
    ADD_ACCOUNT,
    SWITCH_CHANNEL,
}

private const val YTMUSIC_ADD_ACCOUNT_URL =
    "https://accounts.google.com/AddSession?ltmpl=music&service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"

private class CroixYtAuthBridge(
    private val onTokens: (visitorData: String, dataSyncId: String, authUser: String) -> Unit,
) {
    @JavascriptInterface
    fun onAuthMeta(visitorData: String?, dataSyncId: String?, authUser: String?) {
        val v = visitorData?.takeUnless { it == "null" || it == "undefined" }.orEmpty().trim()
        val d = dataSyncId?.takeUnless { it == "null" || it == "undefined" }.orEmpty().trim()
        val a = authUser?.takeUnless { it == "null" || it == "undefined" }.orEmpty().filter(Char::isDigit).ifBlank { "0" }
        onTokens(v, d, a)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "JavascriptInterface")
@Composable
fun YtMusicLoginOverlay(
    mode: YtMusicAuthFlowMode = YtMusicAuthFlowMode.SIGN_IN,
    existingCookie: String = "",
    targetSigninUrl: String = "",
    onDismiss: () -> Unit,
    onAuthCaptured: (cookie: String, visitorData: String, dataSyncId: String, authUser: String) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }
    var onYtMusicDomain by remember { mutableStateOf(false) }
    var manualCookie by remember { mutableStateOf("") }
    var latestVisitor by remember { mutableStateOf("") }
    var latestDataSyncId by remember { mutableStateOf("") }
    var latestAuthUser by remember { mutableStateOf("0") }
    val blocker = remember { MutableInteractionSource() }

    fun collectAndFinish(view: WebView?) {
        if (captured) return
        val cm = CookieManager.getInstance()
        view?.let { v ->
            runCatching {
                v.evaluateJavascript(
                    "(function(){var c=(window.yt&&window.yt.config_)||{};CroixAuth.onAuthMeta(c.VISITOR_DATA||'',c.DATASYNC_ID||'',String(c.SESSION_INDEX||'0'));})();",
                    null,
                )
            }
        }
        cm.flush()
        val rawMusic = cm.getCookie("https://music.youtube.com").orEmpty()
        val rawYt = cm.getCookie("https://www.youtube.com").orEmpty()
        val merged = listOf(rawMusic, rawYt)
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.contains("=") }
            .distinctBy { it.substringBefore("=") }
            .joinToString("; ")
        if (merged.contains("SAPISID=") || merged.contains("__Secure-3PAPISID=")) {
            val trigger = {
                if (!captured) {
                    captured = true
                    onAuthCaptured(merged, latestVisitor, latestDataSyncId, latestAuthUser)
                }
            }
            if (view != null) view.postDelayed(trigger, 480L) else trigger()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.removeJavascriptInterface("CroixAuth")
            webViewRef?.destroyCleanly()
            webViewRef = null
        }
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) webViewRef?.goBack() else onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(interactionSource = blocker, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CroixGlassCard(cornerRadius = 50.dp, onClick = onDismiss) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close, tint = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (mode) {
                        YtMusicAuthFlowMode.ADD_ACCOUNT -> if (strings.isSpanish) "Cambiar / Añadir cuenta" else "Add / Switch Account"
                        YtMusicAuthFlowMode.SWITCH_CHANNEL -> if (strings.isSpanish) "Cambiar perfil / canal" else "Switch Channel / Profile"
                        YtMusicAuthFlowMode.SIGN_IN -> strings.connectYtMusic
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    text = when (mode) {
                        YtMusicAuthFlowMode.SWITCH_CHANNEL ->
                            if (strings.isSpanish) "Toca tu foto arriba a la derecha → Cambiar cuenta y luego pulsa «Listo»"
                            else "Tap your avatar → Switch account, then tap Done"
                        else ->
                            if (strings.isSpanish) "Sincronización automática completa + perfiles y canales"
                            else "Full auto-sync + multi-profile support"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
            if (onYtMusicDomain || mode == YtMusicAuthFlowMode.SWITCH_CHANNEL) {
                Button(onClick = { collectAndFinish(webViewRef) }) {
                    Text(if (strings.isSpanish) "Listo" else "Done")
                }
            }
        }

        if (mode != YtMusicAuthFlowMode.SWITCH_CHANNEL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = manualCookie,
                    onValueChange = { manualCookie = it },
                    placeholder = { Text("Cookie YT Music con SAPISID (opcional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = manualCookie.contains("SAPISID"),
                    onClick = {
                        if (!captured && manualCookie.isNotBlank()) {
                            captured = true
                            onAuthCaptured(manualCookie.trim(), "", "", "0")
                        }
                    },
                ) { Text("OK") }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)

                    if (mode == YtMusicAuthFlowMode.SWITCH_CHANNEL) {
                        // Preserve & inject active account cookies so user can pick their Brand Channel directly!
                        if (existingCookie.isNotBlank()) {
                            existingCookie.split(";").map(String::trim).filter { it.contains("=") }.forEach { pair ->
                                cm.setCookie("https://music.youtube.com", pair)
                                cm.setCookie("https://www.youtube.com", pair)
                            }
                            cm.flush()
                        }
                    } else {
                        // Clear WebView cookies so Google shows a clean account chooser (works for ADD_ACCOUNT & SIGN_IN!)
                        cm.removeAllCookies(null)
                        cm.flush()
                    }

                    val bridge = CroixYtAuthBridge { v, d, a ->
                        if (v.isNotBlank()) latestVisitor = v
                        if (d.isNotBlank()) latestDataSyncId = d
                        if (a.isNotBlank()) latestAuthUser = a
                    }

                    val startUrl = when {
                        targetSigninUrl.isNotBlank() -> {
                            when {
                                targetSigninUrl.startsWith("http") -> targetSigninUrl
                                targetSigninUrl.startsWith("/") -> "https://music.youtube.com$targetSigninUrl"
                                else -> "https://music.youtube.com/$targetSigninUrl"
                            }
                        }
                        mode == YtMusicAuthFlowMode.SWITCH_CHANNEL -> "https://music.youtube.com/"
                        mode == YtMusicAuthFlowMode.ADD_ACCOUNT -> YTMUSIC_ADD_ACCOUNT_URL
                        else -> YTMUSIC_LOGIN_URL
                    }

                    WebView(ctx).apply {
                        configureAuthWebView(cm)
                        addJavascriptInterface(bridge, "CroixAuth")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                val isMusic = url?.contains("music.youtube.com") == true
                                if (isMusic) {
                                    onYtMusicDomain = true
                                    runCatching {
                                        view.evaluateJavascript(
                                            "(function(){var c=(window.yt&&window.yt.config_)||{};CroixAuth.onAuthMeta(c.VISITOR_DATA||'',c.DATASYNC_ID||'',String(c.SESSION_INDEX||'0'));})();",
                                            null,
                                        )
                                    }
                                    // Auto-complete on SIGN_IN / ADD_ACCOUNT or targeted signinUrl redirect;
                                    // on manual SWITCH_CHANNEL let the user pick their profile and press "Listo"!
                                    if (mode != YtMusicAuthFlowMode.SWITCH_CHANNEL || targetSigninUrl.isNotBlank()) {
                                        collectAndFinish(view)
                                    }
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        webViewRef = this
                        loadUrl(startUrl)
                    }
                },
                update = { webViewRef = it },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun WebView.configureAuthWebView(cm: CookieManager) {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = LOGIN_MOBILE_UA
    }
    cm.setAcceptThirdPartyCookies(this, true)
    setOnTouchListener { v, ev ->
        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.parent?.requestDisallowInterceptTouchEvent(false)
        }
        false
    }
}

private fun createSpotifyWebViewClient(tryCapture: (String?) -> Boolean): WebViewClient =
    object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val targetUrl = request.url?.toString()
            if (tryCapture(targetUrl)) return true
            if (targetUrl.isNullOrBlank()) return false
            val scheme = runCatching { Uri.parse(targetUrl).scheme?.lowercase() }.getOrNull()
            if (scheme in setOf("http", "https", "javascript", "data", "blob")) return false
            val fallback = runCatching { Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME) }
                .getOrNull()?.getStringExtra("browser_fallback_url")
            if (!fallback.isNullOrBlank()) view.loadUrl(fallback)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            tryCapture(url)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            tryCapture(url)
        }
    }

private fun createPopupWebChromeClient(
    container: FrameLayout,
    parentWebView: WebView,
    cm: CookieManager,
    tryCapture: (String?) -> Boolean,
    onActiveChanged: (WebView) -> Unit,
): WebChromeClient = object : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val popup = WebView(view.context).apply {
            configureAuthWebView(cm)
            webViewClient = createSpotifyWebViewClient(tryCapture)
            webChromeClient = createPopupWebChromeClient(container, parentWebView, cm, tryCapture, onActiveChanged)
        }
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        container.addView(
            popup,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        popup.bringToFront()
        popup.requestFocus()
        onActiveChanged(popup)
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        window.destroyCleanly()
        onActiveChanged(parentWebView)
    }
}

private fun WebView.destroyCleanly() {
    stopLoading()
    loadUrl("about:blank")
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}

private fun readSpotifyCookies(cm: CookieManager, currentUrl: String?): Map<String, String> {
    val origins = linkedSetOf(
        "https://open.spotify.com",
        "https://accounts.spotify.com",
        "https://spotify.com",
    )
    currentUrl?.let { raw ->
        runCatching {
            val uri = Uri.parse(raw)
            val host = uri.host?.lowercase().orEmpty()
            if (host == "spotify.com" || host.endsWith(".spotify.com")) {
                origins += "https://$host"
            }
        }
    }
    cm.flush()
    val out = linkedMapOf<String, String>()
    origins.forEach { origin ->
        cm.getCookie(origin)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val idx = part.indexOf('=')
                if (idx > 0) {
                    val k = part.substring(0, idx).trim()
                    val v = part.substring(idx + 1).trim()
                    if (k.isNotBlank()) out[k] = v
                }
            }
    }
    return out
}
