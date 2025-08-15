package edu.escuelaing.arep.ASE.app.routes;

import edu.escuelaing.arep.ASE.app.http.Request;
import edu.escuelaing.arep.ASE.app.http.Response;

@FunctionalInterface
public interface Route {
    String handle(Request request, Response response);
}
