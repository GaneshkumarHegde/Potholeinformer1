package com.project.hackathon.Potholeinformer;


public class coordinate {
    //an object we are going to use while using firebase
    public int id;
    public String value;
    public double lattitude,longitude;
    public long timestamp;
    public String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLattitude() {
        return lattitude;
    }

    public void setLattitude(float lattitude) {
        this.lattitude = lattitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public coordinate( long timestamp, String value,int id, double lattitude, double longitude,String key) {

        this.key = key;
        this.id = id;
        this.value = value;
        this.lattitude = lattitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public coordinate() {
    }

    public String getValue() {

        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
