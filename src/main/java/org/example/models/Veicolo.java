package org.example.models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.server.ServerCentrale;
import org.example.dao.CaselliJsonDAO;
import org.example.utils.LogUtils;
import org.example.utils.TopicManager;

import javax.net.ssl.SSLSocketFactory;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.example.server.ServerCentrale.*;

public class Veicolo implements MqttCallback, Runnable {
    private static final Random random = new Random();
    private static final String LETTERE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Gson gson = new Gson();

    private final String targa;
    private final String id_telepass;
    private final boolean hasTelepass;
    private transient MqttClient client;
    private transient Casello caselloIn;
    private transient Casello caselloOut;

    // Variabili di stato per il viaggio manuale
    private transient Biglietto biglietto;
    private transient double importoDaPagare;
    private transient CountDownLatch responseLatch;

    //topic
    private transient String topicSbarraIngAperta;
    private transient String topicRichiestaBiglietto;
    private transient String topicBigliettoErogato;
    private transient String topicInserimentoBiglietto;
    private transient String topicRispostaInfo;
    private transient String topicInserimentoDenaro;
    private transient String topicPagamentoEffettuato;
    private transient String topicSbarraUscAperta;
    private transient String topicRichiestaEntrataAuto;
    private transient String topicRichiestaUscitaAuto;


