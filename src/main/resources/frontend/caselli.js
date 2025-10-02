"use strict";

var mapInitialized = false;
var isAdmin = false;
let tuttiICaselli = []; //Array per memorizzare tutti i caselli

// Variabili per la mappa del modale
let modificaMap;
let modificaMarker;

/**
 * Punto di ingresso: attende che auth.js confermi l'autenticazione.
 */
document.addEventListener('keycloakReady', () => {
    fetchCaselli();
    initButtons();
});

/**
 * Funzione che filtra i caselli in base al contenuto dei campi di ricerca.
 */
function filtraCaselli() {
    const nomeTerm = document.getElementById('searchNomeInput').value.toLowerCase().trim();
    const regioneTerm = document.getElementById('searchRegioneInput').value.toLowerCase().trim();

    const caselliFiltrati = tuttiICaselli.filter(casello => {
        const matchNome = casello.nome.toLowerCase().includes(nomeTerm);
        const matchRegione = casello.regione.toLowerCase().includes(regioneTerm);
        return matchNome && matchRegione;
    });

    renderCaselli(caselliFiltrati);
}

/**
 * Recupera i dati dei caselli e li memorizza.
 */
function fetchCaselli() {
    if (!window.keycloak) {
        console.error("Keycloak non inizializzato");
        return;
    }

    fetch("http://localhost:3000/caselli", {
        method: "GET",
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => {
            if (!res.ok) throw new Error("HTTP error " + res.status);
            return res.json();
        })
        .then(data => {
            tuttiICaselli = data; // Memorizza la lista completa
            renderCaselli(tuttiICaselli); // Chiama la nuova funzione di rendering
        })
        .catch(error => {
            console.error("Errore durante il fetch dei caselli ", error);
            document.querySelector("#caselliTableBody").innerHTML = `<tr><td colspan="5" class="text-center text-danger">Errore nel caricamento dei dati.</td></tr>`;
        });
}

/**
 * Renderizza la tabella dei caselli.
 * @param {Array} caselli - La lista di caselli da visualizzare.
 */
function renderCaselli(caselli) {
    const tableBody = document.querySelector("#caselliTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = "";

    if (isAdmin && !document.querySelector("#actionsHeader")) {
        document.querySelector("#caselliTable thead tr").innerHTML += '<th id="actionsHeader">Azioni</th>';
    }

    if (caselli.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center">Nessun casello trovato.</td></tr>`;
        return;
    }

    caselli.forEach(casello => {
        const row = tableBody.insertRow();
        row.id = `casello-${casello.nome.replace(/\s+/g, '-')}`;
        row.innerHTML = `
            <td>${casello.nome}</td>
            <td>${casello.regione}</td>
            <td>${casello.latitudine}</td>
            <td>${casello.longitudine}</td>
        `;
        if (isAdmin) {
            const cell = row.insertCell();
            cell.innerHTML = `
                <button class="btn btn-warning btn-sm" onClick='apriModaleModifica("${casello.nome}", "${casello.regione}", ${casello.latitudine}, ${casello.longitudine})'>Modifica</button>
                <button class="btn btn-danger btn-sm" onClick='eliminaCasello("${row.id}")'>Elimina</button>
            `;
        }
    });
}


// Ascolta gli eventi del DOM per inizializzare listener
document.addEventListener('DOMContentLoaded', () => {
    // Listener per la ricerca
    const searchNome = document.getElementById('searchNomeInput');
    const searchRegione = document.getElementById('searchRegioneInput');
    const clearButton = document.getElementById('clearCaselliSearchButton');

    searchNome.addEventListener('keyup', filtraCaselli);
    searchRegione.addEventListener('keyup', filtraCaselli);
    clearButton.addEventListener('click', () => {
        searchNome.value = '';
        searchRegione.value = '';
        filtraCaselli(); // O renderCaselli(tuttiICaselli)
    });

    // Listener per il modale di modifica
    const modificaModalEl = document.getElementById('modificaCaselloModal');
    modificaModalEl.addEventListener('shown.bs.modal', function (event) {
        const lat = parseFloat(event.currentTarget.dataset.lat);
        const lng = parseFloat(event.currentTarget.dataset.lng);
        setupModificaMap(lat, lng);
    });

    // Listener per l'invio del form di modifica
    document.getElementById("modificaCaselloForm").addEventListener("submit", function(e) {
        e.preventDefault();
        const nomeOriginale = document.getElementById('modifica-id-originale').value;
        const formData = new FormData(this);
        formData.delete('id_originale');
        const casello = Object.fromEntries(formData.entries());

        window.keycloak.updateToken(5).then(() => {
            fetch(`http://localhost:3000/caselli/${encodeURIComponent(nomeOriginale)}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${window.keycloak.token}` },
                body: JSON.stringify(casello)
            })
                .then(async res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status} - ${await res.text()}`);
                    alert("Casello modificato con successo!");
                    location.reload();
                })
                .catch(err => {
                    console.error(err);
                    alert("Errore durante la modifica del casello: " + err.message);
                });
        }).catch(() => alert("Sessione scaduta. Ricarica la pagina."));
    });
});


/**
 * Apre il modale di modifica.
 */
function apriModaleModifica(nome, regione, lat, lng) {
    const modalElement = document.getElementById('modificaCaselloModal');
    modalElement.dataset.nome = nome;
    modalElement.dataset.regione = regione;
    modalElement.dataset.lat = lat;
    modalElement.dataset.lng = lng;

    document.getElementById('modifica-id-originale').value = nome;
    document.getElementById('modifica-nome').value = nome;
    document.getElementById('modifica-regione').value = regione;
    document.getElementById('modifica-lat').value = lat;
    document.getElementById('modifica-lng').value = lng;

    const myModal = new bootstrap.Modal(modalElement);
    myModal.show(); [1]
}

/**
 * Inizializza o aggiorna la mappa nel modale.
 */
function setupModificaMap(lat, lng) {
    if (!modificaMap) {
        modificaMap = L.map('map-modifica').setView([lat, lng], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(modificaMap);
        modificaMarker = L.marker([lat, lng]).addTo(modificaMap);
        modificaMap.on('click', function(e) {
            const { lat, lng } = e.latlng;
            document.getElementById('modifica-lat').value = lat.toFixed(6);
            document.getElementById('modifica-lng').value = lng.toFixed(6);
            modificaMarker.setLatLng(e.latlng);
        });
    } else {
        modificaMap.setView([lat, lng], 13);
        modificaMarker.setLatLng([lat, lng]);
    }
    setTimeout(() => modificaMap.invalidateSize(), 100);
}

/**
 * Inizializza i pulsanti per l'admin.
 */
function initButtons() {
    fetch("http://localhost:3000/ruoli", {
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => res.json())
        .then(data => {
            if (data.includes("amministratore")) {
                isAdmin = true;
                document.getElementById("adminButtons").style.display = "block";
                document.getElementById("aggiungiCaselloButton").addEventListener("click", toggleForm);
                renderCaselli(tuttiICaselli); // Ri-renderizza per mostrare la colonna azioni
            }
        })
        .catch(error => console.error("Errore durante il fetch dei ruoli ", error));
}

/**
 * Mostra/nasconde il form di aggiunta.
 */
function toggleForm() {
    const form = document.getElementById("aggiungiCaselloForm");
    form.style.display = form.style.display === "none" ? "block" : "none";
    if (form.style.display === "block" && !mapInitialized) {
        initForm();
    }
}

/**
 * Inizializza il form e la mappa di aggiunta.
 */
function initForm() {
    mapInitialized = true;
    const map = L.map('map').setView([41.9028, 12.4964], 6);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    let marker;
    map.on('click', function(e) {
        const { lat, lng } = e.latlng;
        document.getElementById('lat').value = lat.toFixed(6);
        document.getElementById('lng').value = lng.toFixed(6);
        marker ? marker.setLatLng(e.latlng) : marker = L.marker(e.latlng).addTo(map);
    });

    document.getElementById("caselloForm").addEventListener("submit", function(e) {
        e.preventDefault();
        const formData = new FormData(this);
        const casello = Object.fromEntries(formData.entries());

        window.keycloak.updateToken(5).then(() => {
            fetch(`http://localhost:3000/caselli`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${window.keycloak.token}` },
                body: JSON.stringify(casello)
            })
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status} - ${res.statusText}`);
                    alert("Casello aggiunto con successo!");
                    location.reload();
                })
                .catch(err => {
                    console.error(err);
                    alert("Errore durante l'invio del casello: " + err.message);
                });
        }).catch(() => alert("Sessione scaduta. Ricarica la pagina."));
    });
}

/**
 * Funzione di eliminazione.
 */
function eliminaCasello(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;

    const nomeCasello = row.children[0].textContent;
    if (!confirm(`Sei sicuro di voler eliminare il casello "${nomeCasello}" e tutte le tratte associate?`)) {
        return;
    }

    window.keycloak.updateToken(5).then(() => {
        fetch(`http://localhost:3000/caselli/${encodeURIComponent(nomeCasello)}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${window.keycloak.token}` }
        })
            .then(async res => {
                if (!res.ok) throw new Error(`HTTP ${res.status} - ${await res.text()}`);
                alert(`Casello "${nomeCasello}" eliminato con successo!`);
                location.reload(); // Ricarica per aggiornare anche le tratte, se mostrate altrove
            })
            .catch(err => {
                console.error("Errore durante l'eliminazione:", err);
                alert("Errore durante l'eliminazione del casello: " + err.message);
            });
    }).catch(() => alert("Sessione scaduta. Ricarica la pagina."));
}