import annotations.Requires;

public class CarService {
    @annotations.Ensures({"ON"})
    Car start(@Requires({"OFF"}) Car car) {
        while(!car.turnOn()) {
            System.out.println("turning on...");
        }
        car.setSpeed(50);
        return car;
    }

    @annotations.Ensures({"OFF"})
    Car stop(@Requires({"ON"}) Car car) {
        car.setSpeed(0);
        car.turnOff();
        return car;
    }
}