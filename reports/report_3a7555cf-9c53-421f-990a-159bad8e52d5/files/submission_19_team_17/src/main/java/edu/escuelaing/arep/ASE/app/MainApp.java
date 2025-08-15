package edu.escuelaing.arep.ASE.app;


import edu.escuelaing.arep.ASE.app.routes.RouteManager;

import java.io.IOException;

public class MainApp {
    public static void main(String[] args) {
        StaticFiles.setLocation("src/main/resources/");

        RouteManager.get("/hello", (req, res) -> "Hello " + req.getValues("name"));
        RouteManager.get("/pi", (req, res) -> String.valueOf(Math.PI));

        try {
            WebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
