package com.claimportal.claims.outbox;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPollerTest {

    private final OutboxRelay relay = mock(OutboxRelay.class);
    private final OutboxPoller poller = new OutboxPoller(relay);

    @Test
    void keepsPublishingWhileBatchesComeBackFull() {
        when(relay.publishPending()).thenReturn(50, 50, 7);
        when(relay.wasFullBatch(anyInt())).thenAnswer(inv -> (int) inv.getArgument(0) == 50);

        poller.poll();

        verify(relay, times(3)).publishPending();
    }

    @Test
    void stopsAfterAFailedOrEmptyRun() {
        when(relay.publishPending()).thenReturn(0);
        when(relay.wasFullBatch(anyInt())).thenAnswer(inv -> (int) inv.getArgument(0) == 50);

        poller.poll();

        verify(relay, times(1)).publishPending();
    }
}
