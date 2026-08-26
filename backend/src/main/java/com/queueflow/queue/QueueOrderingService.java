package com.queueflow.queue;

import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class QueueOrderingService {
    public static final int WAITING_MINUTES_PER_POINT = 15;

    public String databaseOrderBy() {
        return "(coalesce(p.weight,0) + floor(extract(epoch from (current_timestamp - qe.entered_at)) / 900)) DESC, "
                + "qe.scheduled_at NULLS LAST, qe.entered_at";
    }

    public Comparator<Candidate> comparator(Instant now) {
        return Comparator.<Candidate>comparingLong(candidate -> effectiveScore(candidate, now)).reversed()
                .thenComparing(Candidate::scheduledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Candidate::enteredAt);
    }

    long effectiveScore(Candidate candidate, Instant now) {
        long waitingMinutes = Math.max(0, Duration.between(candidate.enteredAt(), now).toMinutes());
        return candidate.priorityWeight() + waitingMinutes / WAITING_MINUTES_PER_POINT;
    }

    public record Candidate(int priorityWeight, Instant scheduledAt, Instant enteredAt) {}
}
