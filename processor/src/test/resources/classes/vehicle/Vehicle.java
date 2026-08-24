import annotations.Typestate;

@Typestate("protocols/vehicle.protocol")
public class Vehicle {
    int speed;
    boolean powered;

    Vehicle(int speed) {
        this.speed = speed;
        this.powered = false;
    }

    void start() {
        this.powered = true;
    }

    void stop() {
        this.powered = false;
    }

    void setSpeed(int speed) {
        this.speed = speed;
    }
}