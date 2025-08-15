package org.example;


import edu.escuelaing.arep.ASE.app.http.Request;
import edu.escuelaing.arep.ASE.app.routes.Route;
import edu.escuelaing.arep.ASE.app.routes.RouteManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class RouteManagerTest {

    @Test
    public void testGetRoute() {
        Route testRoute = (request, response) -> "Test Response";
        RouteManager.get("/test", testRoute);
        Route route = RouteManager.getRoute("/test");
        assertNotNull(route);
        assertEquals("Test Response", route.handle(new Request("GET", "/test"), null));
    }
}