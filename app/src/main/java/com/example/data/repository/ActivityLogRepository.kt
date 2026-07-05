package com.example.data.repository

import com.example.data.dao.ActivityLogDao
import com.example.data.entity.ActivityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository to abstract transactions for system activity logging audits.
 */
class ActivityLogRepository(private val activityLogDao: ActivityLogDao) {

    /**
     * Exposes a Flow stream containing all user activities.
     */
    val allLogs: Flow<List<ActivityLogEntity>> = activityLogDao.getAllActivityLogs()

    /**
     * Records a pre-built activity log entry.
     */
    suspend fun insertLog(log: ActivityLogEntity) = withContext(Dispatchers.IO) {
        activityLogDao.insertLog(log)
    }

    /**
     * Helper to write a new activity log seamlessly given direct inputs.
     */
    suspend fun logAction(username: String, actionType: String, description: String) = withContext(Dispatchers.IO) {
        val log = ActivityLogEntity(
            username = username,
            actionType = actionType,
            description = description
        )
        activityLogDao.insertLog(log)
    }

    /**
     * Wipes all historic logs from database.
     */
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        activityLogDao.clearLogs()
    }
}
