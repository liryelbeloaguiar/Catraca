package com.queueflow.queue;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/queues/{queueId}")
public class QueueController {
    private final QueueService service;
    public QueueController(QueueService service){this.service=service;}
    @PostMapping("/entries") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('QUEUE_MANAGE')")
    QueueService.Ticket enqueue(@PathVariable UUID queueId,@RequestBody EnqueueRequest request,@AuthenticationPrincipal Jwt jwt){return service.enqueue(queueId,request.patientId(),request.appointmentId(),request.priorityId(),UUID.fromString(jwt.getSubject()));}
    @PostMapping("/call-next") @PreAuthorize("hasAuthority('QUEUE_MANAGE')")
    Map<String,Object> callNext(@PathVariable UUID queueId, @RequestBody(required=false) CallNextRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        CallNextRequest safeRequest = request == null ? new CallNextRequest(null, null) : request;
        return service.callNext(queueId, UUID.fromString(jwt.getSubject()),
                safeRequest.counterId(), safeRequest.roomId());
    }
    public record EnqueueRequest(UUID patientId,UUID appointmentId,UUID priorityId){}
    public record CallNextRequest(UUID counterId, UUID roomId) {}
}
