package com.github.dedis.popstellar.repository.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.HashEntity
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionEntity
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingEntity
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.lao.LAOEntity
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.message.MessageEntity
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpDao
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpEntity
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionDao
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionEntity
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity
import com.github.dedis.popstellar.repository.database.wallet.WalletDao
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingEntity
import javax.inject.Singleton

@Singleton
@Database(
    entities =
        [
            MessageEntity::class,
            LAOEntity::class,
            WalletEntity::class,
            SubscriptionsEntity::class,
            ElectionEntity::class,
            RollCallEntity::class,
            MeetingEntity::class,
            ChirpEntity::class,
            ReactionEntity::class,
            TransactionEntity::class,
            HashEntity::class,
            WitnessingEntity::class,
            WitnessEntity::class,
            PendingEntity::class],
    version = 4)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun messageDao(): MessageDao

  abstract fun laoDao(): LAODao

  abstract fun walletDao(): WalletDao

  abstract fun subscriptionsDao(): SubscriptionsDao

  abstract fun witnessingDao(): WitnessingDao

  abstract fun witnessDao(): WitnessDao

  abstract fun pendingDao(): PendingDao

  abstract fun electionDao(): ElectionDao

  abstract fun rollCallDao(): RollCallDao

  abstract fun meetingDao(): MeetingDao

  abstract fun chirpDao(): ChirpDao

  abstract fun reactionDao(): ReactionDao

  abstract fun transactionDao(): TransactionDao

  abstract fun hashDao(): HashDao
}
