package common.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;

public class MessageQueueTest {

  private static final String MESSAGE = "msg";

  @Test
  public void listenBeforeReceiveWork() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(0);

    new Thread(() -> {
      Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
      lock.release();
    }).start();

    queue.onNewMsg(MESSAGE);
    lock.acquire();
  }

  @Test
  public void multipleListenBeforeReceiveWork() throws InterruptedException {
    int n = 10;
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(1 - n);

    for (int i = 0; i < n; i++) {
      new Thread(() -> {
        Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
        lock.release();
      }).start();
    }

    for (int i = 0; i < n; i++)
      queue.onNewMsg(MESSAGE);

    lock.acquire();
  }

  @Test
  public void noMessageAfterTimeoutReturnsNull() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(0);

    new Thread(() -> {
      Assertions.assertNull(queue.takeTimeout(500));
      lock.release();
    }).start();

    lock.acquire();
  }
}
