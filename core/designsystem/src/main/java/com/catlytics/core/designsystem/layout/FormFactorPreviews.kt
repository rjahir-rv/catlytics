package com.catlytics.core.designsystem.layout

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Teléfono", device = Devices.PHONE, showBackground = true)
@Preview(name = "Plegable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Escritorio", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews
