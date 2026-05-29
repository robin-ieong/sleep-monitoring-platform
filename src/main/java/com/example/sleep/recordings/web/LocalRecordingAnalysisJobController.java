package com.example.sleep.recordings.web;

import com.example.sleep.recordings.infrastructure.SqsRecordingAnalysisJobPoller;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
public class LocalRecordingAnalysisJobController {

    private final SqsRecordingAnalysisJobPoller poller;

    public LocalRecordingAnalysisJobController(SqsRecordingAnalysisJobPoller poller) {
        if (poller == null) {
            throw new IllegalArgumentException("poller must not be null");
        }
        this.poller = poller;
    }

    @PostMapping("/dev/recording-analysis-jobs/poll")
    ResponseEntity<RecordingAnalysisJobPollResponse> poll() {
        return ResponseEntity.ok(new RecordingAnalysisJobPollResponse(poller.pollOnce()));
    }
}
