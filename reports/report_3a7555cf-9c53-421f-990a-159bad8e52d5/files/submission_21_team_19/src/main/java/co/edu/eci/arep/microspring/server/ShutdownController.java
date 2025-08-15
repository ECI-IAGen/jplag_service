package co.edu.eci.arep.microspring.server;

import co.edu.eci.arep.microspring.server.GetMapping;
import co.edu.eci.arep.microspring.server.RestController;

@RestController
public class ShutdownController {

    @GetMapping("/shutdown")
    public String shutdown() {
        new Thread(() -> MicroServer.gracefulShutdown()).start();
        return "Apagando el servidor...";
    }
}