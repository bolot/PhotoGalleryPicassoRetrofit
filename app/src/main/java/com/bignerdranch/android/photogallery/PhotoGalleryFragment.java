package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

import com.squareup.picasso.Picasso;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PhotoGalleryFragment extends VisibleFragment {
    GridView mGridView;
    List<GalleryItem> mItems;
    private FlickrService mFlickrService;
    private Callback<FlickrService.FlickrResponse> mFlickrResponseCallback = new Callback<FlickrService.FlickrResponse>() {
        @Override
        public void success(FlickrService.FlickrResponse flickrResponse, Response response) {
            mItems = flickrResponse.getPhotos();

            if (mItems.size() > 0) {
                String resultId = mItems.get(0).getId();
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId)
                        .commit();
            }

            setupAdapter();
        }

        @Override
        public void failure(RetrofitError error) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mFlickrService = FlickrService.Factory.create();
        updateItems();
    }
    
    public void updateItems() {
        String query = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
        if (query != null) {
            mFlickrService.search(query, mFlickrResponseCallback);
        } else {
            mFlickrService.fetchItems(mFlickrResponseCallback);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        
        mGridView = (GridView)v.findViewById(R.id.gridView);
        
        setupAdapter();
        
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> gridView, View view, int pos,
                    long id) {
                GalleryItem item = mItems.get(pos);
                
                Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
                Intent i = new Intent(getActivity(), PhotoPageActivity.class);
                i.setData(photoPageUri);
                
                startActivity(i);
            }
        });
        
        return v;
    }

    @Override
    @TargetApi(11)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // pull out the SearchView
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView)searchItem.getActionView();

            // get the data from our searchable.xml as a SearchableInfo
            SearchManager searchManager = (SearchManager)getActivity()
                .getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchInfo = searchManager.getSearchableInfo(name);

            searchView.setSearchableInfo(searchInfo);
        }
    }

    @Override
    @TargetApi(11)
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                    .commit();
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    getActivity().invalidateOptionsMenu();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    void setupAdapter() {
        if (getActivity() == null || mGridView == null) return;
        
        if (mItems != null) {
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(List<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.gallery_item, parent, false);
            }
            
            GalleryItem item = getItem(position);
            ImageView imageView = (ImageView)convertView
                    .findViewById(R.id.gallery_item_imageView);
            Picasso.with(getContext())
                    .load(item.getUrl())
                    .placeholder(R.drawable.brian_up_close)
                    .into(imageView);

            return convertView;
        }
    }
}
