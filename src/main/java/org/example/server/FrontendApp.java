package org.example.server;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.InputStream;
import java.util.Objects;

public class FrontendApp {
    public static final int PORT = 8081;

    public static void start() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/frontend", Location.CLASSPATH);
        }).start(PORT);

        // Rotta principale che reindirizza a index.html
        app.get("/", ctx -> {
            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/index.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });

        // Definizione delle rotte che reindirizzano alle pagine HTML corrispondenti
        app.get("/caselli", ctx -> {

            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/caselli.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });

        app.get("/tratte", ctx -> {
            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/tratte.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });

        app.get("/guadagni", ctx -> {
            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/guadagni.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });

        app.get("/telepass", ctx -> {
            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/pagamenti_telepass.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });

        app.get("/multe", ctx -> {
            InputStream htmlFile = FrontendApp.class.getResourceAsStream("/frontend/multe.html");
            ctx.result(Objects.requireNonNull(htmlFile)).contentType("text/html");
        });
    }
}

