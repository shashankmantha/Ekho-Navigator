package com.ekhonavigator.feature.study

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import androidx.core.net.toUri

private const val STUDY_ROOM_URL = "https://csuci.libcal.com/spaces"

@Composable
fun StudyScreen(
    onViewLibraryOnMap: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primaryToolbarColor = MaterialTheme.colorScheme.primary.toArgb()
    val isCompactWidth = !currentWindowAdaptiveInfo()
        .windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = onViewLibraryOnMap,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = EkhoIcons.Place,
                        contentDescription = "View library on map",
                        modifier = Modifier.size(20.dp),
                    )
                }
                FloatingActionButton(
                    onClick = { openBookingInCustomTab(context, primaryToolbarColor) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = EkhoIcons.OpenInNew,
                        contentDescription = "Book a room",
                    )
                }
            }
        },
    ) { paddingValues ->
        AvailabilityWebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun AvailabilityWebView(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    configureForEmbeddedWidget()
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            injectLayoutTweaks(view)
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
            LoadingOverlay()
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Loading study rooms…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun openBookingInCustomTab(context: Context, toolbarColorArgb: Int) {
    val colorScheme = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(toolbarColorArgb)
        .build()
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setDefaultColorSchemeParams(colorScheme)
        .build()
        .launchUrl(context, STUDY_ROOM_URL.toUri())
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureForEmbeddedWidget() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
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

// LibCal's desktop CSS needs overrides to sit cleanly in a phone viewport.
private fun injectLayoutTweaks(webView: WebView?) {
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
              html, body {
                background: transparent !important;
                min-height: 100% !important;
              }
              body, .container, .container-fluid,
              #s-lc-public-main, #s-lc-public-page-content,
              #col1, #col1 > *, .row, .col-md-12, .col-xs-12 {
                margin: 0 !important;
                padding-left: 0 !important;
                padding-right: 0 !important;
                width: 100% !important;
                max-width: 100% !important;
                float: none !important;
                min-height: 100% !important;
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
              .fc,
              .fc .fc-view-harness,
              .fc .fc-scroller,
              .fc .fc-scroller-liquid-absolute {
                width: 100% !important;
                max-width: 100% !important;
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
                min-height: 32px !important;
                font-size: 12px !important;
              }
              .fc .fc-timeline-slot {
                min-width: 48px !important;
                width: 48px !important;
              }
              /* Extra space for our floating action buttons */
              @media (orientation: portrait) {
                body { padding-bottom: 36px !important; }
              }
              @media (orientation: landscape) {
                body { padding-bottom: 44px !important; }
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
