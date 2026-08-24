import annotations.Typestate;

@Typestate("protocols/electric-car.protocol")
public class ElectricCar extends Car {
    int inheritedAgain = inheritedSpeed;
    int charge;

    ElectricCar(int charge) {
        int copy = charge;
        this.charge = copy;
    }

    void recharge(int charge) {
        this.charge = charge;
    }
}