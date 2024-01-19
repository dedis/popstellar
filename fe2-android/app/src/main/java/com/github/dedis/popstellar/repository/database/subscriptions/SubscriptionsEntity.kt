package com.github.dedis.popstellar.repository.database.subscriptions

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Channel

@Entity(tableName = "subscriptions")
@Immutable
class SubscriptionsEntity(
    @field:ColumnInfo(name = "lao_id") @field:PrimaryKey val laoId: String,
    @field:ColumnInfo(name = "server_address") val serverAddress: String,
    @field:ColumnInfo(name = "subscription") val subscriptions: Set<Channel>
)