    public Veicolo(boolean hasTelepass) {
        this.hasTelepass = hasTelepass;
        this.targa = this.generateNewPlate();

        this.id_telepass = hasTelepass ? UUID.randomUUID().toString() : null;

        this.caselloIn = CaselliJsonDAO.pickRandomCasello();
        do {
            this.caselloOut = CaselliJsonDAO.pickRandomCasello();
        } while (caselloOut == null || caselloOut.getNome().equals(Objects.requireNonNull(caselloIn).getNome()));

        try {
            String clientId = "Veicolo_" + targa;
            client = new MqttClient(ServerCentrale.BROKER_URL, clientId, new MemoryPersistence());
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            client.connect(options);

            initializeTopics(this.hasTelepass, CaselliJsonDAO.getIdCaselloInUso(caselloIn.getNome(), true, this.hasTelepass), CaselliJsonDAO.getIdCaselloInUso(caselloOut.getNome(), false, this.hasTelepass));
            subscribeToTopics(hasTelepass);

        } catch (MqttException e) {
            System.err.println("Errore durante l'inizializzazione del client MQTT del veicolo: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics(boolean telepass, String idIn, String idOut){
        //Ingresso
        this.topicSbarraIngAperta = TopicManager.getOpenBarTopic(idIn);
        //Uscita
        this.topicSbarraUscAperta = TopicManager.getOpenBarTopic(idOut);
        if(telepass){
            this.topicRichiestaEntrataAuto = TopicManager.getAutoEnteringRequestTopic(idIn);
            this.topicRichiestaUscitaAuto = TopicManager.getAutoExitRequestTopic(idOut);
        } else {
            this.topicRichiestaBiglietto = TopicManager.getTicketRequestTopic(idIn);
            this.topicBigliettoErogato = TopicManager.getTicketPrintedTopic(idIn);
            this.topicInserimentoBiglietto = TopicManager.getInsertTicketTopic(idOut);
            this.topicRispostaInfo = TopicManager.getResponseInfoTopic(idOut);
            this.topicInserimentoDenaro = TopicManager.getInsertMoneyTopic(idOut);
            this.topicPagamentoEffettuato = TopicManager.getPaymentDoneTopic(idOut);
        }
    }

    private void subscribeToTopics(boolean telepass){
        try{
            client.subscribe(topicSbarraIngAperta, 1);
            client.subscribe(topicSbarraUscAperta, 1);

            LogUtils.logVeicolo(this.targa, "Sottoscritto ai topic");
            System.out.println("- " + topicSbarraIngAperta);
            System.out.println("- " + topicSbarraUscAperta);
            if(telepass){

            } else {
                client.subscribe(this.topicBigliettoErogato, 1);
                client.subscribe(this.topicRispostaInfo, 1);
                client.subscribe(this.topicPagamentoEffettuato, 1);

                System.out.println("- " + topicBigliettoErogato);
                System.out.println("- " + topicRispostaInfo);
                System.out.println("- " + topicPagamentoEffettuato);
            }
        } catch (MqttException e){
            LogUtils.logErr("Errore del veicolo durante la sottoscrizione ai topic", e);
        }
    }

    public String getTarga() {
        return targa;
    }

    public String getId_telepass() {
        return id_telepass;
    }

    public boolean hasTelepass() {
        return hasTelepass;
    }

    private String generateNewPlate() {

        StringBuilder targa = new StringBuilder();
        for(int i = 0; i < 2; i++){
            targa.append(LETTERE.charAt(random.nextInt(LETTERE.length())));
        }
        for(int i = 0; i < 3; i++){
            targa.append(random.nextInt(10));
        }
        for(int i = 0; i < 2; i++){
            targa.append(LETTERE.charAt(random.nextInt(LETTERE.length())));
        }
        return targa.toString();
    }

    // --- METODI PER L'INTERAZIONE MANUALE ---

    public void richiediBiglietto(String nomeCasello) throws MqttException {

        String payload = "{ \"richiesta\": \"biglietto\", \"targa\": \"" + this.targa + "\" }";
        client.publish(this.topicRichiestaBiglietto, payload.getBytes(), 1, false);
        System.out.println("[Veicolo " + targa + "] Richiesto biglietto al casello manuale " + nomeCasello);
    }

    public void inserisciBiglietto(String nomeCasello) throws MqttException {
        if (biglietto == null) {
            System.err.println("[Veicolo " + targa + "] ERRORE: Tentativo di uscire senza biglietto!");
            return;
        }
        String payload = "{" + "\"targa\": \"" + this.targa + "\", " + "\"biglietto_id\": \"" + this.biglietto.getId() + "\", " + "\"biglietto_data\": \"" + this.biglietto.getTs().toString() + "\", " + "\"casello_ingresso\": \"" + this.biglietto.getIdIngresso() + "\"" + "}";
        client.publish(this.topicInserimentoBiglietto, payload.getBytes(), 1, false);
        System.out.println("[Veicolo " + targa + "] Inserito biglietto " + biglietto.getId() + " al casello " + nomeCasello);
    }

    public void effettuaPagamento(String nomeCasello) throws MqttException {
        double importo_inserito = random.nextDouble(Math.max(0, this.importoDaPagare - 2), this.importoDaPagare + 5);
        String payload = "{ \"importo\": " + importo_inserito + ", \"targa\": \"" + this.targa + "\" }";
        client.publish(this.topicInserimentoDenaro, payload.getBytes(), 1, false);
        LogUtils.logVeicolo(this.targa, "Pagamento di " + String.format("%.2f", importo_inserito) + " EUR effettuato al casello " + nomeCasello);
    }


    // --- METODI PER L'INTERAZIONE AUTOMATICA (Telepass) ---

    public void inviaRichiestaEntrataAutomatica(String nomeCasello) {
        String payload = "{ \"telepass_id\": \"" + this.id_telepass + "\", \"targa\": \"" + this.targa + "\" }";
        try {
            LogUtils.logVeicolo(this.targa, "Richiesta entrata automatica inviata a " + nomeCasello);
            client.publish(this.topicRichiestaEntrataAuto, payload.getBytes(), 1, false);
        } catch (MqttException e) {
            LogUtils.logErr("Errore durante l'invio della richiesta di entrata al casello " + nomeCasello, e);
        }
    }

    public void inviaRichiestaUscitaAutomatica(String nomeCasello) {
        String payload = "{ \"telepass_id\": \"" + this.id_telepass + "\", \"targa\": \"" + this.targa + "\" }";
        try {
            client.publish(this.topicRichiestaUscitaAuto, payload.getBytes(), 1, false);
            LogUtils.logVeicolo(this.targa, " Richiesta uscita inviata a " + nomeCasello);
        } catch (MqttException e) {
            LogUtils.logErr("Errore durante l'invio della richiesta di uscita al casello " + nomeCasello, e);
        }
    }


    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Veicolo " + targa + ": Connessione MQTT persa.");
    }

    /**
     * Gestisce i messaggi in arrivo destinati a questo veicolo.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        JsonObject json = gson.fromJson(payload, JsonObject.class);
        if(json.has("targa")) if(!json.get("targa").getAsString().equals(this.targa)) return;

        // Il veicolo reagisce ai messaggi del casello
        LogUtils.logVeicolo(this.targa, "Messaggio ricevuto su topic " + topic);

        if(topic.equals(this.topicSbarraIngAperta) || topic.equals(this.topicSbarraUscAperta)) responseLatch.countDown();
        else if(topic.equals(this.topicBigliettoErogato)) gestisciBigliettoErogato(json);
        else if(topic.equals(this.topicRispostaInfo)) gestisciRispostaInfo(json);
        else if(topic.equals(this.topicPagamentoEffettuato)) gestisciPagamentoEffettuato(json);

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non necessario per questa logica
    }


    /**
     * Logica principale del viaggio del veicolo.
     */
    @Override
    public void run() {
        try {
            if (this.hasTelepass) {
                viaggioConTelepass();
            } else {
                viaggioManuale();
            }
        } catch (Exception e) {
            System.err.println("[Veicolo " + targa + "] Viaggio interrotto da un errore: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            try {
                if (client != null && client.isConnected()) {
                    client.disconnect();
                    client.close();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void gestisciBigliettoErogato(JsonObject json){
        String bigliettoId = json.get("biglietto_id").getAsString();
        String bigliettoData = json.get("biglietto_data").getAsString();
        String nomeCasello = json.get("casello").getAsString();
        this.biglietto = new Biglietto(UUID.fromString(bigliettoId), LocalDateTime.parse(bigliettoData), nomeCasello);
        System.out.println("[Veicolo " + targa + "] Ho ricevuto il biglietto: " + this.biglietto.getId());
        responseLatch.countDown();
    }

    private void gestisciRispostaInfo(JsonObject json){
        this.importoDaPagare = json.get("pedaggio").getAsDouble();
        LogUtils.logVeicolo(this.targa, "Mi è stato comunicato l'importo da pagare: " + String.format("%.2f", this.importoDaPagare) + " EUR");
        responseLatch.countDown();
    }

    private void gestisciPagamentoEffettuato(JsonObject json){
        String tipo = json.get("tipo").getAsString();
        if(tipo.equals("pagamento_completato")){
            double resto = json.get("resto").getAsDouble();
            LogUtils.logVeicolo(this.targa, "Ho ricevuto il resto di " + String.format("%.2f", resto) + " EUR");

            responseLatch.countDown();

        } else if(tipo.equals("importo_insufficiente")) {
            String nomeCasello = json.has("casello") ? json.get("casello").getAsString() : caselloOut.getNome();
            LogUtils.logVeicolo(this.targa, "L'importo era insufficiente, effettuo nuovo pagamento al casello " + nomeCasello);
            try {
                effettuaPagamento(nomeCasello); // tenta nuovo pagamento
            } catch(MqttException e){
                LogUtils.logErr("Errore durante l'effettuazione del nuovo pagamento", e);
            }
        }
    }


    private void viaggioConTelepass() throws InterruptedException {
        LogUtils.simulaSvolgimento("Inizio Viaggio Automatico per Veicolo " + targa);

        LogUtils.logVeicolo(targa, "In ingresso al casello automatico " + caselloIn.getNome());
        inviaRichiestaEntrataAutomatica(caselloIn.getNome());

        responseLatch = new CountDownLatch(1);
        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            LogUtils.logErr("La sbarra di ingresso non si è aperta", null);
            return;
        }

        LogUtils.logVeicolo(targa, "In viaggio sull'autostrada...");
        Thread.sleep(random.nextInt(4, 10) * 1000L);
        LogUtils.logVeicolo(targa, "In avvicinamento al casello di uscita manuale: " + caselloOut.getNome());

        Thread.sleep(2000);

        inviaRichiestaUscitaAutomatica(this.caselloOut.getNome());

        responseLatch = new CountDownLatch(1);
        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            LogUtils.logErr("La sbarra di ingresso non si è aperta", null);
            return;
        }
        
        LogUtils.simulaSvolgimento("Fine Viaggio Telepass per Veicolo " + targa);
    }

    private void viaggioManuale() throws Exception {
        LogUtils.simulaSvolgimento("Inizio Viaggio Manuale per Veicolo " + targa);

        LogUtils.logVeicolo(targa, "In avvicinamento al casello di ingresso manuale: " + caselloIn.getNome());

        // Richiesta biglietto e attesa della risposta
        responseLatch = new CountDownLatch(1);
        richiediBiglietto(caselloIn.getNome());
        if (!responseLatch.await(10, TimeUnit.SECONDS)) { // Attende max 10 secondi
            LogUtils.logErr("Nessun biglietto ricevuto dal casello di ingresso", null);
            return;
        }
        // Attesa apertura sbarra
        LogUtils.logVeicolo(this.targa, "In attesa apertura sbarra...");
        responseLatch = new CountDownLatch(1);
        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            LogUtils.logErr("La sbarra di ingresso non si è aperta", null);
            return;
        }
        LogUtils.logVeicolo(targa, "Ingresso effettuato");

        LogUtils.logVeicolo(targa, "In viaggio sull'autostrada...");
        Thread.sleep(random.nextInt(4, 10) * 1000L);

        LogUtils.logVeicolo(targa, "In avvicinamento al casello di uscita manuale: " + caselloOut.getNome());

        // Inserimento biglietto e attesa dell'importo da pagare
        inserisciBiglietto(caselloOut.getNome());
        responseLatch = new CountDownLatch(1);
        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            LogUtils.logErr("Nessuna informazione di pagamento ricevuta", null);
            return;
        }

        LogUtils.logVeicolo(this.targa, "Sto effettuando il pagamento");
        Thread.sleep(1000 + random.nextLong(2) * 1000);

        // Pagamento
        responseLatch = new CountDownLatch(1);
        effettuaPagamento(caselloOut.getNome());

        // Attendi che il pagamento sia effettivamente completato
        if (!responseLatch.await(15, TimeUnit.SECONDS)) {
            LogUtils.logErr("Pagamento non completato entro il timeout", null);
            return;
        }

        responseLatch = new CountDownLatch(1);
        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            LogUtils.logErr("La sbarra non si è aperta entro il timeout", null);
            return;
        }

        LogUtils.logVeicolo(targa, "Uscita completata");

        LogUtils.simulaSvolgimento("Fine viaggio manuale per " + targa);
    }


}