package com.example.videoobjectdetection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sa
 * @date 28.08.2023
 * @time 17:10
 */
@Slf4j
@Service
public class AmazonVideoLabelService
{
    @Value("${aws.access-key-id}")
    private String ACCESS_KEY;

    @Value("${aws.secret-key}")
    private String SECRET_KEY;

    @Value("${aws.bucket}")
    private String BUCKET;

    public void detectLabels(String video) throws Exception
    {
        StopWatch sw = new StopWatch();
        sw.start();

        AwsCredentials awsCredentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);

        AwsCredentialsProvider credentials = StaticCredentialsProvider.create(awsCredentials);

        RekognitionClient rekClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();

        S3Object s3Obj = S3Object.builder()
                .bucket(BUCKET)
                .name(video)
                .build();

        Video vidOb = Video.builder()
                .s3Object(s3Obj)
                .build();

        StartLabelDetectionRequest labelDetectionRequest = StartLabelDetectionRequest.builder()
                .jobTag("DetectingLabels")
                //.notificationChannel(channel)
                .video(vidOb)
                .minConfidence(50F)
                .build();

        StartLabelDetectionResponse labelDetectionResponse = rekClient.startLabelDetection(labelDetectionRequest);
        String startJobId = labelDetectionResponse.jobId();

        boolean ans = true;
        String status = "";
        int yy = 0;
        while (ans)
        {

            GetLabelDetectionRequest detectionRequest = GetLabelDetectionRequest.builder()
                    .jobId(startJobId)
                    .maxResults(10)
                    .build();

            GetLabelDetectionResponse result = rekClient.getLabelDetection(detectionRequest);
            status = result.jobStatusAsString();

            if (status.compareTo("SUCCEEDED") == 0)
            {
                ans = false;
                getLabelDetectionResults(startJobId, rekClient);
            }
            else
            {
                System.out.println(yy + " status is: " + status);
            }

            Thread.sleep(1000);
            yy++;
        }
        sw.stop();
        log.info("Job is completed in seconds: {}", sw.getTotalTimeSeconds());
    }

    private void getLabelDetectionResults(String startJobId, RekognitionClient rekClient)
    {

        int maxResults = 10;
        String paginationToken = null;
        GetLabelDetectionResponse labelDetectionResult = null;
        Set<String> labelSet = new HashSet<>();

        do
        {
            if (labelDetectionResult != null)
            {
                paginationToken = labelDetectionResult.nextToken();
            }

            GetLabelDetectionRequest labelDetectionRequest = GetLabelDetectionRequest.builder()
                    .jobId(startJobId)
                    .sortBy(LabelDetectionSortBy.TIMESTAMP)
                    .maxResults(maxResults)
                    .nextToken(paginationToken)
                    .build();

            labelDetectionResult = rekClient.getLabelDetection(labelDetectionRequest);

            VideoMetadata videoMetaData = labelDetectionResult.videoMetadata();

//            System.out.println("Format: " + videoMetaData.format());
//            System.out.println("Codec: " + videoMetaData.codec());
//            System.out.println("Duration: " + videoMetaData.durationMillis());
//            System.out.println("FrameRate: " + videoMetaData.frameRate());

            //Show labels, confidence and detection times
            List<LabelDetection> detectedLabels = labelDetectionResult.labels();

            for (LabelDetection detectedLabel : detectedLabels)
            {
                long seconds = detectedLabel.timestamp();
                Label label = detectedLabel.label();
                labelSet.add(label.name());
//                System.out.println("Millisecond: " + Long.toString(seconds) + " ");
//
//                System.out.println("   Label:" + label.name());
//                System.out.println("   Confidence:" + detectedLabel.label().confidence().toString());
//
//                List<Instance> instances = label.instances();
//                System.out.println("   Instances of " + label.name());
//                if (instances.isEmpty())
//                {
//                    System.out.println("        " + "None");
//                }
//                else
//                {
//                    for (Instance instance : instances)
//                    {
//                        System.out.println("        Confidence: " + instance.confidence().toString());
//                        System.out.println("        Bounding box: " + instance.boundingBox().toString());
//                    }
//                }
//                System.out.println("   Parent labels for " + label.name() + ":");
//                List<Parent> parents = label.parents();
//                if (parents.isEmpty())
//                {
//                    System.out.println("        None");
//                }
//                else
//                {
//                    for (Parent parent : parents)
//                    {
//                        System.out.println("        " + parent.name());
//                    }
//                }
//                System.out.println();
            }
        } while (labelDetectionResult != null && labelDetectionResult.nextToken() != null);

        for (String label : labelSet)
        {
            System.out.println(label);
        }

    }
}
