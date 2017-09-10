package com.gianlu.aria2app.Activities;

import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.gianlu.aria2app.Activities.Search.SearchEngine;
import com.gianlu.aria2app.Activities.Search.SearchResult;
import com.gianlu.aria2app.Activities.Search.SearchUtils;
import com.gianlu.aria2app.Activities.Search.Torrent;
import com.gianlu.aria2app.Adapters.SearchResultsAdapter;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import java.util.Collections;
import java.util.List;

// TODO: Should have tutorial (?)
public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener, SearchUtils.ISearch, SearchResultsAdapter.IAdapter, SearchUtils.ITorrent {
    private RecyclerView list;
    private ProgressBar loading;
    private CoordinatorLayout layout;
    private LinearLayout message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle(R.string.searchTorrent);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        layout = findViewById(R.id.search);
        loading = findViewById(R.id.search_loading);
        message = findViewById(R.id.search_message);
        list = findViewById(R.id.search_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Button messageMore = findViewById(R.id.search_messageMore);
        messageMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://gianlu.xyz/torrent-search-engine/")));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search_search).getActionView();

        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        loading.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        MessageLayout.hide(layout);

        SearchUtils.get(this).search(query.trim(), 10, null, this); // TODO: Select engines to query
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onClose() {
        message.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        MessageLayout.hide(layout);
        return false;
    }

    @Override
    public void onResult(List<SearchResult> results, List<SearchEngine> missingEngines) { // TODO: Notify missing engines
        loading.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);
        MessageLayout.hide(layout);

        list.setAdapter(new SearchResultsAdapter(this, results, this));
    }

    @Override
    public void serviceUnavailable() {
        loading.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        MessageLayout.show(layout, getString(R.string.searchEngine_offline), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void onDone(Torrent torrent) {
        showTorrentDialog(torrent);
    }

    @Override
    public void onTorrentException(Exception ex) {
        Toaster.show(this, Utils.Messages.FAILED_LOADING, ex);
    }

    @Override
    public void onException(Exception ex) {
        loading.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getLocalizedMessage()), R.drawable.ic_error_outline_black_48dp);
        Logging.logMe(this, ex);
    }

    @Override
    public void onResultSelected(SearchResult result) {
        SearchUtils.get(this).getTorrent(result, this);
    }

    private void showTorrentDialog(final Torrent torrent) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.torrent_dialog, null, false);
        SuperTextView size = layout.findViewById(R.id.torrentDialog_size);
        SuperTextView seeders = layout.findViewById(R.id.torrentDialog_seeders);
        SuperTextView leeches = layout.findViewById(R.id.torrentDialog_leeches);

        size.setHtml(R.string.size, CommonUtils.dimensionFormatter(torrent.size, false));
        seeders.setHtml(R.string.numSeeder, torrent.seeders);
        leeches.setHtml(R.string.numLeeches, torrent.leeches);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(torrent.title)
                .setView(layout)
                .setPositiveButton(R.string.download, null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        JTA2 jta2;
                        try {
                            jta2 = JTA2.instantiate(SearchActivity.this);
                        } catch (JTA2InitializingException ex) {
                            Toaster.show(SearchActivity.this, Utils.Messages.FAILED_ADD_DOWNLOAD, ex);
                            return;
                        }

                        jta2.addUri(Collections.singletonList(torrent.magnet), null, null, new JTA2.IGID() {
                            @Override
                            public void onGID(String gid) {
                                dialogInterface.dismiss();

                                Snackbar.make(SearchActivity.this.layout, R.string.downloadAdded, Snackbar.LENGTH_SHORT)
                                        .setAction(R.string.show, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                startActivity(new Intent(SearchActivity.this, MainActivity.class));
                                            }
                                        }).show();
                            }

                            @Override
                            public void onException(Exception ex) {
                                Toaster.show(SearchActivity.this, Utils.Messages.FAILED_ADD_DOWNLOAD, ex);
                            }
                        });
                    }
                });
            }
        });

        CommonUtils.showDialog(this, dialog);

        // TODO: Open in browser
        // TODO: Download torrent file
        // TODO: Copy/share magnet
    }
}
