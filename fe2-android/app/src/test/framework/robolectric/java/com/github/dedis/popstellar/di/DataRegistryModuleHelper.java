package com.github.dedis.popstellar.di;

import android.content.Context;

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

  public static DataRegistry buildRegistry(Context context) {
    return buildRegistry(
        context,
        new LAORepository(AppDatabaseModuleHelper.getAppDatabase(context)),
        Mockito.mock(KeyManager.class));
  }

  public static DataRegistry buildRegistry(
      Context context, LAORepository laoRepository, KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(),
        new RollCallRepository(),
        new DigitalCashRepository(),
        new MessageRepository(AppDatabaseModuleHelper.getAppDatabase(context)),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      Context context,
      LAORepository laoRepository,
      KeyManager keyManager,
      RollCallRepository rollCallRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(),
        rollCallRepo,
        new DigitalCashRepository(),
        new MessageRepository(AppDatabaseModuleHelper.getAppDatabase(context)),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      Context context,
      LAORepository laoRepository,
      ElectionRepository electionRepo,
      KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        electionRepo,
        new RollCallRepository(),
        new DigitalCashRepository(),
        new MessageRepository(AppDatabaseModuleHelper.getAppDatabase(context)),
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
        new RollCallRepository(),
        new DigitalCashRepository(),
        messageRepo,
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      Context context,
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      RollCallRepository rollCallRepo,
      KeyManager keyManager) {
    return buildRegistry(
        laoRepo,
        socialMediaRepo,
        new ElectionRepository(),
        rollCallRepo,
        new DigitalCashRepository(),
        new MessageRepository(AppDatabaseModuleHelper.getAppDatabase(context)),
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
        new ElectionRepository(),
        new RollCallRepository(),
        new DigitalCashRepository(),
        msgRepo,
        keyManager,
        serverRepo);
  }

  public static DataRegistry buildRegistry(
      Context context, DigitalCashRepository digitalCashRepo, KeyManager keyManager) {
    AppDatabase appDatabase = AppDatabaseModuleHelper.getAppDatabase(context);
    return buildRegistry(
        new LAORepository(appDatabase),
        new SocialMediaRepository(),
        new ElectionRepository(),
        new RollCallRepository(),
        digitalCashRepo,
        new MessageRepository(appDatabase),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      ElectionRepository electionRepo,
      RollCallRepository rollCallRepo,
      DigitalCashRepository digitalCashRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    LaoHandler laoHandler = new LaoHandler(keyManager, msgRepo, laoRepo, serverRepo);
    RollCallHandler rollCallHandler = new RollCallHandler(laoRepo, rollCallRepo, digitalCashRepo);
    ElectionHandler electionHandler = new ElectionHandler(msgRepo, laoRepo, electionRepo);
    ConsensusHandler consensusHandler = new ConsensusHandler(laoRepo);
    ChirpHandler chirpHandler = new ChirpHandler(laoRepo, socialMediaRepo);
    TransactionCoinHandler transactionCoinHandler = new TransactionCoinHandler(digitalCashRepo);

    return DataRegistryModule.provideDataRegistry(
        laoHandler,
        rollCallHandler,
        electionHandler,
        consensusHandler,
        chirpHandler,
        transactionCoinHandler);
  }
}
