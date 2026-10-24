package com.catlytics.feature.settings.impl

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.layout.deferredPadding
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.style.LibraryActionBadges

@Composable
internal fun LicensesSettingsContent(
    @RawRes librariesRes: Int,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val libraries by produceLibraries(librariesRes)
    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize(),
        contentPadding = deferredPadding(
            top = { scaffoldContentPadding.calculateTopPadding() + 8.dp },
            bottom = { bottomPadding() + 80.dp },
        ),
        actionLabels = LibraryActionBadges(
            source = stringResource(R.string.settings_licenses_action_source),
            website = stringResource(R.string.settings_licenses_action_website),
            sponsorEnabled = false,
            viewLicense = stringResource(R.string.settings_licenses_action_view_license),
        ),
        licenseDialogConfirmText = stringResource(R.string.settings_licenses_dialog_confirm),
    )
}
