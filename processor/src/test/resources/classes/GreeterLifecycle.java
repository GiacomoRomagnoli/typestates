public class GreeterLifecycle {
    void spawnAndGreet() {
        Greeter greeter = new Greeter();
        greeter.sayHello();
        greeter.sayGoodby();
    }
}