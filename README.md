# PISSIR - Sistema di Gestione Autostradale

Progetto sviluppato da **Emanuele Marzone** e **Samuel Titonel**.

## 🎯 Obiettivo del Progetto

**PISSIR** (Progettazione e Implementazione Software e di Servizi In Rete) è un sistema che modella e simula la gestione di una rete autostradale. L'architettura è basata su componenti distribuiti (microservizi) che comunicano in modo asincrono tramite un broker **MQTT**, garantendo modularità e scalabilità.

Il progetto include due parti principali:
1.  **La simulazione dei dispositivi di campo**: caselli manuali e automatici, telecamere e sbarre che interagiscono con i veicoli.
2.  **Un'applicazione gestionale web**: un pannello di controllo per amministratori e impiegati per monitorare e gestire il sistema (caselli, tariffe, passaggi, ecc.).

## ✨ Funzionalità Principali

### Simulazione Autostradale
*   **Gestione Ingressi**: Simulazione di richieste di ingresso manuali (con erogazione biglietto) e automatiche (tramite Telepass).
*   **Gestione Uscite**: Simulazione di uscite manuali con calcolo del pedaggio e pagamento, e uscite automatiche con addebito Telepass.
*   **Logica Centrale**: Un server centrale gestisce la logica di business, calcola le tratte, i pedaggi e gestisce eventuali guasti o incongruenze.

### Pannello Gestionale
*   **Consultazione Dati**: Visualizzazione di caselli, tariffe, passaggi registrati, multe e guadagni.
*   **Gestione Amministrativa**:
    *   Aggiunta, modifica e rimozione di caselli.
    *   Modifica delle tariffe autostradali.
    *   Gestione dei pagamenti Telepass (riscossione di pagamenti in sospeso).
*   **Controllo Accessi Basato su Ruoli**:
    *   **Amministratore**: Accesso completo a tutte le funzionalità di visualizzazione e modifica.
    *   **Impiegato**: Accesso in sola lettura per la consultazione di dati e statistiche.

## 🛠️ Architettura e Stack Tecnologico

Il sistema è progettato come un insieme di microservizi che comunicano tramite topic MQTT.

*   **Linguaggi**: **Java** (backend e simulazione), **HTML/CSS/JavaScript** (frontend gestionale).
*   **Comunicazione**: **MQTT** (broker **Mosquitto**) con comunicazione sicura tramite **TLS**.
*   **Backend Gestionale**: API REST sviluppate con il framework **Javalin**.
*   **Autenticazione**: **Keycloak** per la gestione di utenti, ruoli e autenticazione sul pannello gestionale.
*   **Database**: Simulato tramite file **JSON** per la persistenza dei dati (caselli, tratte, veicoli in transito, ecc.).
*   **Testing**: Test automatici con **JUnit5** per le classi DAO.

### Componenti Principali
*   **Casello (Manuale/Automatico)**: Simula l'interazione con il veicolo.
*   **Telecamera**: Simula la lettura della targa.
*   **Sbarra**: Simula l'apertura/chiusura del passaggio.
*   **Server Centrale**: Orchestratore del sistema, gestisce la logica e espone le API REST.
*   **Broker MQTT**: Gestisce la comunicazione tra tutti i componenti.
*   **Software Utente (Gestionale)**: Interfaccia web per l'interazione con il sistema.

## 🚀 Installazione e Avvio

Per eseguire il progetto, è necessario configurare l'ambiente come segue.

### Prerequisiti
1.  **Java**
2.  **Mosquitto MQTT Broker**
3.  **Keycloak v12.0.4** o superiore

### 1. Configurazione Mosquitto
Configurare il broker Mosquitto per utilizzare la comunicazione sicura TLS e l'autenticazione. Modificare il file `mosquitto.conf` per:
*   Ascoltare sulla porta `8883`.
*   Disabilitare le connessioni anonime (`allow_anonymous false`).
*   Specificare i percorsi per i certificati CA (`cafile`), del server (`certfile`, `keyfile`) e il file delle password (`password_file`).

