package com.github.dedis.popstellar.testutils;

import com.github.dedis.popstellar.repository.remote.MessageSender;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Completable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;

/** This helper class is design to simplify the tests of message sending */
public class MessageSenderHelper {

  private final MessageSender sender = Mockito.mock(MessageSender.class);
  private final Set<Completable> producedCompletable = new HashSet<>();

  /** Returns the mocked MessageSender */
  public MessageSender getMockedSender() {
    return sender;
  }

  /** Setup the mock, this should be called in the setup function of the test */
  public void setupMock() {
    Answer<Completable> answer =
        i -> {
          Completable completable = Completable.complete();
          producedCompletable.add(completable);
          return completable;
        };

    reset(sender);
    lenient().when(sender.publish(any(), any())).thenAnswer(answer);
    lenient().when(sender.publish(any(), any(), any())).thenAnswer(answer);
    lenient().when(sender.subscribe(any())).thenAnswer(answer);
    lenient().when(sender.unsubscribe(any())).thenAnswer(answer);
    lenient().when(sender.catchup(any())).thenAnswer(answer);
  }

  /** Assert that the generated Completables' subscribe() function was called */
  public void assertSubscriptions() {
    producedCompletable.forEach(c -> c.test().assertSubscribed());
  }
}
