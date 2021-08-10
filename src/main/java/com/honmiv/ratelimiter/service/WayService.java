package com.honmiv.ratelimiter.service;

import com.google.gson.Gson;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.honmiv.ratelimiter.bucket.BucketRateLimiter;
import com.honmiv.ratelimiter.bus.HazelcastBus;
import com.honmiv.ratelimiter.dto.HttpRequest;
import com.honmiv.ratelimiter.dto.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ForkJoinPool;

@Slf4j
@Configuration
public class WayService {

    public WayService(
            BucketRateLimiter bucketRateLimiter,
            @Value("${reqs.total.new}") int newReqsTotalPerListener,
            @Value("${reqs.total.old}") int oldReqsTotalPerListener,
            HazelcastBus hazelcastBus
    ) {
        log.info("adding listener for rqKeyList");
        hazelcastBus.setRequestListener(
                new ItemListener<>() {
                    private final int newReqsTotal = newReqsTotalPerListener;
                    private final int oldReqsTotal = oldReqsTotalPerListener;
                    private int newReqs = newReqsTotalPerListener;
                    private int oldReqs = oldReqsTotalPerListener;

                    @Override
                    public void itemAdded(ItemEvent<String> item) {
                        log.info("rqUid {} received", item);

                        boolean processOldReq;

                        if (oldReqs > 0) {
                            processOldReq = true;
                            oldReqs--;
                        } else if (newReqs > 0) {
                            processOldReq = false;
                            newReqs--;
                        } else {
                            processOldReq = true;
                            oldReqs = oldReqsTotal - 1;
                            newReqs = newReqsTotal;
                        }

                        HttpRequest httpRequest;

                        if (processOldReq) {
                            httpRequest = new Gson().fromJson(hazelcastBus.getOldRequest(), HttpRequest.class);
                        } else {
                            httpRequest = new Gson().fromJson(hazelcastBus.getNewRequest(), HttpRequest.class);
                        }

                        log.info("httprquest = {}", httpRequest);
                        if (httpRequest != null) {
                            ForkJoinPool.commonPool().submit(() -> {
                                log.info("throttling");
                                bucketRateLimiter.throttle();
                                // webClient that send http request and receive response:)))
                                String response = HttpResponse.builder()
                                        .rqUid(httpRequest.getRqUid())
                                        .value("response")
                                        .build()
                                        .toString();
                                hazelcastBus.sendResponseToBus(httpRequest.getRqUid(), response);
                            });
                        } else {
                            log.warn("null wayRequest");
                        }
                    }

                    @Override
                    public void itemRemoved(ItemEvent<String> item) {
                        log.trace("removed rqUid = {} from requests list", item.getItem());
                    }
                });
        log.info("WayService initialized");
    }
}
