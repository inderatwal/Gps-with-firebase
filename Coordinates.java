package com.tt.jattkaim.helprr;

/**
 * Created by jattkaim on 4/08/16.
 */
public class Coordinates {

    private String id;
    private double lat;
    private double lng;
    private String photo;
    private Details details;

    public Coordinates(){

    }

    public Coordinates(String id, Details details, double lat, double lng, String photo){
        this.details=details;
        this.lat=lat;
        this.lng=lng;
        this.photo=photo;
        this.id=id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public void setDetails(Details details) {
        this.details = details;
    }

    public Details getDetails() {
        return details;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }


    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }


}
