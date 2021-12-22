package common.net;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MessageQueue implements MessageBuffer {

  private final List<String> queue = new LinkedList<>();

  public synchronized void onNewMsg(String msg) {
    queue.add(msg);
    notifyAll();
  }

  @Override
  public synchronized String peek() {
    return queue.get(0);
  }

  @Override
  public synchronized String peek(final Predicate<String> filter) {
    return queue.stream().filter(filter).findFirst().orElse(null);
  }

  @Override
  public synchronized List<String> peekAll() {
    return new ArrayList<>(queue);
  }

  @Override
  public synchronized List<String> peekAll(Predicate<String> filter) {
    return queue.stream().filter(filter).collect(Collectors.toList());
  }

  @Override
  public synchronized List<String> peekN(int firstN) {
    return queue.stream().limit(firstN).collect(Collectors.toList());
  }

  @Override
  public synchronized String take() {
    return queue.isEmpty() ? null : queue.remove(0);
  }

  @Override
  public synchronized String take(Predicate<String> filter) {
    Iterator<String> it = queue.iterator();
    while (it.hasNext()) {
      String msg = it.next();

      if (filter.test(msg)) {
        it.remove();
        return msg;
      }
    }

    return null;
  }

  @Override
  public synchronized List<String> takeAll() {
    List<String> messages = new ArrayList<>(queue);
    queue.clear();
    return messages;
  }

  @Override
  public synchronized List<String> takeAll(final Predicate<String> filter) {
    List<String> messages = new LinkedList<>();
    Iterator<String> it = queue.iterator();

    while (it.hasNext()) {
      String msg = it.next();
      if (filter.test(msg)) {
        messages.add(msg);
        it.remove();
      }
    }

    return messages;
  }

  @Override
  public synchronized List<String> takeN(int limit) {
    List<String> messages = new LinkedList<>();
    for (int i = 0; i < limit && !queue.isEmpty(); i++) messages.add(queue.remove(0));

    return messages;
  }

  @Override
  public synchronized String takeTimeout(long timeout) {
    return retrieveWithTimeout(this::take, timeout);
  }

  @Override
  public synchronized String takeTimeout(Predicate<String> filter, long timeout) {
    return retrieveWithTimeout(() -> take(filter), timeout);
  }

  @Override
  public synchronized void clear() {
    queue.clear();
  }

  private synchronized String retrieveWithTimeout(Supplier<String> retriever, long timeout) {
    long start = System.currentTimeMillis();
    String msg = retriever.get();

    try {
      long timeSinceStart = 0;
      while (msg == null && timeSinceStart < timeout) {
        // Wait for the remaining time before the timout.
        // If a notify signal is received, the wait is ended prematurely.
        wait(timeout - timeSinceStart);
        msg = retriever.get();

        timeSinceStart = System.currentTimeMillis() - start;
      }
    } catch (InterruptedException e) {
      return null;
    }

    return msg;
  }
}
