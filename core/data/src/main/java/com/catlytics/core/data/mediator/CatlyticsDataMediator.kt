package com.catlytics.core.data.mediator

interface CatlyticsDataMediator {
    suspend fun syncLibrary()
}
