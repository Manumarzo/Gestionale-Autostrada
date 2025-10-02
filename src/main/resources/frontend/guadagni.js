"use strict";

const API_BASE_URL = 'http://localhost:3000';

// Attende che l'autenticazione Keycloak sia pronta
document.addEventListener('keycloakReady', () => {
    if (!window.keycloak || !window.keycloak.authenticated) {
        console.error("Utente non autenticato.");
        document.body.innerHTML = '<h1>Accesso Richiesto</h1><p>Devi effettuare il login per vedere questa pagina.</p>';
        return;
    }

    // Controlla se l'utente ha il ruolo di amministratore
    if (!window.keycloak.hasRealmRole("amministratore")) {
        document.body.innerHTML = '<h1>Accesso Negato</h1><p>Non hai i permessi necessari per visualizzare questa pagina.</p><a href="/" class="btn btn-primary">Torna alla Home</a>';
        return;
    }

    // Se l'utente è un amministratore, procedi a caricare i dati
    console.log("Utente amministratore autenticato. Caricamento dati guadagni...");
    fetchGuadagnoTotale();
    populateCaselliDropdowns();
    setupFormListener();
});

/**
 * Funzione helper per le chiamate API, aggiunge automaticamente il token di autenticazione.
 * @param {string} endpoint - L'endpoint dell'API da chiamare (es. "/guadagni").
 * @returns {Promise<Response>} La risposta dalla fetch.
 */
function apiFetch(endpoint) {
    return fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + window.keycloak.token,
            'Content-Type': 'application/json'
        }
    });
}

/**
 * Recupera e visualizza il guadagno totale da tutti i viaggi.
 * Chiama l'endpoint GET /guadagni.
 */
async function fetchGuadagnoTotale() {
    const guadagnoContainer = document.getElementById("guadagno-totale");
    try {
        const response = await apiFetch('/guadagni');
        if (!response.ok) {
            throw new Error(`Errore dal server: ${response.statusText}`);
        }
        const data = await response.json();
        // Formatta il numero come valuta italiana
        guadagnoContainer.textContent = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(data.guadagnoTotale);
    } catch (error) {
        console.error("Errore nel recuperare il guadagno totale:", error);
        guadagnoContainer.textContent = "Errore nel caricamento.";
        guadagnoContainer.classList.add('text-danger');
    }
}

/**
 * Recupera la lista di tutti i caselli e popola i menu a tendina.
 * Chiama l'endpoint GET /caselli.
 */
async function populateCaselliDropdowns() {
    const ingressoSelect = document.getElementById('casello-ingresso');
    const uscitaSelect = document.getElementById('casello-uscita');

    try {
        const response = await apiFetch('/caselli');
        if (!response.ok) throw new Error('Errore nel recupero dei caselli');

        const caselli = await response.json();

        // Pulisce opzioni esistenti (tranne la prima disabilitata)
        ingressoSelect.innerHTML = '<option value="" selected disabled>Seleziona un casello...</option>';
        uscitaSelect.innerHTML = '<option value="" selected disabled>Seleziona un casello...</option>';

        caselli.forEach(casello => {
            const option = `<option value="${casello.nome}">${casello.nome}</option>`;
            ingressoSelect.innerHTML += option;
            uscitaSelect.innerHTML += option;
        });
    } catch (error) {
        console.error("Errore nel popolare i dropdown dei caselli:", error);
        // Potresti mostrare un errore all'utente qui
    }
}

/**
 * Imposta il listener per il form di ricerca della tratta.
 * Quando sottomesso, chiama l'endpoint GET /guadagni/{ingresso}/{uscita}.
 */
function setupFormListener() {
    const form = document.getElementById("ricerca-tratta-form");
    form.addEventListener("submit", async (event) => {
        event.preventDefault(); // Impedisce l'invio tradizionale del form
        event.stopPropagation(); // Ferma la propagazione per la validazione

        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        form.classList.add('was-validated');

        const caselloIngresso = document.getElementById("casello-ingresso").value;
        const caselloUscita = document.getElementById("casello-uscita").value;
        const risultatoContainer = document.getElementById("risultato-tratta");
        const guadagnoSpecificoP = document.getElementById("guadagno-tratta-specifica");

        guadagnoSpecificoP.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Ricerca...`;
        risultatoContainer.style.display = 'block';

        try {
            // encodeURIComponent per gestire nomi di caselli con caratteri speciali
            const url = `/guadagni/${encodeURIComponent(caselloIngresso)}/${encodeURIComponent(caselloUscita)}`;
            const response = await apiFetch(url);

            if (!response.ok) throw new Error(`Errore dal server: ${response.statusText}`);

            const data = await response.json();
            guadagnoSpecificoP.textContent = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(data.guadagnoTratta);
            guadagnoSpecificoP.classList.remove('text-danger');
            guadagnoSpecificoP.classList.add('text-success');

        } catch (error) {
            console.error("Errore nel recuperare il guadagno per la tratta:", error);
            guadagnoSpecificoP.textContent = "Dati non disponibili o tratta non trovata.";
            guadagnoSpecificoP.classList.add('text-danger');
        }
    });
}