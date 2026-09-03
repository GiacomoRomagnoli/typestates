import annotations.Typestate;

@Typestate("cars/suv.protocol")
public class Suv extends Car {

    public Mode switchMode() {
        return Mode.SPORT;
    }

    public void setEcoDrive(boolean state) {}

    public void setFourWheels(boolean state) {}

    @Override
    public void setSpeed(int speed) {}
}