package cz.fim.uhk.thesis.hybrid_client_test_app.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Modelová třída hybridního klienta (zároveň i klienta v rámci IS)
 */
public class User implements Serializable {

    private String ssid;

    private double latitude;

    private double longitude;

    private boolean isOnline;

    private String actualState;

    private String futureState;

    private Date firstConnectionToServer;

    private Date lastConnectionToServer;

    private SensorInformation sensorInformation;

    public User() {}

    public User(String ssid, double latitude, double longitude, boolean isOnline, String actualState,
                String futureState, Date firstConnectionToServer, Date lastConnectionToServer,
                SensorInformation sensorInformation) {
        this.ssid = ssid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isOnline = isOnline;
        this.actualState = actualState;
        this.futureState = futureState;
        this.firstConnectionToServer = firstConnectionToServer;
        this.lastConnectionToServer = lastConnectionToServer;
        this.sensorInformation = sensorInformation;
    }

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

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setActualState(String actualState) {
        this.actualState = actualState;
    }

    public void setFutureState(String futureState) {
        this.futureState = futureState;
    }

    public void setFirstConnectionToServer(Date firstConnectionToServer) {
        this.firstConnectionToServer = firstConnectionToServer;
    }

    public void setLastConnectionToServer(Date lastConnectionToServer) {
        this.lastConnectionToServer = lastConnectionToServer;
    }

    public void setSensorInformation(SensorInformation sensorInformation) {
        this.sensorInformation = sensorInformation;
    }
}
