package com.example.subzero.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY nextBillingDate ASC, expiryDate ASC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Int): Asset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset): Long

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Int)
}
