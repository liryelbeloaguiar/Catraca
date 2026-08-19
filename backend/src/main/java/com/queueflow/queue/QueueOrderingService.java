package com.queueflow.queue;

import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class QueueOrderingService {
    public String databaseOrderBy() {
        return "coalesce(p.weight,0) DESC, qe.scheduled_at NULLS LAST, qe.entered_at";
    }

    public Comparator<Candidate> comparator() {
        return Comparator.comparingInt(Candidate::priorityWeight).reversed()
                .thenComparing(Candidate::scheduledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Candidate::enteredAt);
    }

    public record Candidate(int priorityWeight, Instant scheduledAt, Instant enteredAt) {}
}
