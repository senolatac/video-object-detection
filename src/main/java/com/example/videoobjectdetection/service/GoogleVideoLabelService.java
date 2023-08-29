package com.example.videoobjectdetection.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.videointelligence.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author sa
 * @date 29.08.2023
 * @time 12:10
 */
@Slf4j
@Service
public class GoogleVideoLabelService
{
    @Value("classpath:text-to-speech-creds.json")
    private Resource resource;

    public void detectLabels(String fileName) throws Exception
    {
        StopWatch sw = new StopWatch();
        sw.start();
        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);
        ByteString videoBytes = ByteString.copyFrom(data);
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(resource.getInputStream()));

        VideoIntelligenceServiceSettings settings = VideoIntelligenceServiceSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();

        try (VideoIntelligenceServiceClient client = VideoIntelligenceServiceClient.create(settings))
        {
            String gcsUri = "gs://cloud-samples-data/video/cat.mp4";

            // Create an operation that will contain the response when the operation completes.
            AnnotateVideoRequest request =
                    AnnotateVideoRequest.newBuilder()
                            //.setInputContent(videoBytes)
                            .setInputUri(gcsUri)
                            .addFeatures(Feature.LABEL_DETECTION)
                            .build();

            OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> response = client.annotateVideoAsync(request);

            System.out.println("Waiting for operation to complete...");

            List<VideoAnnotationResults> results = response.get().getAnnotationResultsList();
            if (results.isEmpty()) {
                System.out.println("No labels detected in " + gcsUri);
                return;
            }

            for (VideoAnnotationResults result : results) {
                System.out.println("Labels:");
                // get video segment label annotations
                for (LabelAnnotation annotation : result.getSegmentLabelAnnotationsList()) {
                    System.out.println("Video label description : " + annotation.getEntity().getDescription());
                    // categories
                    for (Entity categoryEntity : annotation.getCategoryEntitiesList()) {
                        System.out.println("Label Category description : " + categoryEntity.getDescription());
                    }
                    // segments
                    for (LabelSegment segment : annotation.getSegmentsList()) {
                        double startTime =
                                segment.getSegment().getStartTimeOffset().getSeconds()
                                        + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                        double endTime =
                                segment.getSegment().getEndTimeOffset().getSeconds()
                                        + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                        System.out.printf("Segment location : %.3f:%.3f\n", startTime, endTime);
                        System.out.println("Confidence : " + segment.getConfidence());
                    }
                }
            }
        }
        sw.stop();
        log.info("Job completed in seconds: {}", sw.getTotalTimeSeconds());
    }
}
