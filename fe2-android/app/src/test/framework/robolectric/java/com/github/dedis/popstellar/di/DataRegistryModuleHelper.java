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
        new ElectionRepository(),
        new RollCallRepository(),
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, KeyManager keyManager, RollCallRepository rollCallRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(),
        rollCallRepo,
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, KeyManager keyManager, MeetingRepository meetingRepo) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        new ElectionRepository(),
        new RollCallRepository(),
        meetingRepo,
        new DigitalCashRepository(),
        new MessageRepository(),
        keyManager,
        new ServerRepository());
  }

  public static DataRegistry buildRegistry(
      LAORepository laoRepository, ElectionRepository electionRepo, KeyManager keyManager) {
    return buildRegistry(
        laoRepository,
        new SocialMediaRepository(),
        electionRepo,
        new RollCallRepository(),
        new MeetingRepository(),
        new DigitalCashRepository(),
        new MessageRepository(),
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
        new ElectionRepository(),
        rollCallRepo,
        new MeetingRepository(),
        new DigitalCashRepository(),
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
        new ElectionRepository(),
        new RollCallRepository(),
        new MeetingRepository(),
        new DigitalCashRepository(),
        msgRepo,
        keyManager,
        serverRepo);
  }

  public static DataRegistry buildRegistry(
      DigitalCashRepository digitalCashRepo, KeyManager keyManager) {
    return buildRegistry(
        new LAORepository(),
        new SocialMediaRepository(),
        new ElectionRepository(),
        new RollCallRepository(),
        new MeetingRepository(),
        digitalCashRepo,
        new MessageRepository(),
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

    return DataRegistryModule.provideDataRegistry(
        laoHandler,
        rollCallHandler,
        meetingHandler,
        electionHandler,
        consensusHandler,
        chirpHandler,
        reactionHandler,
        transactionCoinHandler);
  }
}
