package com.catlytics.core.data.mediator

interface DataMediator {
    suspend fun syncLibrary()
}
