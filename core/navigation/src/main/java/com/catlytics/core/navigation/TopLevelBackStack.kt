package com.catlytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TopLevelBackStack(
    startKey: NavKey,
) {
    private val topLevelStacks = linkedMapOf(
        startKey to mutableStateListOf(startKey),
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startKey)

    fun addTopLevel(key: NavKey) {
        val stack = topLevelStacks.remove(key) ?: mutableStateListOf(key)
        topLevelStacks[key] = stack
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: NavKey) {
        currentStack().add(key)
        updateBackStack()
    }

    /**
     * Quita las entradas que están sobre [anchor] en la pestaña actual y apila [key]. Lo usa la
     * vista lista-detalle: elegir otro elemento de la lista reemplaza el detalle abierto en vez
     * de acumularlo. Si [anchor] no está en la pestaña, solo apila [key].
     */
    fun replaceAbove(anchor: NavKey, key: NavKey) {
        val stack = currentStack()
        val anchorIndex = stack.lastIndexOf(anchor)
        if (anchorIndex >= 0) {
            while (stack.lastIndex > anchorIndex) {
                stack.removeAt(stack.lastIndex)
            }
        }
        stack.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val currentStack = topLevelStacks[topLevelKey] ?: return

        if (currentStack.size <= 1) {
            if (topLevelStacks.size <= 1) return
            topLevelStacks.remove(topLevelKey)
            topLevelKey = topLevelStacks.keys.last()
            updateBackStack()
            return
        }

        currentStack.removeAt(currentStack.lastIndex)
        updateBackStack()
    }

    internal fun snapshot(): List<List<NavKey>> = topLevelStacks.values.map { it.toList() }

    private fun currentStack(): SnapshotStateList<NavKey> =
        topLevelStacks.getOrPut(topLevelKey) { mutableStateListOf(topLevelKey) }

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStacks.values.flatten())
    }

    internal companion object {
        fun restore(stacks: List<List<NavKey>>): TopLevelBackStack? {
            val validStacks = stacks.filter { it.isNotEmpty() }
            if (validStacks.isEmpty()) return null
            val backStack = TopLevelBackStack(validStacks.first().first())
            backStack.topLevelStacks.clear()
            validStacks.forEach { stack ->
                backStack.topLevelStacks[stack.first()] = mutableStateListOf(*stack.toTypedArray())
            }
            backStack.topLevelKey = backStack.topLevelStacks.keys.last()
            backStack.updateBackStack()
            return backStack
        }
    }
}

/**
 * Crea un [TopLevelBackStack] que sobrevive a cambios de configuración (rotar, plegar o
 * redimensionar la ventana) y a la muerte del proceso. Todas las rutas deben ser `@Serializable`.
 */
@Composable
fun rememberTopLevelBackStack(startKey: NavKey): TopLevelBackStack =
    rememberSerializable(serializer = TopLevelBackStackSerializer(startKey)) {
        TopLevelBackStack(startKey)
    }

internal class TopLevelBackStackSerializer(
    private val startKey: NavKey,
) : KSerializer<TopLevelBackStack> {
    private val delegate = ListSerializer(ListSerializer(NavKeySerializer<NavKey>()))

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: TopLevelBackStack) {
        encoder.encodeSerializableValue(delegate, value.snapshot())
    }

    override fun deserialize(decoder: Decoder): TopLevelBackStack =
        TopLevelBackStack.restore(decoder.decodeSerializableValue(delegate))
            ?: TopLevelBackStack(startKey)
}
