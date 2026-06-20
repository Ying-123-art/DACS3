package com.example.giuaky.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.repository.PostRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

    fun createPost(context: Context, title: String, content: String, imageUri: Uri?) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val base64Image = if (imageUri != null) {
                uriToBase64(context, imageUri)
            } else ""

            val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Người dùng"
            val post = Post(
                userId = user.uid,
                authorName = authorName,
                title = title,
                content = content,
                imageUrl = base64Image,
                location = _uiState.value.locationName,
                latitude = _uiState.value.latitude,
                longitude = _uiState.value.longitude,
                timestamp = System.currentTimeMillis()
            )
            val result = repository.createPost(post)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun updatePost(context: Context, postId: String, title: String, content: String, existingImageUrl: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val finalBase64 = if (newImageUri != null) {
                uriToBase64(context, newImageUri)
            } else {
                existingImageUrl
            }

            val result = repository.updatePost(postId, title, content, finalBase64)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    private suspend fun uriToBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            // Step 1: Get dimensions of image to calculate sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            // Step 2: Calculate inSampleSize (Target roughly 800px)
            var inSampleSize = 1
            val targetSize = 800
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                    inSampleSize *= 2
                }
            }

            // Step 3: Decode with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext ""

            // Step 4: Final Resize and Compress
            val scaledBitmap = scaleBitmap(bitmap)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxWidth = 640
        val maxHeight = 640
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) return bitmap
        
        val ratio: Float = width.toFloat() / height.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        
        if (width > height) {
            finalHeight = (maxWidth / ratio).toInt()
        } else {
            finalWidth = (maxHeight * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deletePost(postId)
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
