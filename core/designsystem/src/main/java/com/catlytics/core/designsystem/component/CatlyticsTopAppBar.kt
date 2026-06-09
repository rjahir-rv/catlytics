package com.catlytics.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatlyticsTopAppBar(
    title: String,
    @DrawableRes navigationIconRes: Int,
    navigationIconContentDescription: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    painter = painterResource(navigationIconRes),
                    contentDescription = navigationIconContentDescription,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun CatlyticsTopAppBarPreview() {
    CatlyticsTheme {
        CatlyticsTopAppBar(
            title = "Inicio",
            navigationIconRes = R.drawable.ic_settings,
            navigationIconContentDescription = "Abrir ajustes",
            onNavigationClick = {},
        )
    }
}
