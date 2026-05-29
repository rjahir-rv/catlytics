package com.catlytics.core.domain.repository

import com.catlytics.core.model.ListeningStats
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeListeningStats(): Flow<ListeningStats>
}
