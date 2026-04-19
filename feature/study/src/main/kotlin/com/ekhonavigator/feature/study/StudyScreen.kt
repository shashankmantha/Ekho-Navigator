package com.ekhonavigator.feature.study

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val STUDY_ROOM_URL = "https://csuci.libcal.com/spaces"

@Composable
fun StudyScreen() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(STUDY_ROOM_URL))
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Click here to book a room")
        }

        Text(
            text = "Live study room availability",
            style = MaterialTheme.typography.titleMedium,
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setupPortraitFriendlySettings()
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                injectPortraitLayoutTweaks(view)
                                isLoading = false
                            }
                        }
                        loadUrl(STUDY_ROOM_URL)
                    }
                },
                update = { webView ->
                    if (webView.url.isNullOrBlank()) {
                        webView.loadUrl(STUDY_ROOM_URL)
                    }
                },
            )

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading study rooms...",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupPortraitFriendlySettings() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.loadsImagesAutomatically = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.setSupportZoom(true)
    settings.textZoom = 95
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = false
    overScrollMode = WebView.OVER_SCROLL_NEVER
}

private fun injectPortraitLayoutTweaks(webView: WebView?) {
    webView?.evaluateJavascript(
        """
        (function() {
          var style = document.getElementById('ekho-study-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'ekho-study-style';
            style.innerHTML = `
              #s-lc-public-cust-header,
              #s-lc-public-bc,
              #s-lc-public-footer,
              .breadcrumb,
              .alert-warning {
                display: none !important;
              }
              body, .container, #s-lc-public-main, #s-lc-public-page-content, #col1 {
                margin: 0 !important;
                padding: 0 !important;
                width: 100% !important;
                max-width: 100% !important;
              }
              .s-lc-spaces-setup-info {
                padding: 8px !important;
              }
              .form-inline {
                display: flex !important;
                flex-direction: column !important;
                align-items: stretch !important;
                gap: 8px !important;
              }
              .form-inline .form-group,
              .form-inline .form-control {
                width: 100% !important;
              }
              .fc .fc-toolbar {
                display: flex !important;
                flex-wrap: wrap !important;
                gap: 6px !important;
              }
              .fc .fc-toolbar-title {
                font-size: 16px !important;
              }
              .fc .fc-scroller,
              .fc .fc-scroller-liquid-absolute {
                overflow-y: auto !important;
              }
              .fc .fc-datagrid-cell-frame,
              .fc .fc-timeline-slot-frame,
              .fc .fc-cell-text {
                min-height: 44px !important;
                font-size: 12px !important;
              }
            `;
            document.head.appendChild(style);
          }

          var viewport = document.querySelector('meta[name="viewport"]');
          if (viewport) {
            viewport.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes');
          }
        })();
        """.trimIndent(),
        null,
    )
}
