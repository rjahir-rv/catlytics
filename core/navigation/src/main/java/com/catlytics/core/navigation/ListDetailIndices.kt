package com.catlytics.core.navigation

data class ListDetailIndices(val listIndex: Int, val detailIndex: Int?)

fun <T> findListDetailIndices(
    items: List<T>,
    isList: (T) -> Boolean,
    isDetail: (T) -> Boolean,
): ListDetailIndices? {
    val lastIndex = items.lastIndex
    if (lastIndex < 0) return null
    val last = items[lastIndex]
    if (isList(last)) return ListDetailIndices(listIndex = lastIndex, detailIndex = null)
    if (!isDetail(last)) return null
    for (index in lastIndex - 1 downTo 0) {
        val item = items[index]
        when {
            isList(item) -> return ListDetailIndices(listIndex = index, detailIndex = lastIndex)
            isDetail(item) -> continue
            else -> return null
        }
    }
    return null
}
