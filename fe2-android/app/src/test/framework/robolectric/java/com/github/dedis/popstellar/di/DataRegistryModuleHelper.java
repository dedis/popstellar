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
        new MessageRepository(),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo, SocialMediaRepository socialMediaRepo, KeyManager keyManager) {
    return buildRegistry(
        laoRepo, socialMediaRepo, new MessageRepository(), keyManager, new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    return buildRegistry(laoRepo, new SocialMediaRepository(), msgRepo, keyManager, serverRepo);
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepo,
      SocialMediaRepository socialMediaRepo,
      MessageRepository msgRepo,
      KeyManager keyManager,
      ServerRepository serverRepo) {
    LaoHandler laoHandler = new LaoHandler(keyManager, msgRepo, laoRepo, serverRepo);
    RollCallHandler rollCallHandler = new RollCallHandler(keyManager, laoRepo);
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
