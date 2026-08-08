package com.megaiptv.eltv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory fallback storage for channels when database initialization fails.
 * This allows the app to function (non-persistently) even if Room database fails on TV devices.
 */
public class InMemoryChannelStore {
    private static InMemoryChannelStore INSTANCE;
    
    private final List<Channel> channels = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();
    private boolean isActive = false;
    private long nextChannelId = 1;
    
    private InMemoryChannelStore() {}
    
    public static InMemoryChannelStore getInstance() {
        if (INSTANCE == null) {
            synchronized (InMemoryChannelStore.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InMemoryChannelStore();
                }
            }
        }
        return INSTANCE;
    }
    
    public synchronized void activate() {
        isActive = true;
        android.util.Log.w("InMemoryChannelStore", "Activated - running in fallback mode");
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    // Channel operations
    
    public synchronized void insertChannels(List<Channel> newChannels) {
        // Assign IDs to channels that don't have them
        for (Channel ch : newChannels) {
            if (ch.getId() == null) {
                ch.setId(nextChannelId++);
            }
        }
        channels.addAll(newChannels);
        android.util.Log.i("InMemoryChannelStore", "Stored " + newChannels.size() + " channels in memory");
    }
    
    public synchronized void deleteChannelsBySource(String sourceId) {
        channels.removeIf(ch -> sourceId.equals(ch.getSourceId()));
    }
    
    public synchronized List<Channel> getAllChannels() {
        return new ArrayList<>(channels);
    }
    
    public synchronized List<Channel> getFavorites() {
        return channels.stream()
                .filter(Channel::isFavorite)
                .collect(Collectors.toList());
    }
    
    public synchronized List<String> getGroups() {
        return channels.stream()
                .map(Channel::getCategory)
                .filter(cat -> cat != null && !cat.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    public synchronized List<Channel> getChannelsByGroup(String group) {
        return channels.stream()
                .filter(ch -> group.equals(ch.getCategory()))
                .collect(Collectors.toList());
    }
    
    public synchronized void updateChannel(Channel channel) {
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getId() != null && 
                channels.get(i).getId().equals(channel.getId())) {
                channels.set(i, channel);
                return;
            }
        }
    }
    
    public synchronized List<Channel> searchChannels(String query) {
        String lowerQuery = query.toLowerCase();
        return channels.stream()
                .filter(ch -> ch.getName() != null && 
                             ch.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }
    
    // Source operations
    
    public synchronized void insertSource(Source source) {
        // Remove existing source with same URL
        sources.removeIf(s -> s.getUrl().equals(source.getUrl()));
        sources.add(source);
    }
    
    public synchronized List<Source> getAllSources() {
        return new ArrayList<>(sources);
    }
    
    public synchronized void deleteSource(String url) {
        sources.removeIf(s -> s.getUrl().equals(url));
    }
    
    public synchronized void clear() {
        channels.clear();
        sources.clear();
    }
}
