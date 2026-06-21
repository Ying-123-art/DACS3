package com.example.giuaky.ui.screen

import androidx.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.giuaky.viewmodel.HomeViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postsWithLocation = uiState.posts.filter { it.latitude != 0.0 && it.longitude != 0.0 }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bản Đồ Hành Trình") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Info bar
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (uiState.mapFocusPoint != null) "📍 Đang hiển thị vị trí được chọn" 
                        else "📍 ${postsWithLocation.size} địa điểm được ghi lại",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(6.0)
                        controller.setCenter(GeoPoint(16.0, 108.0))
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    
                    postsWithLocation.forEach { post ->
                        // Lấy thông tin user mới nhất để hiển thị tên đúng
                        val latestUser = uiState.users[post.userId]
                        val displayName = latestUser?.displayName ?: post.authorName

                        val marker = Marker(mapView).apply {
                            position = GeoPoint(post.latitude, post.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = post.title.ifEmpty { displayName }
                            snippet = "Người đăng: $displayName\nĐịa điểm: ${post.location}"
                        }
                        mapView.overlays.add(marker)
                    }

                    uiState.mapFocusPoint?.let { (lat, lon) ->
                        val focusPoint = GeoPoint(lat, lon)
                        mapView.controller.animateTo(focusPoint)
                        mapView.controller.setZoom(15.0)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
