import annotations.Typestate;

@Typestate("protocols/greeter.protocol")
public class Greeter {
    void sayHello(int value) {
        value = true;
    }
    void sayGoodbye() {

    }
}