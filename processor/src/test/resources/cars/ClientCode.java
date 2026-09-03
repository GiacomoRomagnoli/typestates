import annotations.Requires;

public class ClientCode {
    public static void example() {
        Car car = new Suv();

        while (!car.turnOn()) {
        }

        car.setSpeed(50);
        car.turnOff();
    }
}