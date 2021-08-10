package com.honmiv.ratelimiter.bucket;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class BucketRateLimiter {

    @SneakyThrows
    public void throttle() {
        //TODO: implement bucket4j over Ignite throttling
        Thread.sleep(5000);
    }
}
