// auth.js
"use strict";

const keycloak = new Keycloak({
    url: 'http://localhost:8080/auth',
    realm: 'AutostradaPISSIR',
    clientId: 'autostradaPissir'
});

// Rendi keycloak globale per essere accessibile dagli altri script
window.keycloak = keycloak;

document.addEventListener("DOMContentLoaded", () => {
    keycloak.init({ onLoad: 'login-required' }).then(authenticated => {
        if (authenticated) {
            console.log("Keycloak inizializzato e utente autenticato.");
            // Una volta che l'autenticazione è confermata, invia un evento
            document.dispatchEvent(new Event('keycloakReady'));
        } else {
            // Se per qualche motivo l'utente non è autenticato, reindirizza al login.
            keycloak.login();
        }
    }).catch(error => {
        console.error("Errore critico di Keycloak:", error);
        document.body.innerHTML = `<div class="alert alert-danger m-4"><strong>Errore di connessione:</strong> Impossibile inizializzare il servizio di autenticazione. Controlla la console per dettagli.</div>`;
    });
});