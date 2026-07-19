package com.megaiptv.eltv;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "sources")
public class Source implements Serializable {

    @PrimaryKey
    @NonNull
    private String url = "";
    private String name;
    private long lastSync;

    public Source() {}

    public Source(@NonNull String url, String name) {
        this.url = url;
        this.name = name;
        this.lastSync = 0;
    }

    @NonNull
    public String getUrl() { return url; }
    public void setUrl(@NonNull String url) { this.url = url; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getLastSync() { return lastSync; }
    public void setLastSync(long lastSync) { this.lastSync = lastSync; }
}

