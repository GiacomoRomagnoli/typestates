import annotations.Typestate;

import java.util.Random;

@Typestate("protocols/car.protocol")
public class Car {
    int speed;
    private Random rand = new Random();
    boolean turnOn() {
        return rand.nextBoolean();
    }
    void turnOff() {
        System.out.println("Stopping");
    }
    void setSpeed(int speed) {
        this.speed = speed;
    }
}