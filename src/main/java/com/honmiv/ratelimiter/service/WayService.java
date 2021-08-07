package com.honmiv.ratelimiter.service;

import com.google.gson.Gson;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.map.IMap;
import com.honmiv.ratelimiter.bucket.BucketRateLimiter;
import com.honmiv.ratelimiter.dto.HttpRequest;
import com.honmiv.ratelimiter.dto.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class WayService {

    public WayService(
            BucketRateLimiter bucketRateLimiter,
            @Value("${reqs.total.new}") int newReqsTotalPerListener,
            @Value("${reqs.total.old}") int oldReqsTotalPerListener,
            IMap<String, String> requestsMap,
            IMap<String, String> responsesMap,
            IList<String> requestsKeyList
    ) {
        requestsKeyList.addItemListener(
                new ItemListener<>() {
                    private final int newReqsTotal = newReqsTotalPerListener;
                    private final int oldReqsTotal = oldReqsTotalPerListener;
                    private int newReqs = newReqsTotalPerListener;
                    private int oldReqs = oldReqsTotalPerListener;

                    @Override
                    public void itemAdded(ItemEvent<String> item) {

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

                        ForkJoinPool.commonPool().submit(() -> {
                            bucketRateLimiter.throttle();

                            HttpRequest httpRequest;

                            if (processOldReq) {
                                httpRequest = new Gson().fromJson(requestsMap.get(requestsKeyList.get(0)), HttpRequest.class);
                            } else {
                                httpRequest = new Gson().fromJson(requestsMap.get(requestsKeyList.get(requestsKeyList.size() - 1)), HttpRequest.class);
                            }
                            if (httpRequest != null) {
                                log.info("rquid = {} sending to responsesMap", httpRequest.getRqUid());
                                requestsMap.remove(httpRequest.getRqUid());

                                log.info("START {}", LocalTime.now());
                                requestsKeyList.remove(httpRequest.getRqUid());
                                log.info("STOP {}", LocalTime.now());

                                // webClient that send http request and receive response:)))
                                responsesMap.put(
                                        httpRequest.getRqUid(), HttpResponse.builder()
                                                .rqUid(httpRequest.getRqUid())
                                                .value("response")
                                                .build()
                                                .toString());

                                log.info("rquid {} sent to responsesMap", httpRequest.getRqUid());
                            } else {
                                log.warn("null wayRequest");
                            }
                        });
                    }

                    @Override
                    public void itemRemoved(ItemEvent<String> item) {
                        // do nothing
                    }
                },
                true);
        log.info("WayService initialized");

    }
}
