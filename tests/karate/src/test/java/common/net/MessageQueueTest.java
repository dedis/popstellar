package common.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MessageQueueTest {

  private static final String MESSAGE = "msg";
  private static final int TEST_SAMPLE = 1000;
  private static final Random random = new Random(0);

  @Test
  public void listenBeforeReceiveWork() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(0);

    new Thread(
            () -> {
              Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
              lock.release();
            })
        .start();

    queue.onNewMsg(MESSAGE);
    Assertions.assertTrue(lock.tryAcquire(5, TimeUnit.SECONDS));
  }

  @Test
  public void multipleListenBeforeReceiveWork() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(1 - TEST_SAMPLE);

    for (int i = 0; i < TEST_SAMPLE; i++) {
      new Thread(
              () -> {
                Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
                lock.release();
              })
          .start();
    }

    for (int i = 0; i < TEST_SAMPLE; i++) queue.onNewMsg(MESSAGE);

    Assertions.assertTrue(lock.tryAcquire(5, TimeUnit.SECONDS));
  }

  @Test
  public void multipleReceiveBeforeListenWork() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(1 - TEST_SAMPLE);

    for (int i = 0; i < TEST_SAMPLE; i++) queue.onNewMsg(MESSAGE);

    for (int i = 0; i < TEST_SAMPLE; i++) {
      new Thread(
              () -> {
                Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
                lock.release();
              })
          .start();
    }

    Assertions.assertTrue(lock.tryAcquire(5, TimeUnit.SECONDS));
  }

  @Test
  public void noMessageAfterTimeoutReturnsNull() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(0);

    new Thread(
            () -> {
              Assertions.assertNull(queue.takeTimeout(500));
              lock.release();
            })
        .start();

    Assertions.assertTrue(lock.tryAcquire(5, TimeUnit.SECONDS));
  }

  @Test
  public void listenAndReceiveMessagesInRandomOrderWorks() throws InterruptedException {
    MessageQueue queue = new MessageQueue();

    Semaphore lock = new Semaphore(1 - TEST_SAMPLE);
    List<Thread> threads = new ArrayList<>(TEST_SAMPLE * 2);

    for (int i = 0; i < TEST_SAMPLE; i++) {
      // Message sender
      threads.add(new Thread(() -> queue.onNewMsg(MESSAGE)));
      // Message receiver
      threads.add(
          new Thread(
              () -> {
                Assertions.assertEquals(MESSAGE, queue.takeTimeout(5000));
                lock.release();
              }));
    }

    Collections.shuffle(threads, random);
    threads.forEach(Thread::start);

    Assertions.assertTrue(lock.tryAcquire(5, TimeUnit.SECONDS));
  }
}
