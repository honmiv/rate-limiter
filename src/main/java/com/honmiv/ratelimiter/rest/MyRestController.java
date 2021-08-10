package com.honmiv.ratelimiter.rest;

import com.google.gson.Gson;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.honmiv.ratelimiter.bus.HazelcastBus;
import com.honmiv.ratelimiter.dto.HttpRequest;
import com.honmiv.ratelimiter.dto.HttpResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
public class MyRestController {

    private final HazelcastBus hazelcastBus;

    @PostMapping("/way")
    public DeferredResult<HttpResponse> handleReqDefResult(@RequestBody HttpRequest httpRequest) {
        log.info("Received rquid = {}", httpRequest.getRqUid());
        return hazelcastBus.sendRequestToBus(httpRequest.getRqUid(), httpRequest.toString());
    }
}
