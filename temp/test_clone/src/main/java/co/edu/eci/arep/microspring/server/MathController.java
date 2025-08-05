package co.edu.eci.arep.microspring.server;

@RestController
public class MathController {
    @GetMapping("/e")
    public static String e(String nousada) {
        return Double.toString(Math.E);
    }

}
