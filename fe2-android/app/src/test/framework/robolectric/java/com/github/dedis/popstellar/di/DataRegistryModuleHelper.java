package com.github.dedis.popstellar.di;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.utility.handler.data.*;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.mockito.Mockito;

import javax.inject.Singleton;

/** This class helps in the creation of the DataRegistry */
@Singleton
public class DataRegistryModuleHelper {

  private static final Application application = ApplicationProvider.getApplicationContext();
  private static final AppDatabase appDatabase =
      AppDatabaseModuleHelper.getAppDatabase(application);

  static {
    appDatabase.close();
  }

  public static DataRegistry buildRegistry() {
    return buildRegistry(
        new LAORepository(appDatabase, application), Mockito.mock(KeyManager.class));
  }

  public static DataRegistry buildRegistry(LAORepository laoRepository, KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(appDatabase, application),
        new RollCallRepository(appDatabase, application),
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, KeyManager keyManager, RollCallRepository rollCallRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(appDatabase, application),
        rollCallRepo,
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, KeyManager keyManager, MeetingRepository meetingRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(appDatabase, application),
        new RollCallRepository(appDatabase, application),
        meetingRepo,
        new DigitalCashRepository(),
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, ElectionRepository electionRepo, KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        electionRepo,
        new RollCallRepository(appDatabase, application),
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository,
      ElectionRepository electionRepo,
      KeyManager keyManager,
      MessageRepository messageRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        electionRepo,
        new RollCallRepository(appDatabase, application),
        new MeetingRepository(),
        new DigitalCashRepository(),
        messageRepo,
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      RollCallRepository rollCallRepo,
      KeyManager keyManager) {
    return buildRegistry(
        laoRepo,
        socialMediaRepo,
        new ElectionRepository(appDatabase, application),
        rollCallRepo,
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    return buildRegistry(
        laoRepo,
        new SocialMediaRepository(),
        new ElectionRepository(appDatabase, application),
        new RollCallRepository(appDatabase, application),
        new MeetingRepository(),
        new DigitalCashRepository(),
        msgRepo,
        keyManager,
        serverRepo);
  }

  public static DataRegistry buildRegistry(
      DigitalCashRepository digitalCashRepo, KeyManager keyManager) {
    return buildRegistry(
        new LAORepository(appDatabase, application),
        new SocialMediaRepository(),
        new ElectionRepository(appDatabase, application),
        new RollCallRepository(appDatabase, application),
        new MeetingRepository(),
        digitalCashRepo,
        new MessageRepository(appDatabase, application),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      ElectionRepository electionRepo,
      RollCallRepository rollCallRepo,
      MeetingRepository meetingRepo,
      DigitalCashRepository digitalCashRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    LaoHandler laoHandler = new LaoHandler(keyManager, msgRepo, laoRepo, serverRepo);
    RollCallHandler rollCallHandler = new RollCallHandler(laoRepo, rollCallRepo, digitalCashRepo);
    MeetingHandler meetingHandler = new MeetingHandler(laoRepo, meetingRepo);
    ElectionHandler electionHandler = new ElectionHandler(msgRepo, laoRepo, electionRepo);
    ConsensusHandler consensusHandler = new ConsensusHandler(laoRepo);
    ChirpHandler chirpHandler = new ChirpHandler(laoRepo, socialMediaRepo);
    ReactionHandler reactionHandler = new ReactionHandler(laoRepo, socialMediaRepo);
    TransactionCoinHandler transactionCoinHandler = new TransactionCoinHandler(digitalCashRepo);
    WitnessingHandler witnessingHandler = new WitnessingHandler(laoRepo);

    return DataRegistryModule.provideDataRegistry(
        laoHandler,
        rollCallHandler,
        meetingHandler,
        electionHandler,
        consensusHandler,
        chirpHandler,
        reactionHandler,
        transactionCoinHandler,
        witnessingHandler);
  }
}
