package com.example.sleep.recordings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RecordingController {

    private final RegisterRecordingService service;

    public RecordingController(RegisterRecordingService service) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        this.service = service;
    }

    @PostMapping("/recordings")
    ResponseEntity<RegisterRecordingHttpResponse> register(@RequestBody RegisterRecordingHttpRequest request) {
        Recording recording = service.register(request.toCommand());

        return ResponseEntity
                .created(URI.create("/recordings/" + recording.id()))
                .body(RegisterRecordingHttpResponse.from(recording));
    }
}
