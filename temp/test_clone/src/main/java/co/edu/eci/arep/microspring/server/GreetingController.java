package co.edu.eci.arep.microspring.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private static final AtomicLong counter = new AtomicLong();

    private static final Map<String, Greeting> greetingCache = new HashMap<>();

    @GetMapping("/greeting")
    public static Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        if (greetingCache.containsKey(name)) {
            return greetingCache.get(name);
        }

        long id = counter.incrementAndGet();
        Greeting newGreeting = new Greeting(id, String.format(template, name));
        greetingCache.put(name, newGreeting);
        return newGreeting;
    }
}
