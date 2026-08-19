package com.queueflow.queue;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueueOrderingServiceTest {
    private final QueueOrderingService service = new QueueOrderingService();

    @Test
    void ordersByPriorityThenScheduleThenCheckIn() {
        var now = Instant.parse("2026-08-19T12:00:00Z");
        var regularEarly = new QueueOrderingService.Candidate(0, now, now.minusSeconds(600));
        var priority = new QueueOrderingService.Candidate(10, now.plusSeconds(1800), now.minusSeconds(60));
        var regularLate = new QueueOrderingService.Candidate(0, now.plusSeconds(900), now.minusSeconds(300));
        var candidates = new ArrayList<>(List.of(regularLate, regularEarly, priority));

        candidates.sort(service.comparator());

        assertThat(candidates).containsExactly(priority, regularEarly, regularLate);
    }
}
