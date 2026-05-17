import java.util.Random;

@Typestate("protocols/car.protocol")
public class Car {
    private Random rand = new Random();
    boolean start() {
        return rand.nextBoolean();
    }
    void stop() {
        System.out.println("Stopping");
    }
}