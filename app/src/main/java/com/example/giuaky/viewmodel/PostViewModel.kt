package com.example.giuaky.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.repository.PostRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class PostUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val post: Post? = null,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val error: String? = null
)

class PostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val post = repository.getPost(postId)
            _uiState.update { it.copy(isLoading = false, post = post, locationName = post?.location ?: "") }
        }
    }

    fun createPost(title: String, content: String, imageUri: Uri?) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Người dùng"
            val post = Post(
                userId = user.uid,
                authorName = authorName,
                title = title,
                content = content,
                location = _uiState.value.locationName,
                latitude = _uiState.value.latitude,
                longitude = _uiState.value.longitude,
                timestamp = System.currentTimeMillis()
            )
            val result = repository.createPost(post, imageUri)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun updatePost(postId: String, title: String, content: String, existingImageUrl: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.updatePost(postId, title, content, existingImageUrl, newImageUri)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun deletePost(postId: String, imageUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deletePost(postId, imageUrl)
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun fetchLocation(context: Context) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModelScope.launch {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val city = addresses?.firstOrNull()?.let { addr ->
                                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "${location.latitude}, ${location.longitude}"
                            } ?: "${location.latitude}, ${location.longitude}"
                            _uiState.update { it.copy(locationName = city, latitude = location.latitude, longitude = location.longitude) }
                        }
                    } else {
                        Toast.makeText(context, "Không thể lấy vị trí. Hãy bật GPS", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (_: SecurityException) {}
    }

    fun clearLocation() = _uiState.update { it.copy(locationName = "", latitude = 0.0, longitude = 0.0) }
    fun clearState() = _uiState.update { PostUiState() }
}
