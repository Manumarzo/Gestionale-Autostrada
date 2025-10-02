package org.example.keycloak;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Auth {

    public static void requireRole(Context ctx, String requiredRole) {
        List<String> roles = extractRoles(ctx);
        if (roles == null || !roles.contains(requiredRole)) {
            throw new ForbiddenResponse("Accesso negato: ruolo richiesto \"" + requiredRole + "\".");
        }
    }

    public static void requireAnyRole(Context ctx, String... allowedRoles) {
        List<String> roles = extractRoles(ctx);
        if (roles == null) {
            throw new ForbiddenResponse("Accesso negato: nessun ruolo presente.");
        }
        for (String role : allowedRoles) {
            System.out.println(role + " " + roles.contains(role));
            if (roles.contains(role)) {
                return;
            }
        }
        throw new ForbiddenResponse("Accesso negato: nessuno dei ruoli richiesti presente.");
    }

    @SuppressWarnings("unchecked")
    public static List<String> extractRoles(Context ctx) {
        try {
            Map<String, Object> accessToken = ctx.attribute("access_token");
            if (accessToken == null) return null;

            List<String> roles = new ArrayList<>();

            Map<String, Object> realmAccess = (Map<String, Object>) accessToken.get("realm_access");
            if (realmAccess != null) {
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                if (realmRoles != null) {
                    roles.addAll(realmRoles);
                }
            }

            Map<String, Object> resourceAccess = (Map<String, Object>) accessToken.get("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("autostradaPissir");
                if (clientAccess != null) {
                    List<String> clientRoles = (List<String>) clientAccess.get("roles");
                    if (clientRoles != null) {
                        roles.addAll(clientRoles);
                    }
                }
            }

            return roles.isEmpty() ? null : roles;
        } catch (Exception e) {
            return null;
        }
    }
}