### 2. Configurazione Keycloak
1.  Avviare Keycloak tramite lo script `standalone.bat` (Windows) o `standalone.sh` (Linux/macOS).
2.  Accedere al pannello di amministrazione su `http://localhost:8080`.
3.  Creare un nuovo **realm** chiamato `AutostradaPISSIR`.
4.  All'interno del realm, creare un nuovo **client** con ID `autostradaPissir`.
5.  Impostare il **Root URL** del client a `http://localhost:8081` (l'indirizzo del Server Centrale).
6.  Creare due **ruoli**: `amministratore` e `impiegato`.
7.  Creare due utenti e assegnare loro i rispettivi ruoli. Credenziali di esempio:
    *   **Utente 1**: `amministratore` / `amministratore` (con ruolo amministratore)
    *   **Utente 2**: `impiegato` / `impiegato` (con ruolo impiegato)

### 3. Avvio del Sistema
1.  Assicurarsi che Mosquitto e Keycloak siano in esecuzione.
2.  Avviare il **`ServerCentrale`** (classe Java). Il server si metterà in ascolto su `http://localhost:8081`.
3.  Avviare la classe **`Main`** per avviare la simulazione dei caselli e osservare i log delle operazioni sul terminale.
4.  Aprire un browser e navigare su `http://localhost:8081`. Si verrà reindirizzati alla pagina di login di Keycloak.
5.  Effettuare l'accesso con le credenziali create per visualizzare il pannello gestionale.

## 🔌 API Endpoints (Gestionale)

Il Server Centrale espone le seguenti API REST, con accesso basato sui ruoli.

### Caselli
*   `GET /caselli` - Restituisce l'elenco completo dei caselli. (Accesso: `impiegato`, `amministratore`)
*   `POST /caselli` - Aggiunge un nuovo casello. (Accesso: `amministratore`)
*   `PUT /caselli/{nomeCasello}` - Modifica un casello esistente. (Accesso: `amministratore`)
*   `DELETE /caselli/{nomeCasello}` - Rimuove un casello. (Accesso: `amministratore`)

### Tratte
*   `GET /tratte` - Restituisce tutte le tratte. (Accesso: `impiegato`, `amministratore`)
*   `PUT /tratte/{trattaId}` - Modifica il pedaggio di una tratta. (Accesso: `amministratore`)

### Guadagni
*   `GET /guadagni` - Restituisce il guadagno complessivo. (Accesso: `amministratore`)
*   `GET /guadagni/{nomeCaselloIngresso}/{nomeCaselloUscita}` - Restituisce il guadagno di una tratta specifica. (Accesso: `amministratore`)

### Telepass
*   `GET /telepass` - Mostra l'elenco dei pagamenti Telepass. (Accesso: `amministratore`)
*   `PUT /telepass/{telepassId}` - Riscuote un singolo pagamento. (Accesso: `amministratore`)
*   `PUT /telepass` - Riscuote tutti i pagamenti in sospeso. (Accesso: `amministratore`)

### Multe
*   `GET /multe` - Restituisce l'elenco di tutte le multe. (Accesso: `impiegato`, `amministratore`)

## 🧪 Testing

Il progetto include una suite di test automatici realizzati con **JUnit5** per verificare il corretto funzionamento delle classi DAO (Data Access Object) che gestiscono la persistenza dei dati su file JSON. I test coprono le seguenti aree:
*   **`CaselliJsonDAOTest`**: Caricamento e recupero dei caselli.
*   **`PagamentiTelepassJsonDAOTest`**: Aggiunta e gestione dei pagamenti Telepass.
*   **`MulteJsonDAOTest`**: Creazione e recupero delle multe.
*   **`TratteJsonDAOTest`**: Gestione delle tratte e calcolo dei pedaggi.
*   **`VeicoliJsonDAOTest`**: Gestione dei veicoli registrati e in transito.
*   **`ViaggiJsonDAOTest`**: Creazione, salvataggio e ricerca dei viaggi.

I test sono progettati per non interferire con i dati reali, utilizzando file temporanei che vengono eliminati al termine dell'esecuzione.