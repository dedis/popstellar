package common.net;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessageQueue {

  private final List<String> queue = new LinkedList<>();

  public synchronized void onNewMsg(String msg) {
    queue.add(msg);
    notifyAll();
  }

  public synchronized List<String> allMessages() {
    return new ArrayList<>(queue);
  }

  public synchronized List<String> messages(int firstN) {
    return queue.stream().limit(firstN).collect(Collectors.toList());
  }

  public synchronized List<String> messages(Predicate<String> filter) {
    return queue.stream().filter(filter).collect(Collectors.toList());
  }

  public synchronized String lastMessage() {
    return queue.isEmpty() ? null : queue.remove(0);
  }

  public synchronized String lastMessage(Predicate<String> filter) {
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

  public synchronized String lastMessage(long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();
    String msg = lastMessage();

    while (msg == null && System.currentTimeMillis() - start < timeout) {
      wait(timeout);
      msg = lastMessage();
    }

    return msg;
  }
}
