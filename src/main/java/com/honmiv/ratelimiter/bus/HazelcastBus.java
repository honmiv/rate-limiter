package com.honmiv.ratelimiter.bus;

import com.google.gson.Gson;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.honmiv.ratelimiter.dto.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Component
public class HazelcastBus {

    private final IMap<String, String> requestsMap;
    private final IMap<String, String> responsesMap;
    private final IList<String> requestsKeyList;
    private final IMap<String, String> responsesListenerToRemoveMap;

    public HazelcastBus(IMap<String, String> requestsMap,
                        IMap<String, String> responsesMap,
                        IList<String> requestsKeyList,
                        IMap<String, String> responsesListenerToRemoveMap) {
        this.requestsMap = requestsMap;
        this.responsesMap = responsesMap;
        this.requestsKeyList = requestsKeyList;
        this.responsesListenerToRemoveMap = responsesListenerToRemoveMap;

        requestsMap.addEntryListener(
                new EntryAdapter<String, String>() {
                    @Override
                    public void entryAdded(EntryEvent<String, String> event) {
                        super.entryAdded(event);
                        log.info("request with rquid {} added", event.getKey());
                        requestsKeyList.add(event.getKey());
                    }

                    @Override
                    public void entryExpired(EntryEvent<String, String> event) {
                        super.entryExpired(event);
                        requestsKeyList.removeIf(e -> e.equals(event.getKey()));
                    }
                },
                false);

        responsesListenerToRemoveMap.addEntryListener(
                new EntryAdapter<String, String>() {
                    @Override
                    public void entryRemoved(EntryEvent<String, String> event) {
                        super.entryRemoved(event);
                        responsesMap.removeEntryListener(UUID.fromString(event.getOldValue()));
                    }
                },
                true);
    }

    public DeferredResult<HttpResponse> sendRequestToBus(String rquid, String request) {
        log.info("adding response listener for rquid {}", rquid);
        DeferredResult<HttpResponse> deferredResult = new DeferredResult<>();
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
                        responsesListenerToRemoveMap.remove(event.getKey());
                        deferredResult.setResult(new Gson().fromJson(event.getValue(), HttpResponse.class));
                    }
                },
                rquid,
                true);
        responsesListenerToRemoveMap.put(rquid, entryListener.toString());
        log.info("sending to requests rquid = {}", rquid);
        requestsMap.put(rquid, request);
        log.info("sent to requests rquid = {}", rquid);
        return deferredResult;
    }

    public void sendResponseToBus(String rqUid, String response) {
        responsesMap.put(rqUid, response);
    }

    public void setRequestListener(ItemListener<String> itemListener) {
        requestsKeyList.addItemListener(itemListener, false);
    }

    public String getNewRequest() {
        String requestRqUid = requestsKeyList.remove(requestsKeyList.size() - 1);
        return requestsMap.remove(requestRqUid);
    }

    public String getOldRequest() {
        String requestRqUid = requestsKeyList.remove(0);
        return requestsMap.remove(requestRqUid);
    }
}
