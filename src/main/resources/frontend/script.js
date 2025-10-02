"use strict";

document.addEventListener('keycloakReady', () => {
    console.log("keycloakReady ricevuto su index.html, costruisco la pagina.");
    setupUI();
    createDashboardCards();
});

function setupUI() {
    const logoutButton = document.getElementById("logoutButton");
    const userInfo = document.getElementById("userInfo");

    if (window.keycloak && window.keycloak.authenticated) {
        logoutButton.addEventListener("click", () => window.keycloak.logout());

        window.keycloak.loadUserInfo().then(profile => {
            userInfo.textContent = `Benvenuto, ${profile.preferred_username}`;
        });
    }
}

function createDashboardCards() {
    const container = document.getElementById("dashboard-cards");
    if (!container) return; // Sicurezza

    container.innerHTML = "";
    const hasRole = (role) => window.keycloak.hasRealmRole(role);

    const cards = [
        { title: "Gestione Caselli", text: "Getisci i caselli autostradali.", href: "/caselli", roles: ["amministratore", "impiegato"] },
        { title: "Gestione Tratte", text: "Gestisci le tariffe delle tratte.", href: "/tratte", roles: ["amministratore", "impiegato"] },
        { title: "Report Guadagni", text: "Visualizza i report sui guadagni.", href: "/guadagni", roles: ["amministratore"] },
        { title: "Pagamenti Telepass", text: "Gestisci i pagamenti non riscossi.", href: "/telepass", roles: ["amministratore"] },
        { title: "Gestione Multe", text: "Cerca e visualizza le multe.", href: "/multe", roles: ["amministratore", "impiegato"] }
    ];

    cards.forEach(card => {
        if (card.roles.some(hasRole)) {
            const cardCol = document.createElement("div");
            cardCol.className = "col-md-6 col-lg-4";
            cardCol.innerHTML = `
                <div class="card h-100">
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title">${card.title}</h5>
                        <p class="card-text">${card.text}</p>
                        <a href="${card.href}" class="btn btn-primary mt-auto">Vai alla Sezione</a>
                    </div>
                </div>`;
            container.appendChild(cardCol);
        }
    });
}