package com.catlytics.feature.library.impl.metadata

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.R as DesignR
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.feature.library.impl.R

enum class BulkEditMode { Tracks, Album }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTracksMetadataSheet(
    trackIds: List<String>,
    onDismiss: () -> Unit,
    mode: BulkEditMode = BulkEditMode.Tracks,
) {
    val viewModel: EditTracksMetadataViewModel = hiltViewModel(key = "edit-tracks-metadata")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val artworkPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onArtworkPicked(it.toString()) } }
    val fileWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onFileWritePermissionResult(result.resultCode == Activity.RESULT_OK) }

    LaunchedEffect(trackIds) { viewModel.open(trackIds) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditTracksMetadataEvent.Message ->
                    Toast.makeText(context, event.text.resolve(resources), Toast.LENGTH_SHORT).show()
                is EditTracksMetadataEvent.RequestFileWrite -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaStore.createWriteRequest(
                            context.contentResolver,
                            event.mediaUris.map(String::toUri),
                        )
                        fileWritePermission.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } else {
                        viewModel.onFileWritePermissionResult(granted = false)
                    }
                }
                EditTracksMetadataEvent.Close -> onDismiss()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (val state = uiState) {
            is EditTracksMetadataUiState.Editing -> EditTracksMetadataContent(
                state = state,
                mode = mode,
                onArtistChange = viewModel::onArtistChange,
                onAlbumChange = viewModel::onAlbumChange,
                onRestoreArtist = viewModel::onRestoreArtist,
                onRestoreAlbum = viewModel::onRestoreAlbum,
                onChooseArtwork = {
                    artworkPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRestoreArtwork = viewModel::onRestoreArtwork,
                onSeparateArtistChange = viewModel::onSeparateArtistChange,
                canWriteToFile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                onWriteToFileChange = viewModel::onWriteToFileChange,
                onDismiss = onDismiss,
                onSave = viewModel::save,
            )
            EditTracksMetadataUiState.NotFound -> Text(
                text = stringResource(R.string.library_bulk_edit_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
            EditTracksMetadataUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(48.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
internal fun EditTracksMetadataContent(
    state: EditTracksMetadataUiState.Editing,
    mode: BulkEditMode,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onRestoreArtist: () -> Unit,
    onRestoreAlbum: () -> Unit,
    onChooseArtwork: () -> Unit,
    onRestoreArtwork: () -> Unit,
    onSeparateArtistChange: (Boolean) -> Unit,
    canWriteToFile: Boolean,
    onWriteToFileChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val mixed = stringResource(R.string.library_bulk_edit_mixed_values)
    val restoring = stringResource(R.string.library_bulk_edit_restoring_field)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = when (mode) {
                BulkEditMode.Tracks ->
                    pluralStringResource(R.plurals.library_bulk_edit_title, state.trackCount, state.trackCount)
                BulkEditMode.Album -> stringResource(R.string.library_album_edit_title)
            },
            style = MaterialTheme.typography.titleLarge,
        )
        if (mode == BulkEditMode.Album) {
            Text(
                text = pluralStringResource(
                    R.plurals.library_album_edit_supporting,
                    state.trackCount,
                    state.trackCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CatlyticsArtwork(
            model = form.artworkPreviewUri,
            seed = form.album.value,
            placeholderIcon = if (mode == BulkEditMode.Album) DesignR.drawable.ic_album else DesignR.drawable.ic_audio,
            contentDescription = stringResource(R.string.library_bulk_edit_artwork_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(144.dp)
                .clip(CatlyticsCorners.Large)
                .align(Alignment.CenterHorizontally),
        )
        if (form.artwork == ArtworkEdit.Restore) {
            Text(
                text = stringResource(R.string.library_bulk_edit_restoring_artwork),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = onChooseArtwork) {
                Text(stringResource(R.string.library_edit_change_artwork))
            }
            if (state.artworkHasEdits && form.artwork != ArtworkEdit.Restore) {
                TextButton(onClick = onRestoreArtwork) {
                    Text(stringResource(R.string.library_bulk_edit_restore_artworks))
                }
            }
        }
        val artistField: @Composable () -> Unit = {
            MetadataTextField(
                value = form.artist.value,
                onValueChange = onArtistChange,
                label = stringResource(R.string.library_edit_field_artist),
                original = null,
                onRestore = onRestoreArtist,
                suggestions = state.artistSuggestions,
                placeholder = mixed.takeIf { state.artistIsMixed },
                supportingText = restoring.takeIf { form.artist.restore },
                canRestore = state.artistHasEdits && !form.artist.restore,
            )
            state.mergedArtistAlias?.let { alias ->
                SeparateArtistRow(
                    alias = alias,
                    checked = form.separateArtist,
                    onCheckedChange = onSeparateArtistChange,
                )
            }
        }
        val albumField: @Composable () -> Unit = {
            MetadataTextField(
                value = form.album.value,
                onValueChange = onAlbumChange,
                label = stringResource(
                    if (mode == BulkEditMode.Album) {
                        R.string.library_album_edit_field_title
                    } else {
                        R.string.library_edit_field_album
                    },
                ),
                original = null,
                onRestore = onRestoreAlbum,
                suggestions = state.albumSuggestions,
                placeholder = mixed.takeIf { state.albumIsMixed },
                supportingText = restoring.takeIf { form.album.restore },
                canRestore = state.albumHasEdits && !form.album.restore,
            )
        }
        if (mode == BulkEditMode.Album) {
            albumField()
            artistField()
        } else {
            artistField()
            albumField()
        }
        if (canWriteToFile) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.library_bulk_edit_write_to_files),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.library_edit_write_to_file_supporting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = form.writeToFile, onCheckedChange = onWriteToFileChange)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_edit_cancel))
            }
            Button(onClick = onSave, enabled = state.canSave) {
                Text(
                    stringResource(
                        if (state.isSaving) R.string.library_edit_saving else R.string.library_edit_save,
                    ),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
