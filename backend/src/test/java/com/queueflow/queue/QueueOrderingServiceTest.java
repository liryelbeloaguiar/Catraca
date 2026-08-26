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

        candidates.sort(service.comparator(now));

        assertThat(candidates).containsExactly(priority, regularEarly, regularLate);
    }

    @Test
    void waitingTimeEventuallyPreventsStarvation() {
        var now = Instant.parse("2026-08-19T15:00:00Z");
        var recentPriority = new QueueOrderingService.Candidate(5, null, now.minusSeconds(60));
        var longWaitingRegular = new QueueOrderingService.Candidate(0, null, now.minusSeconds(90 * 60));
        var candidates = new ArrayList<>(List.of(recentPriority, longWaitingRegular));

        candidates.sort(service.comparator(now));

        assertThat(candidates).containsExactly(longWaitingRegular, recentPriority);
    }

    @Test
    void usesScheduledTimeThenEntryTimeToBreakScoreTies() {
        var now = Instant.parse("2026-08-19T15:00:00Z");
        var walkIn = new QueueOrderingService.Candidate(0, null, now.minusSeconds(5 * 60));
        var laterAppointment = new QueueOrderingService.Candidate(0, now.minusSeconds(10 * 60), now.minusSeconds(5 * 60));
        var earlierAppointment = new QueueOrderingService.Candidate(0, now.minusSeconds(20 * 60), now.minusSeconds(4 * 60));
        var candidates = new ArrayList<>(List.of(walkIn, laterAppointment, earlierAppointment));

        candidates.sort(service.comparator(now));

        assertThat(candidates).containsExactly(earlierAppointment, laterAppointment, walkIn);
    }
}
