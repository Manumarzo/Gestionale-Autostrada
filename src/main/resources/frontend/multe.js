"use strict";

var isAdmin = false;
let multe = []; //Array per memorizzare tutti i caselli


document.addEventListener('keycloakReady', () => {
    fetchMulte();
});

/**
 * Recupera i dati delle multe e li memorizza.
 */
function fetchMulte() {
    if (!window.keycloak) {
        console.error("Keycloak non inizializzato");
        return;
    }

    fetch("http://localhost:3000/multe", {
        method: "GET",
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => {
            if (!res.ok) throw new Error("HTTP error " + res.status);
            return res.json();
        })
        .then(data => {
            multe = data;
            renderMulte(multe);
        })
        .catch(error => {
            console.error("Errore durante il fetch delle multe ", error);
            document.querySelector("#caselliTableBody").innerHTML = `<tr><td colspan="5" class="text-center text-danger">Errore nel caricamento dei dati.</td></tr>`;
        });
}

/**
 * Renderizza la tabella delle multe
 * @param {Array} multe - La lista di multe da visualizzare.
 */
function renderMulte(multe) {
    const tableBody = document.querySelector("#multeTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = "";

    if (isAdmin && !document.querySelector("#actionsHeader")) {
        document.querySelector("#caselliTable thead tr").innerHTML += '<th id="actionsHeader">Azioni</th>';
    }

    if (multe.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center">Nessuna multa trovata.</td></tr>`;
        return;
    }

    multe.forEach(multa => {
        const row = tableBody.insertRow();
        //row.id = `multa-${multa.nome.replace(/\s+/g, '-')}`;
        row.innerHTML = `
            <td>${multa.nome}</td>
            <td>${multa.targa}</td>
            <td>${multa.motivazione}</td>
            <td>${multa.importo}€</td>
            <td>${new Date(multa.scadenza).toLocaleDateString("it-IT")}</td>
        `;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // Listener per la ricerca
    const searchTarga = document.getElementById('searchTargaInput');
    const clearButton = document.getElementById('clearTargaSearchButton');

    searchTarga.addEventListener('keyup', filtraMulte);
    clearButton.addEventListener('click', () => {
        searchTarga.value = '';
        filtraMulte();
    });
});

/**
 * Funzione che filtra le multe in base alla targa
 */
function filtraMulte() {
    const nomeTerm = document.getElementById('searchTargaInput').value.toLowerCase().trim();

    const multeFiltrate = multe.filter(multa => {
        return multa.targa.toLowerCase().includes(nomeTerm);
    });

    renderMulte(multeFiltrate);
}