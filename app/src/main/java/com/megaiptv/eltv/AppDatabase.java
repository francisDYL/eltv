package com.megaiptv.eltv;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Update;

import java.util.List;

@Database(entities = {Channel.class, Source.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChannelDao channelDao();
    public abstract SourceDao sourceDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "eliptv_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Dao
    public interface ChannelDao {
        @Query("SELECT * FROM channels")
        List<Channel> getAll();

        @Query("SELECT * FROM channels WHERE category = :category")
        List<Channel> getByGroup(String category);

        @Query("SELECT DISTINCT category FROM channels WHERE category IS NOT NULL ORDER BY category ASC")
        List<String> getGroups();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<Channel> channels);

        @Query("DELETE FROM channels WHERE sourceId = :sourceId")
        void deleteBySource(String sourceId);

        @Update
        void update(Channel channel);

        @Query("SELECT * FROM channels WHERE isFavorite = 1")
        List<Channel> getFavorites();

        @Query("SELECT * FROM channels WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'")
        List<Channel> searchChannels(String query);
    }

    @Dao
    public interface SourceDao {
        @Query("SELECT * FROM sources")
        List<Source> getAll();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Source source);

        @Query("DELETE FROM sources WHERE url = :url")
        void delete(String url);
    }
}
