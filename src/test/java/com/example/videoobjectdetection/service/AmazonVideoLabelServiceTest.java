package com.example.videoobjectdetection.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author sa
 * @date 29.08.2023
 * @time 10:09
 */
@SpringBootTest
class AmazonVideoLabelServiceTest
{
    @Autowired
    private AmazonVideoLabelService amazonVideoLabelService;

    @Test
    void detectLabels() throws Exception
    {
        String video = "d85c1f0ff4d184451affc2833f49ab1b.mp4";

        amazonVideoLabelService.detectLabels(video);
    }
}
