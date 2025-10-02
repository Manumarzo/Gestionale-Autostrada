"use strict";

var isAdmin = false;
let tutteLeTratte = []; // Array per memorizzare tutte le tratte

/**
 * Funzione principale che viene eseguita solo quando l'intera pagina HTML è pronta.
 */
document.addEventListener('DOMContentLoaded', () => {

    // Attendiamo che anche Keycloak sia pronto
    document.addEventListener('keycloakReady', () => {
        fetchTratte();
        initAdminFeatures();
    });

    // Listener per i campi di ricerca
    const searchIngresso = document.getElementById('searchIngressoInput');
    const searchUscita = document.getElementById('searchUscitaInput');
    searchIngresso.addEventListener('keyup', filtraTratte);
    searchUscita.addEventListener('keyup', filtraTratte);

    // Listener per il pulsante "Ripulisci"
    const clearButton = document.getElementById('clearSearchButton');
    clearButton.addEventListener('click', () => {
        searchIngresso.value = '';
        searchUscita.value = '';
        renderTratte(tutteLeTratte);
    });

    // Listener per l'invio del form di modifica.
    document.getElementById("modificaPedaggioForm").addEventListener("submit", function(e) {
        e.preventDefault();
        const trattaId = document.getElementById('modifica-tratta-id').value;
        const nuovoPedaggio = parseFloat(document.getElementById('modifica-pedaggio').value);

        if (isNaN(nuovoPedaggio) || nuovoPedaggio < 0) {
            alert("Inserisci un valore di pedaggio valido.");
            return;
        }

        window.keycloak.updateToken(5).then(() => {
            fetch(`http://localhost:3000/tratte/${trattaId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${window.keycloak.token}`
                },
                body: JSON.stringify({ pedaggio: nuovoPedaggio })
            })
                .then(async res => {
                    if (!res.ok) {
                        throw new Error(`HTTP ${res.status} - ${await res.text()}`);
                    }
                    alert("Pedaggio modificato con successo!");
                    location.reload();
                })
                .catch(err => {
                    console.error("Errore durante la modifica del pedaggio:", err);
                    alert("Errore durante la modifica: " + err.message);
                });
        }).catch(() => alert("Sessione scaduta. Ricarica la pagina."));
    });
});


/**
 * Funzione che filtra le tratte in base al contenuto dei campi.
 */
function filtraTratte() {
    const ingressoTerm = document.getElementById('searchIngressoInput').value.toLowerCase().trim();
    const uscitaTerm = document.getElementById('searchUscitaInput').value.toLowerCase().trim();

    const tratteFiltrate = tutteLeTratte.filter(tratta => {
        const matchIngresso = tratta.nomeCaselloIngresso.toLowerCase().includes(ingressoTerm);
        const matchUscita = tratta.nomeCaselloUscita.toLowerCase().includes(uscitaTerm);
        return matchIngresso && matchUscita;
    });

    renderTratte(tratteFiltrate);
}

/**
 * Recupera le tratte dal server e le renderizza.
 */
function fetchTratte() {
    if (!window.keycloak) return;
    fetch("http://localhost:3000/tratte", {
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => {
            if (!res.ok) throw new Error("Errore HTTP " + res.status);
            return res.json();
        })
        .then(data => {
            tutteLeTratte = data;
            renderTratte(tutteLeTratte);
        })
        .catch(error => {
            console.error("Errore durante il fetch delle tratte:", error);
            document.querySelector("#tratteTableBody").innerHTML = `<tr><td colspan="6" class="text-center text-danger">Errore nel caricamento dei dati.</td></tr>`;
        });
}

/**
 * Renderizza le righe della tabella.
 */
function renderTratte(tratte) {
    const tableBody = document.querySelector("#tratteTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = "";

    if (isAdmin && !document.querySelector("#actionsHeaderTratte")) {
        document.querySelector("#tratteTable thead tr").innerHTML += '<th id="actionsHeaderTratte">Azioni</th>';
    }

    if (tratte.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center">Nessuna tratta trovata.</td></tr>`;
        return;
    }

    tratte.forEach(tratta => {
        const row = tableBody.insertRow();
        row.innerHTML = `
            <td>${tratta.nomeCaselloIngresso}</td>
            <td>${tratta.nomeCaselloUscita}</td>
            <td>${tratta.distanza.toFixed(2)}</td>
            <td>${tratta.pedaggio.toFixed(2)}</td>
            <td>${tratta.utilizzo}</td>
        `;

        if (isAdmin) {
            const cell = row.insertCell();
            cell.innerHTML = `<button class="btn btn-warning btn-sm" onClick='apriModaleModifica("${tratta.id}", ${tratta.pedaggio})'>Modifica Pedaggio</button>`;
        }
    });
}

/**
 * Controlla se l'utente è amministratore.
 */
function initAdminFeatures() {
    fetch("http://localhost:3000/ruoli", {
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => res.json())
        .then(data => {
            if (data.includes("amministratore")) {
                isAdmin = true;
                renderTratte(tutteLeTratte);
            }
        })
        .catch(error => console.error("Errore durante il fetch dei ruoli:", error));
}

/**
 * Apre il modale per modificare il pedaggio. QUESTA FUNZIONE È CORRETTA.
 */
function apriModaleModifica(trattaId, pedaggioAttuale) {
    document.getElementById('modifica-tratta-id').value = trattaId;
    document.getElementById('modifica-pedaggio').value = pedaggioAttuale.toFixed(2);
    const modal = new bootstrap.Modal(document.getElementById('modificaPedaggioModal'));
    modal.show(); [1]
}