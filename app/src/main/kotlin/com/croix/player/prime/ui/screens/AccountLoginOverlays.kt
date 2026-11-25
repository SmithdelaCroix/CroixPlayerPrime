/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private const val LOGIN_MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36"

private const val YTMUSIC_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun SpotifyLoginOverlay(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }
    val blocker = remember { MutableInteractionSource() }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
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
                    if (strings.isSpanish) "Inicia sesión para combinar tus gustos al algoritmo" else "Sign in to blend your taste into the algorithm",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.removeAllCookies(null)
                    cm.flush()

                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = LOGIN_MOBILE_UA
                        cm.setAcceptThirdPartyCookies(this, true)

                        setOnTouchListener { v, ev ->
                            when (ev.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                    v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                            false
                        }

                        webViewClient = object : WebViewClient() {
                            private fun checkCookies() {
                                if (captured) return
                                cm.flush()
                                val raw = cm.getCookie("https://open.spotify.com").orEmpty()
                                val map = parseCookieString(raw)
                                val spDc = map["sp_dc"].orEmpty()
                                if (spDc.isNotBlank()) {
                                    captured = true
                                    onCookiesCaptured(spDc, map["sp_key"].orEmpty())
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                checkCookies()
                                return false
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                checkCookies()
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                checkCookies()
                            }
                        }
                        webChromeClient = WebChromeClient()
                        webViewRef = this
                        loadUrl(SpotifyTasteClient.LOGIN_URL)
                    }
                },
                update = { webViewRef = it },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun YtMusicLoginOverlay(
    onDismiss: () -> Unit,
    onCookieCaptured: (cookie: String) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }
    val blocker = remember { MutableInteractionSource() }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CroixGlassCard(cornerRadius = 50.dp, onClick = onDismiss) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close, tint = Color.White)
                }
            }
            Column {
                Text(strings.connectYtMusic, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    if (strings.isSpanish) "Conecta tu cuenta para recomendaciones sin anuncios" else "Connect your account for personalized ad-free feeds",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.removeAllCookies(null)
                    cm.flush()

                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        cm.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            private fun checkCookies(url: String?) {
                                if (captured) return
                                if (url?.contains("music.youtube.com") != true) return
                                cm.flush()
                                val raw = cm.getCookie("https://music.youtube.com").orEmpty()
                                if (raw.contains("SAPISID=") || raw.contains("__Secure-3PAPISID=")) {
                                    captured = true
                                    onCookieCaptured(raw)
                                }
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                checkCookies(url)
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                checkCookies(url)
                            }
                        }
                        webChromeClient = WebChromeClient()
                        webViewRef = this
                        loadUrl(YTMUSIC_LOGIN_URL)
                    }
                },
                update = { webViewRef = it },
            )
        }
    }
}

private fun parseCookieString(raw: String): Map<String, String> =
    raw.split(";").mapNotNull { part ->
        val idx = part.indexOf('=')
        if (idx <= 0) null
        else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
    }.toMap()
