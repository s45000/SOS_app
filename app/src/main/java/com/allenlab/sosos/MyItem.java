package com.allenlab.sosos;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MyItem implements ClusterItem {
    private final LatLng position;
    private final String title;

    public MyItem(double lat, double lng, String title) {
        position = new LatLng(lat, lng);
        this.title = title;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return null;
    }

}
