package com.example.data.sdui

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardSduiDao {
    @Query("SELECT * FROM dashboard_layouts WHERE userId = :userId LIMIT 1")
    fun getLayoutForUser(userId: String): Flow<DashboardLayoutEntity?>

    @Query("SELECT * FROM dashboard_layouts WHERE userId = :userId LIMIT 1")
    suspend fun getLayoutForUserSync(userId: String): DashboardLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLayout(layout: DashboardLayoutEntity)

    @Query("DELETE FROM dashboard_layouts WHERE userId = :userId")
    suspend fun deleteLayoutForUser(userId: String)

    @Query("DELETE FROM dashboard_layouts")
    suspend fun clearAllLayouts()
}
