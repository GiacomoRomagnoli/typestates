import annotations.Typestate;

@Typestate("protocols/suv.protocol")
public class Suv extends Car {
    private Mode mode;
    private boolean ecoDrive;
    private boolean fourWheels;
    @Override
    boolean turnOn() {
        boolean result = super.turnOn();
        if(result) {
            this.mode = Mode.COMFORT;
        }
        return result;
    }
    Mode switchMode() {
        if(mode == Mode.COMFORT) {
            return Mode.SPORT;
        }
        return Mode.SPORT;
    }
    void setEcoDrive(boolean value) {
        this.ecoDrive = value;
    }
    void setFourWheels(boolean value) {
        this.fourWheels = value;
    }
}