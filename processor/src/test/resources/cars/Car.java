import annotations.Typestate;

import java.util.Random;

@Typestate("cars/car.protocol")
public class Car {
    public boolean turnOn() {
        return true;
    }

    public void setSpeed(int speed) {}

    public void turnOff() {}
}