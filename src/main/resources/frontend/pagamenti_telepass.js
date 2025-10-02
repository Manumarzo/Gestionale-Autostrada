"use strict";

var isAdmin = false;
let pagamenti = []; //Array per memorizzare tutti i pagamenti

document.addEventListener('keycloakReady', () => {
    init();
    fetchPagamenti();
});

/**
 * Recupera i dati dei pagamenti e li memorizza.
 */
function fetchPagamenti() {
    if (!window.keycloak) {
        console.error("Keycloak non inizializzato");
        return;
    }

    fetch("http://localhost:3000/telepass", {
        method: "GET",
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => {
            if (!res.ok) throw new Error("HTTP error " + res.status);
            return res.json();
        })
        .then(data => {
            pagamenti = data;
            renderPagamenti(pagamenti);
        })
        .catch(error => {
            console.error("Errore durante il fetch dei pagamenti:", error);
            document.querySelector("#pagamentiTableBody").innerHTML =
                `<tr><td colspan="5" class="text-center text-danger">Errore nel caricamento dei dati.</td></tr>`;
        });
}

/**
 * Renderizza la tabella dei pagamenti.
 * @param {Array} pagamenti - La lista di pagamenti da visualizzare.
 */
function renderPagamenti(pagamenti) {
    const tableBody = document.querySelector("#pagamentiTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = "";

    if (isAdmin && !document.querySelector("#actionsHeader")) {
        document.querySelector("#pagamentiTable thead tr").innerHTML += '<th id="actionsHeader">Azioni</th>';
    }

    if (pagamenti.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center">Nessun pagamento trovato.</td></tr>`;
        return;
    }
    let count = 0;
    pagamenti.forEach(pagamento => {
        const row = tableBody.insertRow();
        row.id = `${pagamento.id}`;
        row.innerHTML = `
            <td>${pagamento.tratta}</td>
            <td>${pagamento.targa}</td>
            <td>${pagamento.importo.toFixed(2)}€</td>
            <td>${pagamento.pagato}</td>
        `;
        if (isAdmin) {
            const cell = row.insertCell();
            if(!pagamento.pagato) cell.innerHTML = `
                <button class="btn btn-danger btn-sm" onClick='riscuotiPagamento("${row.id}")'>Riscuoti</button>
            `;
        }
    });
}

/**
 * Inizializza la pagina per l'admin.
 */
function init() {
    fetch("http://localhost:3000/ruoli", {
        headers: { Authorization: `Bearer ${window.keycloak.token}` }
    })
        .then(res => res.json())
        .then(data => {
            if (data.includes("amministratore")) {
                isAdmin = true;
                document.getElementById("riscuotiPagamentiButton").addEventListener("click", () => {
                    riscuotiPagamento(pagamenti.filter( pagamento => !pagamento.pagato))
                });
            }
        })
        .catch(error => console.error("Errore durante il fetch dei ruoli ", error));
}

/**
 * Riscuoti un pagamento telepass
 */
function riscuotiPagamento(pagamenti) {
    const isArray = Array.isArray(pagamenti);
    const ids = isArray ? pagamenti.map(p => p.id) : [pagamenti];

    if (!confirm(`Vuoi riscuotere ${ids.length} pagamento/i?`)) return;

    const url = isArray ? "http://localhost:3000/telepass" : `http://localhost:3000/telepass/${ids[0]}`;
    const options = {
        method: "PUT",
        headers: {
            Authorization: `Bearer ${window.keycloak.token}`
        }
    };

    if (isArray) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(ids);
    }

    fetch(url, options)
        .then(res => {
            if (!res.ok) throw new Error("Errore HTTP " + res.status);
            return res.json ? res.json() : res.text();
        })
        .then(() => {
            fetchPagamenti(); // Ricarica la tabella aggiornata
        })
        .catch(err => {
            console.error("Errore nella riscossione:", err);
        });
}
