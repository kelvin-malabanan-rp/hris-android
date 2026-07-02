package io.rocketpartners.hris.feature.assets

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.AssetAssignment

interface AssetRepository {
    /** The signed-in user's currently checked-out assets. Self-scoped from the JWT. */
    suspend fun myAssets(): List<AssetAssignment>
}

class LiveAssetRepository(private val client: ApiClient) : AssetRepository {
    // Bare array (NOT paged) of CHECKED_OUT rows.
    override suspend fun myAssets(): List<AssetAssignment> =
        client.send(Endpoint("asset-assignments/my-assets"))
}
