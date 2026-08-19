package com.example.kite.service;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;


@Service
public class ConfigManagerService {

    private final StringRedisTemplate redis;
    private Map<String, Object> configMap = new ConcurrentHashMap<>();

    private Map<String, Set<String>> watchList = new ConcurrentHashMap<>();

    public ConfigManagerService(StringRedisTemplate redis) {
        watchList.put("SELL", new CopyOnWriteArraySet<>());
        watchList.put("BUY", new CopyOnWriteArraySet<>());
        this.redis = redis;
    }

    public void setConfigMap(String key, Object value) {
        configMap.put(key, value);
    }

    public Object getOrDefaultValue(String key, Object value){
        return configMap.getOrDefault(key, value);
    }

    public Map<String, Object> getAllProperties() {
        return configMap;
    }

    public void addWatchList(String action, Set<String> symbols) {
        watchList.get(action).addAll(symbols);
    }

    public void addSymbol(String action, String symbol) {
        watchList.get(action).add(symbol);
    }

    public void removeFromWatchList(String action, String symbol) {
        watchList.get(action).remove(symbol);
    }
    public void removeFromWatchList(String action, List<String> symbols) {
        watchList.get(action).removeAll(symbols);
    }

    public Set<String> getWatchlistForAction(String action) {
        return watchList.get(action);
    }

}
