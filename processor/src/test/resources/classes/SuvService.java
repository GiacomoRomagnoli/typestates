public class SuvService extends CarService {
    @Override
    Car start(@Requires({"OFF"}) Car car) {
        car.turnOn();
        return car;
    }
}