package com.example.daobadat.androidmap.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by giau.tran on 19/01/2016.
 */
public class MyLocation extends RealmObject {

    @PrimaryKey
    private int locationId;

    private String name;
    private double lat;
    private double lng;

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public int getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}