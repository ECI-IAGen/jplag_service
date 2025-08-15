package edu.escuelaing.arep.ASE.app.routes;

import java.util.HashMap;
import java.util.Map;

public class RouteManager {
    private static Map<String, Route> routes = new HashMap<>();

    public static void get(String path, Route route) {
        routes.put(path, route);
    }

    public static Route getRoute(String path) {
        return routes.get(path);
    }

}
