package cz.fim.uhk.thesis.hybrid_client_test_app.model;

import java.util.Date;

public class User {

    private String ssid;

    private double latitude;

    private double longitude;

    private boolean isOnline;

    private String actualState;

    private String futureState;

    private Date firstConnectionToServer;

    private Date lastConnectionToServer;

    private SensorInformation sensorInformation;

    public String getSsid() {
        return ssid;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getActualState() {
        return actualState;
    }

    public String getFutureState() {
        return futureState;
    }

    public Date getFirstConnectionToServer() {
        return firstConnectionToServer;
    }

    public Date getLastConnectionToServer() {
        return lastConnectionToServer;
    }

    public SensorInformation getSensorInformation() {
        return sensorInformation;
    }
}
