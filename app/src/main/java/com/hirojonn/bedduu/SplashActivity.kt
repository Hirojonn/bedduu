package com.hirojonn.bedduu

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.rgb(17, 17, 17)
        window.navigationBarColor = AndroidColor.rgb(17, 17, 17)
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            ElfSplashAnimation(
                onAnimationLoaded = {
                    handler.postDelayed(::openMainActivity, 3_000)
                },
                onFailure = { error ->
                    Log.e("SplashActivity", "Could not load 2secs.svg", error)
                    openMainActivity()
                }
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun openMainActivity() {
        if (hasNavigated || isFinishing) return
        hasNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
private fun ElfSplashAnimation(
    onAnimationLoaded: () -> Unit,
    onFailure: (Throwable) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val svgDocument = remember {
        context.assets.open("2secs.svg")
            .bufferedReader()
            .use { it.readText() }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111)),
        factory = { viewContext ->
            WebView(viewContext).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                setBackgroundColor(AndroidColor.rgb(17, 17, 17))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                var hasReportedLoad = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view != null && !hasReportedLoad) {
                            hasReportedLoad = true
                            onAnimationLoaded()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onFailure(IllegalStateException(error?.description?.toString()))
                        }
                    }
                }
                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                      <style>
                        html, body {
                          width: 100%;
                          height: 100%;
                          margin: 0;
                          background: #111111;
                          overflow: hidden;
                        }
                        body {
                          display: grid;
                          place-items: center;
                        }
                        svg {
                          width: min(90vmin, 512px);
                          height: min(90vmin, 512px);
                        }
                      </style>
                    </head>
                    <body>$svgDocument</body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}
