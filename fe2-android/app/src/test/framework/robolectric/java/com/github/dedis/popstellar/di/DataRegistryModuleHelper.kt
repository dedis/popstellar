package com.github.dedis.popstellar.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.ServerRepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.utility.handler.data.ChirpHandler
import com.github.dedis.popstellar.utility.handler.data.ConsensusHandler
import com.github.dedis.popstellar.utility.handler.data.ElectionHandler
import com.github.dedis.popstellar.utility.handler.data.LaoHandler
import com.github.dedis.popstellar.utility.handler.data.MeetingHandler
import com.github.dedis.popstellar.utility.handler.data.ReactionHandler
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler
import com.github.dedis.popstellar.utility.handler.data.TransactionCoinHandler
import com.github.dedis.popstellar.utility.handler.data.WitnessingHandler
import com.github.dedis.popstellar.utility.security.KeyManager
import javax.inject.Singleton
import org.mockito.Mockito

/** This class helps in the creation of the DataRegistry */
@Singleton
object DataRegistryModuleHelper {
  private val application = ApplicationProvider.getApplicationContext<Application>()
  private val appDatabase = AppDatabaseModuleHelper.getAppDatabase(application)

  init {
    appDatabase.close()
  }

  @JvmStatic
  @JvmOverloads
  fun buildRegistry(
    laoRepository: LAORepository = LAORepository(appDatabase, application),
    keyManager: KeyManager = Mockito.mock(KeyManager::class.java),
    consensusRepository: ConsensusRepository = ConsensusRepository()
  ): DataRegistry {
    val rollCallRepository = RollCallRepository(appDatabase, application)
    val electionRepository = ElectionRepository(appDatabase, application)
    val meetingRepository = MeetingRepository(appDatabase, application)
    val digitalCashRepository = DigitalCashRepository(appDatabase, application)
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      electionRepository,
      rollCallRepository,
      meetingRepository,
      digitalCashRepository,
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepository,
        electionRepository,
        meetingRepository,
        digitalCashRepository
      ),
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      consensusRepository
    )
  }

  @JvmStatic
  fun buildRegistry(
    laoRepository: LAORepository,
    witnessingRepository: WitnessingRepository,
    keyManager: KeyManager
  ): DataRegistry {
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      ElectionRepository(appDatabase, application),
      RollCallRepository(appDatabase, application),
      MeetingRepository(appDatabase, application),
      DigitalCashRepository(appDatabase, application),
      witnessingRepository,
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  @JvmStatic
  fun buildRegistry(
    laoRepository: LAORepository,
    keyManager: KeyManager,
    rollCallRepo: RollCallRepository,
    witnessingRepo: WitnessingRepository
  ): DataRegistry {
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      ElectionRepository(appDatabase, application),
      rollCallRepo,
      MeetingRepository(appDatabase, application),
      DigitalCashRepository(appDatabase, application),
      witnessingRepo,
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  @JvmStatic
  fun buildRegistry(
    laoRepository: LAORepository,
    keyManager: KeyManager,
    meetingRepo: MeetingRepository,
    witnessingRepo: WitnessingRepository
  ): DataRegistry {
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      ElectionRepository(appDatabase, application),
      RollCallRepository(appDatabase, application),
      meetingRepo,
      DigitalCashRepository(appDatabase, application),
      witnessingRepo,
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  fun buildRegistry(
    laoRepository: LAORepository,
    electionRepo: ElectionRepository,
    keyManager: KeyManager
  ): DataRegistry {
    val rollCallRepository = RollCallRepository(appDatabase, application)
    val meetingRepository = MeetingRepository(appDatabase, application)
    val digitalCashRepository = DigitalCashRepository(appDatabase, application)
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      electionRepo,
      rollCallRepository,
      meetingRepository,
      digitalCashRepository,
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepository,
        electionRepo,
        meetingRepository,
        digitalCashRepository
      ),
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  @JvmStatic
  fun buildRegistry(
    laoRepository: LAORepository,
    electionRepo: ElectionRepository,
    witnessingRepo: WitnessingRepository,
    keyManager: KeyManager,
    messageRepo: MessageRepository
  ): DataRegistry {
    return buildRegistry(
      laoRepository,
      SocialMediaRepository(appDatabase, application),
      electionRepo,
      RollCallRepository(appDatabase, application),
      MeetingRepository(appDatabase, application),
      DigitalCashRepository(appDatabase, application),
      witnessingRepo,
      messageRepo,
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  fun buildRegistry(
    laoRepo: LAORepository,
    socialMediaRepo: SocialMediaRepository,
    rollCallRepo: RollCallRepository,
    keyManager: KeyManager
  ): DataRegistry {
    val electionRepository = ElectionRepository(appDatabase, application)
    val meetingRepository = MeetingRepository(appDatabase, application)
    val digitalCashRepository = DigitalCashRepository(appDatabase, application)
    return buildRegistry(
      laoRepo,
      socialMediaRepo,
      electionRepository,
      rollCallRepo,
      meetingRepository,
      digitalCashRepository,
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepo,
        electionRepository,
        meetingRepository,
        digitalCashRepository
      ),
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  @JvmStatic
  fun buildRegistry(
    laoRepo: LAORepository,
    witnessingRepo: WitnessingRepository,
    msgRepo: MessageRepository,
    keyManager: KeyManager,
    serverRepo: ServerRepository
  ): DataRegistry {
    return buildRegistry(
      laoRepo,
      SocialMediaRepository(appDatabase, application),
      ElectionRepository(appDatabase, application),
      RollCallRepository(appDatabase, application),
      MeetingRepository(appDatabase, application),
      DigitalCashRepository(appDatabase, application),
      witnessingRepo,
      msgRepo,
      keyManager,
      serverRepo,
      ConsensusRepository()
    )
  }

  @JvmStatic
  fun buildRegistry(digitalCashRepo: DigitalCashRepository, keyManager: KeyManager): DataRegistry {
    val rollCallRepository = RollCallRepository(appDatabase, application)
    val electionRepository = ElectionRepository(appDatabase, application)
    val meetingRepository = MeetingRepository(appDatabase, application)
    return buildRegistry(
      LAORepository(appDatabase, application),
      SocialMediaRepository(appDatabase, application),
      electionRepository,
      rollCallRepository,
      meetingRepository,
      digitalCashRepo,
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepository,
        electionRepository,
        meetingRepository,
        digitalCashRepo
      ),
      MessageRepository(appDatabase, application),
      keyManager,
      ServerRepository(),
      ConsensusRepository()
    )
  }

  fun buildRegistry(
    laoRepo: LAORepository,
    socialMediaRepo: SocialMediaRepository,
    electionRepo: ElectionRepository,
    rollCallRepo: RollCallRepository,
    meetingRepo: MeetingRepository,
    digitalCashRepo: DigitalCashRepository,
    witnessingRepo: WitnessingRepository,
    msgRepo: MessageRepository,
    keyManager: KeyManager,
    serverRepo: ServerRepository,
    consensusRepo: ConsensusRepository
  ): DataRegistry {
    val laoHandler =
      LaoHandler(keyManager, msgRepo, laoRepo, serverRepo, witnessingRepo, consensusRepo)
    val rollCallHandler = RollCallHandler(laoRepo, rollCallRepo, digitalCashRepo, witnessingRepo)
    val meetingHandler = MeetingHandler(laoRepo, meetingRepo, witnessingRepo)
    val electionHandler = ElectionHandler(msgRepo, laoRepo, electionRepo, witnessingRepo)
    val consensusHandler = ConsensusHandler(laoRepo, witnessingRepo, consensusRepo)
    val chirpHandler = ChirpHandler(laoRepo, socialMediaRepo)
    val reactionHandler = ReactionHandler(laoRepo, socialMediaRepo)
    val transactionCoinHandler = TransactionCoinHandler(digitalCashRepo)
    val witnessingHandler = WitnessingHandler(laoRepo, witnessingRepo)
    return DataRegistryModule.provideDataRegistry(
      laoHandler,
      rollCallHandler,
      meetingHandler,
      electionHandler,
      consensusHandler,
      chirpHandler,
      reactionHandler,
      transactionCoinHandler,
      witnessingHandler
    )
  }
}
