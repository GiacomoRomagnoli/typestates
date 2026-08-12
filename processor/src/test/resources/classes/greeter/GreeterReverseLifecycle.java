public class GreeterReverseLifecycle {
    void spawnAndGreet() {
        Greeter greeter = new Greeter();
        greeter.sayGoodbye();
        greeter.sayHello();
    }
}