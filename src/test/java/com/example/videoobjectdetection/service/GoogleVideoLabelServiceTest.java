package com.example.videoobjectdetection.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author sa
 * @date 29.08.2023
 * @time 12:21
 */
@SpringBootTest
class GoogleVideoLabelServiceTest
{
    @Autowired
    private GoogleVideoLabelService googleVideoLabelService;

    @Test
    void detectLabels() throws Exception
    {
        String fileName = "/Users/mobile/Downloads/creative_344ea34fa4f62c716a0fd9d0da7e77be.mp4";
        googleVideoLabelService.detectLabels(fileName);
    }
}
