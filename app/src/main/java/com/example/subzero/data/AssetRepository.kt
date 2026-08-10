package com.example.subzero.data

import kotlinx.coroutines.flow.Flow

class AssetRepository(private val assetDao: AssetDao) {
    val allAssets: Flow<List<Asset>> = assetDao.getAllAssets()

    suspend fun getAssetById(id: Int): Asset? = assetDao.getAssetById(id)

    suspend fun insertAsset(asset: Asset): Long = assetDao.insertAsset(asset)

    suspend fun updateAsset(asset: Asset) = assetDao.updateAsset(asset)

    suspend fun deleteAsset(asset: Asset) = assetDao.deleteAsset(asset)

    suspend fun deleteAssetById(id: Int) = assetDao.deleteAssetById(id)
}
