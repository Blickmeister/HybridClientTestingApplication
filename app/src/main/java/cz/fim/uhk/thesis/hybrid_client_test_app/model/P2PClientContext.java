package cz.fim.uhk.thesis.hybrid_client_test_app.model;

import java.io.Serializable;

public class P2PClientContext implements Serializable {
    private double temperature;

    private double pressure;

    private double latitude;

    private double longitude;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
