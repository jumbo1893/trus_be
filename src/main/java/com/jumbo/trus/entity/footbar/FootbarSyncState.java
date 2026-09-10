package com.jumbo.trus.entity.footbar;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.Instant;

/** Durable retry, warning and lease state, independent of OAuth token rotation. */
@Entity
@Data
public class FootbarSyncState {
    @Id
    private Long accountId;
    private Instant importedConnectionAt;
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private Instant leaseUntil;
    private String leaseOwner;
    private String warning;
    private boolean reconnectRequired;
    private Instant notifiedAt;
}
