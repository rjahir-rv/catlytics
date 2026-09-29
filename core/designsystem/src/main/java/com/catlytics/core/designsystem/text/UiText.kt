package com.catlytics.core.designsystem.text

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Texto de UI que puede resolverse a un recurso en el momento de mostrarse.
 *
 * Se usa para mensajes que se originan fuera de un Composable (ViewModels, efectos,
 * repositorios expuestos a la UI): en lugar de construir el texto con `String`, se
 * expone el recurso y sus argumentos, y la capa de presentación lo resuelve con
 * `UiText.asString()` (Compose) o `UiText.resolve(context)` (Toast, estado no Compose).
 */
sealed interface UiText {

    /**
     * Texto simple. [args] se aplican como argumentos de formato posicionales (`%1$s`, `%1$d`).
     */
    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /**
     * Texto con cantidad. [count] selecciona la forma del `<plurals>`.
     *
     * Si [args] está vacío, [count] se usa como único argumento de formato; si el texto
     * necesita más argumentos, deben incluirse todos en [args] (incluido el propio count).
     */
    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}
