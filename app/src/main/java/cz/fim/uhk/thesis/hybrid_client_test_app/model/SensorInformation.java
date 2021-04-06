package cz.fim.uhk.thesis.hybrid_client_test_app.model;

import java.io.Serializable;

public class SensorInformation implements Serializable {

    private double temperature;

    private double pressure;

    private User user;

    public SensorInformation(double temperature, double pressure) {
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public User getUser() {
        return user;
    }
}
