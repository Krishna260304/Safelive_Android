package com.safelive.app.presentation.maps

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.gson.Gson
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.model.Ticket
import com.safelive.app.navigation.Screen
import com.safelive.app.presentation.dashboard.StatusChip
import com.safelive.app.ui.theme.DangerRed
import com.safelive.app.ui.theme.SuccessGreen
import com.safelive.app.ui.theme.WarningOrange
import org.json.JSONObject
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gson = remember { Gson() }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val defaultLat = 20.5937
    val defaultLng = 78.9629
    val currentLat = uiState.currentLocation?.latitude ?: defaultLat
    val currentLng = uiState.currentLocation?.longitude ?: defaultLng

    val incidentsJson = remember(uiState.incidents, uiState.tickets) {
        gson.toJson(
            uiState.incidents.map {
                mapOf(
                    "id" to it.id,
                    "title" to it.title,
                    "lat" to it.latitude,
                    "lng" to it.longitude,
                    "priority" to it.priority
                )
            } + uiState.tickets.map {
                mapOf(
                    "id" to "ticket:${it.id}",
                    "title" to it.title,
                    "description" to it.description,
                    "lat" to it.latitude,
                    "lng" to it.longitude,
                    "priority" to it.priority,
                    "type" to "ticket"
                )
            }
        )
    }

    val heatmapJson = remember(uiState.heatmap) { gson.toJson(uiState.heatmap.map { mapOf("lat" to it.latitude, "lng" to it.longitude, "weight" to it.weight) }) }

    val mapHtml = remember(currentLat, currentLng, incidentsJson, heatmapJson) {
        buildMapHtml(currentLat, currentLng, incidentsJson, heatmapJson)
    }

    LaunchedEffect(incidentsJson, heatmapJson, currentLat, currentLng, webView) {
        webView?.let { map ->
            map.evaluateJavascript(
                "if (typeof updateIncidents === 'function') { updateIncidents(${JSONObject.quote(incidentsJson)}); }",
                null
            )
            map.evaluateJavascript(
                "if (typeof updateCenter === 'function') { updateCenter($currentLat, $currentLng); }",
                null
            )
            map.evaluateJavascript(
                "if (typeof updateHeatmap === 'function') { updateHeatmap(${JSONObject.quote(heatmapJson)}); }",
                null
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Map", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadIncidents()
                    }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                view.evaluateJavascript(
                                    "if (typeof updateIncidents === 'function') { updateIncidents(${JSONObject.quote(incidentsJson)}); }",
                                    null
                                )
                                view.evaluateJavascript(
                                    "if (typeof updateCenter === 'function') { updateCenter($currentLat, $currentLng); }",
                                    null
                                )
                                view.evaluateJavascript(
                                    "if (typeof updateHeatmap === 'function') { updateHeatmap(${JSONObject.quote(heatmapJson)}); }",
                                    null
                                )
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                Timber.w("Leaflet: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                                return true
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMarkerClick(id: String) {
                                if (id.startsWith("ticket:")) {
                                    val ticket = viewModel.uiState.value.tickets.find { it.id == id.removePrefix("ticket:") }
                                    ticket?.let(viewModel::selectTicket)
                                } else {
                                    val incident = viewModel.uiState.value.incidents.find { it.id == id }
                                    incident?.let(viewModel::selectIncident)
                                }
                            }
                        }, "Android")
                        loadDataWithBaseURL("https://app.safelive.com/", mapHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView = it }
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = uiState.selectedIncident != null || uiState.selectedTicket != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.selectedIncident?.let { incident ->
                    IncidentMapPopup(incident = incident, onDismiss = { viewModel.selectIncident(null) }, onViewDetail = {
                        navController.navigate(Screen.IncidentDetail.createRoute(incident.id))
                    })
                } ?: uiState.selectedTicket?.let { ticket ->
                    TicketMapPopup(ticket = ticket, onDismiss = { viewModel.selectTicket(null) }, onViewDetail = {
                        navController.navigate(Screen.TicketDetail.createRoute(ticket.id))
                    })
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LegendItem("Critical", DangerRed)
                    LegendItem("High", WarningOrange)
                    LegendItem("Medium", Color(0xFFFFB300))
                    LegendItem("Low", SuccessGreen)
                }
            }
        }
    }
}

private fun buildMapHtml(currentLat: Double, currentLng: Double, initialIncidentsJson: String, initialHeatmapJson: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/leaflet.heat@0.2.0/dist/leaflet-heat.js"></script>
            <style>
                body { padding: 0; margin: 0; }
                html, body, #map { height: 100%; width: 100vw; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: true }).setView([$currentLat, $currentLng], 5);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: 'Â© OpenStreetMap'
                }).addTo(map);

                var markerLayer = L.layerGroup().addTo(map);
                var heatLayer = null;
                
                function getColor(priority) {
                    if (!priority) return 'green';
                    var p = priority.toLowerCase();
                    if (p === 'critical') return 'red';
                    if (p === 'high') return 'orange';
                    if (p === 'medium') return 'gold';
                    return 'green';
                }

                function updateIncidents(jsonString) {
                    try {
                        var incidents = JSON.parse(jsonString);
                        markerLayer.clearLayers();
                        var bounds = [];
                        incidents.forEach(function(inc) {
                            if (typeof inc.lat === 'number' && typeof inc.lng === 'number') {
                                var markerHtml = `<div style="background-color: ${'$'}{getColor(inc.priority)}; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white;"></div>`;
                                var icon = L.divIcon({
                                    html: markerHtml,
                                    className: '',
                                    iconSize: [24, 24],
                                    iconAnchor: [12, 12]
                                });
                                
                                var marker = L.marker([inc.lat, inc.lng], {icon: icon});
                                marker.on('click', function() {
                                    if (window.Android) {
                                        window.Android.onMarkerClick(inc.id);
                                    }
                                });
                                markerLayer.addLayer(marker);
                                bounds.push([inc.lat, inc.lng]);
                            }
                        });
                        if (bounds.length > 0) {
                            map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
                        }
                    } catch (e) {
                        console.error("Error parsing incidents", e);
                    }
                }
                
                function updateCenter(lat, lng) {
                    map.setView([lat, lng], map.getZoom());
                }

                function updateHeatmap(jsonString) {
                    try {
                        if (heatLayer) {
                            map.removeLayer(heatLayer);
                            heatLayer = null;
                        }
                        var points = JSON.parse(jsonString || '[]');
                        if (points.length > 0 && L.heatLayer) {
                            heatLayer = L.heatLayer(points.map(function(p) { return [p.lat, p.lng, p.weight || 1]; }), {
                                radius: 25, blur: 15, maxZoom: 17
                            }).addTo(map);
                        }
                    } catch (e) { console.error('Error parsing heatmap', e); }
                }

                updateIncidents(${JSONObject.quote(initialIncidentsJson)});
                updateHeatmap(${JSONObject.quote(initialHeatmapJson)});
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun LegendItem(label: String, color: Color) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun IncidentMapPopup(
    incident: Incident,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(incident.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        incident.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(incident.status)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (incident.location.isNotBlank()) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        incident.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", color = Color.White)
            }
        }
    }
}

@Composable
private fun TicketMapPopup(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(ticket.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Ticket ${ticket.ticketId.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(ticket.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", color = Color.White)
            }
        }
    }
}
