package com.honmiv.ratelimiter.config;

import com.hazelcast.collection.IList;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class HazelcastConfiguration {

    @Bean
    HazelcastInstance hazelcastInstance() {
        return Hazelcast.newHazelcastInstance();
    }

    @Bean
    public IMap<String, String> requestsMap(HazelcastInstance hazelcastInstance, IList<String> requestsKeyList) {
        IMap<String, String> requestsMap = hazelcastInstance.getMap("requestsMap");
        requestsMap.addEntryListener(
                new EntryAdapter<String, String>() {
                    @Override
                    public void entryAdded(EntryEvent<String, String> event) {
                        super.entryAdded(event);
                        requestsKeyList.add(event.getKey());
                    }

                    @Override
                    public void entryExpired(EntryEvent<String, String> event) {
                        super.entryExpired(event);
                        requestsKeyList.removeIf(e -> e.equals(event.getKey()));
                    }
                },
                false);
        return requestsMap;
    }

    @Bean
    public IMap<String, String> responsesMap(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("responsesMap");
    }

    @Bean
    public IList<String> requestsKeyList(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getList("requestsKeyList");
    }

    @Bean
    public IMap<String, String> responseListenerToRemoveMap(HazelcastInstance hazelcastInstance, IMap<String, String> responsesMap) {
        IMap<String, String> responseListenerToRemoveMap = hazelcastInstance.getMap("responseListenerToRemoveMap");
        responseListenerToRemoveMap.addEntryListener(
                new EntryAdapter<String, String>() {
                    @Override
                    public void entryRemoved(EntryEvent<String, String> event) {
                        super.entryRemoved(event);
                        responsesMap.removeEntryListener(UUID.fromString(event.getOldValue()));
                    }
                },
                true);
        return responseListenerToRemoveMap;
    }
}

