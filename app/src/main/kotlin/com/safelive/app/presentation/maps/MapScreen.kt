package com.safelive.app.presentation.maps

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.safelive.app.domain.model.Incident
import com.safelive.app.navigation.Screen
import com.safelive.app.presentation.dashboard.StatusChip
import com.safelive.app.ui.theme.*
import com.safelive.app.utils.DateUtils
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    val defaultLat = 28.6139
    val defaultLng = 77.2090
    val currentLat = uiState.currentLocation?.latitude ?: defaultLat
    val currentLng = uiState.currentLocation?.longitude ?: defaultLng

    LaunchedEffect(Unit) {
        locationPermission.launchPermissionRequest()
    }

    val incidentsJson = Gson().toJson(uiState.incidents.map {
        mapOf(
            "id" to it.id,
            "title" to it.title,
            "lat" to it.latitude,
            "lng" to it.longitude,
            "priority" to it.priority
        )
    })

    val mapHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { padding: 0; margin: 0; }
                html, body, #map { height: 100%; width: 100vw; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$currentLat, $currentLng], 13);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                var markerLayer = L.layerGroup().addTo(map);
                
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
                        incidents.forEach(function(inc) {
                            if (inc.lat && inc.lng) {
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
                            }
                        });
                    } catch (e) {
                        console.error("Error parsing incidents", e);
                    }
                }
                
                function updateCenter(lat, lng) {
                    map.setView([lat, lng], map.getZoom());
                }

                // Initial load
                updateIncidents('$incidentsJson');
            </script>
        </body>
        </html>
    """.trimIndent()

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
                        if (locationPermission.status.isGranted) {
                            viewModel.loadIncidentsNearLocation(defaultLat, defaultLng)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
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
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMarkerClick(id: String) {
                                val incident = uiState.incidents.find { it.id == id }
                                incident?.let {
                                    viewModel.selectIncident(it)
                                }
                            }
                        }, "Android")
                        loadDataWithBaseURL("https://app.safelive.com/", mapHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    // Instead of reloading the page, execute JS to update markers and center
                    webView.evaluateJavascript("if (typeof updateIncidents === 'function') { updateIncidents('$incidentsJson'); }", null)
                    webView.evaluateJavascript("if (typeof updateCenter === 'function') { updateCenter($currentLat, $currentLng); }", null)
                }
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = PrimaryBlue
                )
            }

            AnimatedVisibility(
                visible = uiState.selectedIncident != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.selectedIncident?.let { incident ->
                    IncidentMapPopup(
                        incident = incident,
                        onDismiss = { viewModel.selectIncident(null) },
                        onViewDetail = {
                            navController.navigate(Screen.IncidentDetail.createRoute(incident.id))
                        }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {}
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(incident.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(incident.category, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(incident.status)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (incident.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(incident.location, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", color = Color.White)
            }
        }
    }
}
