package com.example.sleep.recordings.web;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.application.MarkRecordingStoredCommand;
import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.RecordingRepository;
import com.example.sleep.recordings.application.RegisterRecordingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RecordingController {

    private final RegisterRecordingService service;
    private final MarkRecordingStoredService markStoredService;
    private final RecordingRepository repository;

    public RecordingController(
            RegisterRecordingService service,
            MarkRecordingStoredService markStoredService,
            RecordingRepository repository
    ) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        if (markStoredService == null) {
            throw new IllegalArgumentException("markStoredService must not be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        this.service = service;
        this.markStoredService = markStoredService;
        this.repository = repository;
    }

    @PostMapping("/recordings")
    ResponseEntity<RecordingHttpResponse> register(@Valid @RequestBody RegisterRecordingHttpRequest request) {
        Recording recording = service.register(request.toCommand());

        return ResponseEntity
                .created(URI.create("/recordings/" + recording.id()))
                .body(RecordingHttpResponse.from(recording));
    }

    @GetMapping("/recordings/{id}")
    ResponseEntity<RecordingHttpResponse> get(@PathVariable String id) {
        RecordingId recordingId = new RecordingId(id);
        Recording recording = repository.findById(recordingId)
                .orElseThrow(() -> new RecordingNotFoundException(recordingId));

        return ResponseEntity.ok(RecordingHttpResponse.from(recording));
    }

    @PatchMapping("/recordings/{id}/storage")
    ResponseEntity<RecordingHttpResponse> markStored(
            @PathVariable String id,
            @Valid @RequestBody MarkRecordingStoredHttpRequest request
    ) {
        Recording recording = markStoredService.markStored(new MarkRecordingStoredCommand(
                new RecordingId(id),
                request.toStorageObjectReference()
        ));

        return ResponseEntity.ok(RecordingHttpResponse.from(recording));
    }
}
