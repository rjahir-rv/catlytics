package com.catlytics.feature.library.impl.metadata

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.R as DesignR
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.component.CollapsibleHint
import com.catlytics.core.designsystem.component.InfoToggleButton
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.ArtistAlias
import com.catlytics.feature.library.impl.R

/** Bottom sheet that edits the title, artist, album and artwork of one track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrackMetadataSheet(
    trackId: String,
    onDismiss: () -> Unit,
) {
    val viewModel: EditTrackMetadataViewModel = hiltViewModel(key = "edit-track-metadata")
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

    LaunchedEffect(trackId) { viewModel.open(trackId) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditTrackMetadataEvent.Message ->
                    Toast.makeText(context, event.text.resolve(resources), Toast.LENGTH_SHORT).show()
                is EditTrackMetadataEvent.RequestFileWrite -> {
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
                EditTrackMetadataEvent.Close -> onDismiss()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (val state = uiState) {
            is EditTrackMetadataUiState.Editing -> EditTrackMetadataContent(
                state = state,
                canWriteToFile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                onTitleChange = viewModel::onTitleChange,
                onArtistChange = viewModel::onArtistChange,
                onAlbumChange = viewModel::onAlbumChange,
                onRestoreField = viewModel::restoreField,
                onUnifyArtistChange = viewModel::onUnifyArtistChange,
                onApplyArtworkToAlbumChange = viewModel::onApplyArtworkToAlbumChange,
                onSeparateArtistChange = viewModel::onSeparateArtistChange,
                onWriteToFileChange = viewModel::onWriteToFileChange,
                onChooseArtwork = {
                    artworkPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRestoreArtwork = viewModel::onRestoreArtwork,
                onResetAll = viewModel::resetAll,
                onDismiss = onDismiss,
                onSave = viewModel::save,
            )
            EditTrackMetadataUiState.NotFound -> Text(
                text = stringResource(R.string.library_edit_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
            EditTrackMetadataUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(48.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
internal fun EditTrackMetadataContent(
    state: EditTrackMetadataUiState.Editing,
    canWriteToFile: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onRestoreField: (MetadataField) -> Unit,
    onUnifyArtistChange: (Boolean) -> Unit,
    onApplyArtworkToAlbumChange: (Boolean) -> Unit,
    onSeparateArtistChange: (Boolean) -> Unit,
    onWriteToFileChange: (Boolean) -> Unit,
    onChooseArtwork: () -> Unit,
    onRestoreArtwork: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val original = state.original
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.library_edit_title),
            style = MaterialTheme.typography.titleLarge,
        )
        CatlyticsArtwork(
            model = form.artworkPreviewUri,
            seed = form.title,
            placeholderIcon = DesignR.drawable.ic_audio,
            contentDescription = stringResource(R.string.library_edit_artwork_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(144.dp)
                .clip(CatlyticsCorners.Large)
                .align(Alignment.CenterHorizontally),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = onChooseArtwork) {
                Text(stringResource(R.string.library_edit_change_artwork))
            }
            if (state.hasArtworkEdit) {
                TextButton(onClick = onRestoreArtwork) {
                    Text(stringResource(R.string.library_edit_restore_artwork))
                }
            }
        }
        if (state.canApplyArtworkToAlbum) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CatlyticsCorners.Medium)
                    .clickable { onApplyArtworkToAlbumChange(!form.applyArtworkToAlbum) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = form.applyArtworkToAlbum, onCheckedChange = onApplyArtworkToAlbumChange)
                Text(
                    text = pluralStringResource(
                        R.plurals.library_edit_apply_artwork_to_album,
                        state.albumTrackIds.size,
                        state.albumTrackIds.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        MetadataTextField(
            value = form.title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.library_edit_field_title),
            original = original.title,
            onRestore = { onRestoreField(MetadataField.Title) },
            isError = form.title.isBlank(),
            errorText = stringResource(R.string.library_edit_title_required),
        )
        MetadataTextField(
            value = form.artistName,
            onValueChange = onArtistChange,
            label = stringResource(R.string.library_edit_field_artist),
            original = original.artist.name,
            onRestore = { onRestoreField(MetadataField.Artist) },
            suggestions = state.artistSuggestions,
        )
        state.unifyTarget?.let { target ->
            CheckboxWithHint(
                checked = form.unifyArtist,
                onCheckedChange = onUnifyArtistChange,
                label = stringResource(R.string.library_edit_unify_artist, target.name),
                hint = stringResource(R.string.library_edit_unify_artist_hint, original.artist.name, target.name),
            )
        }
        state.mergedArtistAlias?.let { alias ->
            SeparateArtistRow(
                alias = alias,
                checked = form.separateArtist,
                onCheckedChange = onSeparateArtistChange,
            )
        }
        MetadataTextField(
            value = form.albumTitle,
            onValueChange = onAlbumChange,
            label = stringResource(R.string.library_edit_field_album),
            original = original.albumTitle.orEmpty(),
            onRestore = { onRestoreField(MetadataField.Album) },
            suggestions = state.albumSuggestions,
        )
        if (canWriteToFile) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.library_edit_write_to_file),
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
            if (state.hasOverride) {
                TextButton(onClick = onResetAll, enabled = !state.isSaving) {
                    Text(stringResource(R.string.library_edit_reset_all))
                }
                Spacer(Modifier.weight(1f))
            }
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

@Composable
internal fun SeparateArtistRow(
    alias: ArtistAlias,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CheckboxWithHint(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = stringResource(R.string.library_edit_separate_artist, alias.target.name),
        hint = stringResource(R.string.library_edit_separate_artist_hint, alias.source.name, alias.target.name),
        modifier = modifier,
    )
}

@Composable
internal fun CheckboxWithHint(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    var showHint by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CatlyticsCorners.Medium)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            InfoToggleButton(expanded = showHint, onExpandedChange = { showHint = it })
        }
        CollapsibleHint(visible = showHint, text = hint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetadataTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    original: String?,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    isError: Boolean = false,
    errorText: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    canRestore: Boolean = original != null && value.trim() != original,
) {
    var expanded by remember { mutableStateOf(false) }
    val isEdited = original != null && value.trim() != original
    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            supportingText = when {
                isError && errorText != null -> {
                    { Text(errorText) }
                }
                supportingText != null -> {
                    { Text(supportingText) }
                }
                isEdited -> {
                    { Text(stringResource(R.string.library_edit_original_value, original.orEmpty().ifEmpty { "—" })) }
                }
                else -> null
            },
            trailingIcon = if (canRestore) {
                {
                    IconButton(onClick = onRestore) {
                        Icon(
                            painter = painterResource(DesignR.drawable.ic_close),
                            contentDescription = stringResource(R.string.library_edit_restore_field, label),
                        )
                    }
                }
            } else {
                null
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
