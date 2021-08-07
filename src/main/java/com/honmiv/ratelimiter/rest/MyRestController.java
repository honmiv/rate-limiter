package com.honmiv.ratelimiter.rest;

import com.google.gson.Gson;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
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

    private final IMap<String, String> requestsMap;
    private final IMap<String, String> responsesMap;
    private final IMap<String, String> responseListenerToRemoveMap;

    @PostMapping("/way")
    public DeferredResult<HttpResponse> handleReqDefResult(@RequestBody HttpRequest httpRequest) {
        log.info("Received rquid = {}", httpRequest.getRqUid());
        DeferredResult<HttpResponse> result = new DeferredResult<>();

        log.info("sending to requests rquid = {}", httpRequest.getRqUid());
        requestsMap.put(httpRequest.getRqUid(), httpRequest.toString());
        log.info("sent to requests rquid = {}", httpRequest.getRqUid());

        UUID entryListener = responsesMap.addEntryListener(
                new EntryAdapter<String, String>() {
                    @Override
                    public void entryAdded(EntryEvent<String, String> event) {
                        super.entryAdded(event);
                        log.info("RECEIVED RESPONSE with rquid = {} and body = {} and THREAD {}",
                                event.getKey(),
                                event.getValue(), Thread.currentThread().getId()
                        );
                        responsesMap.remove(event.getKey());
                        responseListenerToRemoveMap.remove(event.getKey());
                        result.setResult(new Gson().fromJson(event.getValue(), HttpResponse.class));
                    }
                },
                httpRequest.getRqUid(),
                true);

        responseListenerToRemoveMap.put(httpRequest.getRqUid(), entryListener.toString());
        return result;
    }
}
