package com.honmiv.ratelimiter.config;

import com.hazelcast.collection.IList;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfiguration {

    @Bean
    HazelcastInstance hazelcastInstance() {
        return Hazelcast.newHazelcastInstance();
    }

    @Bean
    IMap<String, String> requestsMap(HazelcastInstance hazelcastInstance, IList<String> requestsKeyList) {
        return hazelcastInstance.getMap("requestsMap");
    }

    @Bean
    IMap<String, String> responsesMap(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("responsesMap");
    }

    @Bean
    IList<String> requestsKeyList(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getList("requestsKeyList");
    }

    @Bean
    IMap<String, String> responsesListenerToRemoveMap(HazelcastInstance hazelcastInstance, IMap<String, String> responsesMap) {
        return hazelcastInstance.getMap("responseListenerToRemoveMap");
    }
}

