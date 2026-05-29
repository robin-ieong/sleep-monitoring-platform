package com.example.sleep.recordings.web;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingAnalysisResultNotFoundException;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.application.CompleteRecordingUploadCommand;
import com.example.sleep.recordings.application.CompleteRecordingUploadService;
import com.example.sleep.recordings.application.CreateRecordingUploadCommand;
import com.example.sleep.recordings.application.CreateRecordingUploadResult;
import com.example.sleep.recordings.application.CreateRecordingUploadService;
import com.example.sleep.recordings.application.MarkRecordingStoredCommand;
import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.RecordingAnalysisResultRepository;
import com.example.sleep.recordings.application.RecordingRepository;
import com.example.sleep.recordings.application.RequestRecordingAnalysisCommand;
import com.example.sleep.recordings.application.RequestRecordingAnalysisService;
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
    private final CreateRecordingUploadService createUploadService;
    private final CompleteRecordingUploadService completeUploadService;
    private final RequestRecordingAnalysisService requestAnalysisService;
    private final RecordingRepository repository;
    private final RecordingAnalysisResultRepository resultRepository;

    public RecordingController(
            RegisterRecordingService service,
            MarkRecordingStoredService markStoredService,
            CreateRecordingUploadService createUploadService,
            CompleteRecordingUploadService completeUploadService,
            RequestRecordingAnalysisService requestAnalysisService,
            RecordingRepository repository,
            RecordingAnalysisResultRepository resultRepository
    ) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        if (markStoredService == null) {
            throw new IllegalArgumentException("markStoredService must not be null");
        }
        if (createUploadService == null) {
            throw new IllegalArgumentException("createUploadService must not be null");
        }
        if (completeUploadService == null) {
            throw new IllegalArgumentException("completeUploadService must not be null");
        }
        if (requestAnalysisService == null) {
            throw new IllegalArgumentException("requestAnalysisService must not be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (resultRepository == null) {
            throw new IllegalArgumentException("resultRepository must not be null");
        }
        this.service = service;
        this.markStoredService = markStoredService;
        this.createUploadService = createUploadService;
        this.completeUploadService = completeUploadService;
        this.requestAnalysisService = requestAnalysisService;
        this.repository = repository;
        this.resultRepository = resultRepository;
    }

    @PostMapping("/recordings")
    ResponseEntity<RecordingHttpResponse> register(@Valid @RequestBody RegisterRecordingHttpRequest request) {
        Recording recording = service.register(request.toCommand());

        return ResponseEntity
                .created(URI.create("/recordings/" + recording.id()))
                .body(RecordingHttpResponse.from(recording));
    }

    @PostMapping("/recording-uploads")
    ResponseEntity<RecordingUploadHttpResponse> createUpload(@Valid @RequestBody RegisterRecordingHttpRequest request) {
        CreateRecordingUploadResult result = createUploadService.createUpload(new CreateRecordingUploadCommand(
                new RecordingId(request.id()),
                request.ownerId(),
                request.originalFilename(),
                request.contentType()
        ));

        return ResponseEntity
                .created(URI.create("/recordings/" + result.recording().id()))
                .body(RecordingUploadHttpResponse.from(result));
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

    @PostMapping("/recordings/{id}/upload-complete")
    ResponseEntity<RecordingHttpResponse> completeUpload(
            @PathVariable String id,
            @Valid @RequestBody MarkRecordingStoredHttpRequest request
    ) {
        Recording recording = completeUploadService.completeUpload(new CompleteRecordingUploadCommand(
                new RecordingId(id),
                request.toStorageObjectReference()
        ));

        return ResponseEntity.ok(RecordingHttpResponse.from(recording));
    }

    @PostMapping("/recordings/{id}/analysis-requests")
    ResponseEntity<RecordingHttpResponse> requestAnalysis(@PathVariable String id) {
        Recording recording = requestAnalysisService.requestAnalysis(new RequestRecordingAnalysisCommand(
                new RecordingId(id)
        ));

        return ResponseEntity.accepted().body(RecordingHttpResponse.from(recording));
    }

    @GetMapping("/recordings/{id}/analysis-result")
    ResponseEntity<RecordingAnalysisResultHttpResponse> getAnalysisResult(@PathVariable String id) {
        RecordingId recordingId = new RecordingId(id);
        RecordingAnalysisResult result = resultRepository.findByRecordingId(recordingId)
                .orElseThrow(() -> new RecordingAnalysisResultNotFoundException(recordingId));

        return ResponseEntity.ok(RecordingAnalysisResultHttpResponse.from(result));
    }
}
