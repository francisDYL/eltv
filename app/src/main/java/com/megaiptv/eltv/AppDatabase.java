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
    private static volatile boolean DATABASE_AVAILABLE = true;

    public static boolean isDatabaseAvailable() {
        return DATABASE_AVAILABLE;
    }

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        Context appContext = context.getApplicationContext();
                        
                        // Verify database directory exists and is writable
                        java.io.File dbDir = appContext.getDatabasePath("eliptv_database").getParentFile();
                        if (dbDir != null && !dbDir.exists()) {
                            boolean created = dbDir.mkdirs();
                            android.util.Log.i("AppDatabase", "Database directory created: " + created);
                        }
                        
                        INSTANCE = createDatabaseInstance(appContext);
                        
                    } catch (Exception e) {
                        android.util.Log.e("AppDatabase", "Database creation failed, attempting recovery", e);
                        
                        // Try to delete corrupted database and retry once
                        try {
                            Context appContext = context.getApplicationContext();
                            appContext.deleteDatabase("eliptv_database");
                            android.util.Log.i("AppDatabase", "Old database deleted, retrying...");
                            INSTANCE = createDatabaseInstance(appContext);
                        } catch (Exception e2) {
                            android.util.Log.e("AppDatabase", "Database recovery failed - activating in-memory fallback", e2);
                            DATABASE_AVAILABLE = false;
                            INSTANCE = null;
                            InMemoryChannelStore.getInstance().activate();
                            throw new RuntimeException("Failed to create database after recovery attempt: " + e2.getMessage(), e2);
                        }
                    }
                }
            }
        }
        return INSTANCE;
    }
    
    private static AppDatabase createDatabaseInstance(Context appContext) {
        // TV-specific database configuration
        // Use AUTOMATIC journal mode - Room will pick best mode for the device
        AppDatabase db = Room.databaseBuilder(appContext,
                        AppDatabase.class, "eliptv_database")
                .fallbackToDestructiveMigration()
                // Allow main thread queries temporarily - needed for TV devices
                // that have issues with background thread initialization
                .allowMainThreadQueries()
                .build();
        
        android.util.Log.i("AppDatabase", "Database instance created successfully");
        return db;
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
