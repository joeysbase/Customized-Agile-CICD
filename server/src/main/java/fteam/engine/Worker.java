package fteam.engine;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for asynchronous workers managed by {@link WorkerManager}.
 */
public abstract class Worker implements Runnable {
  private List<String> messages;
  private AtomicBoolean status;

  /**
   * Creates a worker with externally supplied status and message sinks.
   */
  public Worker() {
  }

  /**
   * Appends one message to the shared output list.
   *
   * @param msg message to append
   */
  public void addMessage(String msg) {
    this.messages.add(msg);
  }

  /**
   * Returns the shared message list for this worker.
   *
   * @return message list
   */
  public List<String> getMessages() {
    return messages;
  }

  /**
   * Sets the shared completion flag.
   *
   * @param status completion flag
   */
  public void setStatus(AtomicBoolean status) {
    this.status = status;
  }

  /**
   * Sets the shared message sink.
   *
   * @param messages message list
   */
  public void setMessages(List<String> messages) {
    this.messages = messages;
  }

  /**
   * Returns the shared completion flag.
   *
   * @return completion flag
   */
  public AtomicBoolean getStatus() {
    return status;
  }

  /**
   * Marks this worker as completed.
   */
  public void setWorkDone() {
    this.status.set(true);
  }

  /**
   * Indicates whether this worker has finished.
   *
   * @return {@code true} when the worker is done
   */
  public boolean isWorkDone() {
    return status.get();
  }
}
