package com.megaiptv.eltv;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "channels")
public class Channel implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private String sourceId;
    private String name;
    private String url;
    private String logo;
    private String group;
    private boolean isFavorite;

    public Channel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    @Override
    public String toString() {
        return "Channel{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }
}
