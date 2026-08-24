import annotations.Typestate;

@Typestate("protocols/car.protocol")
public class Car extends Vehicle {
    int inheritedSpeed = speed;
    boolean locked;

    Car() {
        super(0);
        this.locked = true;
    }

    Car(int speed) {
        super(speed);
        this.locked = true;
    }

    void lock() {
        this.locked = true;
    }
}