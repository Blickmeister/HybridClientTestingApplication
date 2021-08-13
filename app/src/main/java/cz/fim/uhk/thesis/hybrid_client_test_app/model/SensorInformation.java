package cz.fim.uhk.thesis.hybrid_client_test_app.model;

import java.io.Serializable;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Modelová třída pro data ze senzorů hybridního klienta (zároveň i klienta v rámci IS)
 */
public class SensorInformation implements Serializable {

    private double temperature;

    private double pressure;

    private User user;

    public SensorInformation(double temperature, double pressure) {
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public SensorInformation() {}

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public void setUser(User user) { this.user = user; }

    public User getUser() {
        return user;
    }
}
