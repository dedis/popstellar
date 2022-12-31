package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.handler.data.*;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.mockito.Mockito;

/** This class helps in the creation of the DataRegistry */
public class DataRegistryModuleHelper {

  public static DataRegistry buildRegistry() {
    return buildRegistry(new LAORepository(), Mockito.mock(KeyManager.class));
  }

  public static DataRegistry buildRegistry(LAORepository laoRepository, KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new RollCallRepository(),
        new MessageRepository(),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, KeyManager keyManager, RollCallRepository rollCallRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        rollCallRepo,
        new MessageRepository(),
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
        rollCallRepo,
        new MessageRepository(),
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
        new RollCallRepository(),
        msgRepo,
        keyManager,
        serverRepo);
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      RollCallRepository rollCallRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    LaoHandler laoHandler = new LaoHandler(keyManager, msgRepo, laoRepo, serverRepo);
    RollCallHandler rollCallHandler = new RollCallHandler(laoRepo, rollCallRepo);
    ElectionHandler electionHandler = new ElectionHandler(msgRepo, laoRepo);
    ConsensusHandler consensusHandler = new ConsensusHandler(laoRepo);
    ChirpHandler chirpHandler = new ChirpHandler(laoRepo, socialMediaRepo);
    TransactionCoinHandler transactionCoinHandler = new TransactionCoinHandler(laoRepo);

    return DataRegistryModule.provideDataRegistry(
        laoHandler,
        rollCallHandler,
        electionHandler,
        consensusHandler,
        chirpHandler,
        transactionCoinHandler);
  }
}
