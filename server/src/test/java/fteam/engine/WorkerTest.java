package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the shared Worker base class behavior with a minimal stub subclass. */
class WorkerTest {

  private List<String> messages;
  private AtomicBoolean status;

  @BeforeEach
  void setUp() {
    messages = new ArrayList<>();
    status = new AtomicBoolean(false);
  }

  @Test
  void workerTracksMessagesAndCompletion() {
    Worker worker =
        new Worker() {
          @Override
          public void run() {
            addMessage("first");
            addMessage("second");
            setWorkDone();
          }
        };
    worker.setMessages(messages);
    worker.setStatus(status);

    assertFalse(worker.isWorkDone());
    worker.run();
    assertTrue(worker.isWorkDone());
    assertEquals(List.of("first", "second"), worker.getMessages());
    assertSame(messages, worker.getMessages());
    assertSame(status, worker.getStatus());
  }
}
