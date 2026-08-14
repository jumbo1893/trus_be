package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxEventPayload;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventPayloadFactoryTest {

    @Test
    void matchChangedHasNoAffectedPlayersWhenRosterDidNotChange() {
        OutboxEventPayload payload = OutboxEventPayloadFactory.matchChanged(
                10L, 5L, Set.of(), Set.of()
        );

        assertThat(payload.affectedPlayerIds()).isEmpty();
        assertThat(payload.addedPlayerIds()).isEmpty();
        assertThat(payload.removedPlayerIds()).isEmpty();
    }

    @Test
    void matchChangedUsesOnlyAddedAndRemovedPlayersAsAffected() {
        OutboxEventPayload payload = OutboxEventPayloadFactory.matchChanged(
                10L, 5L, Set.of(1L, 2L), Set.of(3L, 4L)
        );

        assertThat(payload.affectedPlayerIds()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(payload.addedPlayerIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(payload.removedPlayerIds()).containsExactlyInAnyOrder(3L, 4L);
    }
}
