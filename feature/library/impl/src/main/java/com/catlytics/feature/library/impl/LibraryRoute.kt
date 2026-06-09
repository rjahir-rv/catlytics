package com.catlytics.feature.library.impl

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun LibraryRoute(
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permission = requiredAudioPermission()
    var hasAudioPermission by remember(permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refreshLibraryOnce()
        }
    }

    LibraryScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = { permissionLauncher.launch(permission) },
        onFolderVisibilityChange = viewModel::setFolderVisible,
    )
}

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
