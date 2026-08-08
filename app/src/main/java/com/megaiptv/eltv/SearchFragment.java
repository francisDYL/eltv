package com.megaiptv.eltv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private final ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
    private final Handler            mHandler     = new Handler(Looper.getMainLooper());
    private final ExecutorService    mExecutor    = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder ivh, Object item,
                                      RowPresenter.ViewHolder rvh, Row row) {
                if (item instanceof Channel) {
                    Intent intent = new Intent(requireActivity(), DetailsActivity.class);
                    intent.putExtra(DetailsActivity.CHANNEL, (Channel) item);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        search(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        search(query);
        return true;
    }

    private void search(String query) {
        mRowsAdapter.clear();
        if (query == null || query.trim().isEmpty()) return;

        mExecutor.execute(() -> {
            List<Channel> results;
            
            // Use in-memory fallback if database is unavailable
            if (InMemoryChannelStore.getInstance().isActive()) {
                results = InMemoryChannelStore.getInstance().searchChannels(query.trim());
            } else {
                try {
                    results = AppDatabase.getDatabase(requireContext())
                            .channelDao().searchChannels(query.trim());
                } catch (Exception e) {
                    android.util.Log.e("SearchFragment", "Search failed", e);
                    results = new java.util.ArrayList<>();
                }
            }
            
            List<Channel> finalResults = results;
            mHandler.post(() -> {
                mRowsAdapter.clear();
                if (!finalResults.isEmpty()) {
                    ArrayObjectAdapter listAdapter =
                            new ArrayObjectAdapter(new ChannelCardPresenter());
                    listAdapter.addAll(0, finalResults);
                    mRowsAdapter.add(new ListRow(
                            new HeaderItem(0, getString(R.string.search_results)), listAdapter));
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}

