package org.example.microservices;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.dao.CaselliJsonDAO;
import org.example.utils.TopicManager;
import org.example.models.Casello;
import org.example.utils.LogUtils;

import javax.net.ssl.SSLSocketFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import static org.example.server.ServerCentrale.*;

public class CaselloUscitaAutomatico extends Casello implements Runnable, MqttCallback {

    private static final String CLIENT_ID = "CaselloUscitaAutomatico";
    private static final Gson gson = new Gson();
    private static final String MQTT_USC_AUTO_USERNAME = "uscitaAuto";
    private static final String MQTT_USC_AUTO_PASSWORD = "auto456";

    private MqttClient client;
    private Telecamera telecamera;
    private Sbarra sbarra;

    private static int contatoreDispositivi = 0;

    //nuovi topic
    private String topicRichiestaUscita;
    private String topicRichiestaTarga;
    private String topicLetturaTarga;
    private String topicInviaDati;

    public CaselloUscitaAutomatico(String nome, String regione, double latitudine, double longitudine){
        super(nome, regione, latitudine, longitudine);
        init();
    }

    public CaselloUscitaAutomatico(Casello c) {
        super(c.getNome(), c.getRegione(), c.getLatitudine(), c.getLongitudine());
        init();
    }

    private void init(){
        setDispositivoId(this.getNome() + "_USC_AUTO_" + String.format("%02d", ++contatoreDispositivi));

        this.telecamera = new Telecamera(getDispositivoId(), "uscita");
        Thread thTelecamera = new Thread(this.telecamera);
        thTelecamera.start();

        this.sbarra = new Sbarra(getDispositivoId());
        Thread thSbarra = new Thread(this.sbarra);
        thSbarra.start();

        initializeTopics();

        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID + "_" + this.getNome(), new MemoryPersistence());
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_USC_AUTO_USERNAME);
            options.setPassword(MQTT_USC_AUTO_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            client.connect(options);
            System.out.println("Casello Uscita Automatico " + this.getNome() + " connesso al broker MQTT");

            subscribeToTopics();

        } catch (MqttException e) {
            System.err.println("Errore durante l'inizializzazione del client MQTT: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics() {
        //nuovi topic
        this.topicRichiestaUscita = TopicManager.getAutoExitRequestTopic(getDispositivoId());
        this.topicRichiestaTarga = TopicManager.getAutoOutPlateRequestTopic(getDispositivoId());
        this.topicLetturaTarga = TopicManager.getAutoOutPlateResponseTopic(getDispositivoId());
        this.topicInviaDati = TopicManager.getExitDataTopic(getDispositivoId());
    }

    private void subscribeToTopics() {
        try {
            // Sottoscrizione ai topic di interesse
            client.subscribe(this.topicRichiestaUscita, 1);
            client.subscribe(this.topicLetturaTarga, 1);

            System.out.println("Sottoscrizione ai topic completata:");
            System.out.println("- " + this.topicRichiestaUscita);
            System.out.println("- " + this.topicLetturaTarga);

        } catch (MqttException e) {
            System.err.println("Errore durante la sottoscrizione ai topic: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connessione MQTT persa: " + cause.getMessage());
        // Tentativo di riconnessione automatica
        try {
            if (!client.isConnected()) {
                client.reconnect();
                subscribeToTopics();
            }
        } catch (MqttException e) {
            System.err.println("Errore durante la riconnessione: " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        LogUtils.logMessageArrived(topic, payload);

        try {
            JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);
            if(topic.equals(this.topicRichiestaUscita)) gestisciRichiestaUscita(jsonMessage);
            else if(topic.equals(this.topicLetturaTarga)) gestisciLetturaTarga(jsonMessage);

        } catch (Exception e) {
            System.err.println("Errore durante l'elaborazione del messaggio: " + e.getMessage());
        }
    }

    private void gestisciRichiestaUscita(JsonObject message){
        System.out.println("Richiesta di uscita ricevuta da telepass");

        String telepassId = message.get("telepass_id").getAsString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String targa = message.get("targa").getAsString();

        System.out.println("ID Telepass: " + telepassId);
        System.out.println("Timestamp: " + timestamp);

        // Richiesta lettura targa alla telecamera
        richiestaLetturaTarga(telepassId, targa);
    }



    private void richiestaLetturaTarga(String telepassId, String targa) {
        try {
            JsonObject richiestaTargaMsg = new JsonObject();
            richiestaTargaMsg.addProperty("tipo", "richiesta_targa");
            richiestaTargaMsg.addProperty("casello", this.getNome());
            richiestaTargaMsg.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            richiestaTargaMsg.addProperty("tipo_dispositivo", "uscita");
            richiestaTargaMsg.addProperty("dispositivo_id", getDispositivoId());
            richiestaTargaMsg.addProperty("telepass_id", telepassId);
            richiestaTargaMsg.addProperty("targa", targa);

            String jsonString = gson.toJson(richiestaTargaMsg);
            client.publish(this.topicRichiestaTarga, jsonString.getBytes(), 1, false);

            System.out.println("Richiesta lettura targa inviata alla telecamera");

        } catch (MqttException e) {
            System.err.println("Errore nell'invio della richiesta targa: " + e.getMessage());
        }
    }

    private void gestisciLetturaTarga(JsonObject message){
        System.out.println("Targa letta dalla telecamera");

        String targa = message.get("targa").getAsString();
        String timestamp = message.get("timestamp").getAsString();
        String telepassId = message.get("telepass_id").getAsString();

        System.out.println("Targa: " + targa);

        inviaEntrataAlServer(telepassId, targa, timestamp);
    }

    private void inviaEntrataAlServer(String telepassId, String targa, String timestamp) {
        try {
            JsonObject entrataMsg = new JsonObject();
            entrataMsg.addProperty("tipo", "uscita_automatica");
            entrataMsg.addProperty("casello", this.getNome());
            entrataMsg.addProperty("timestamp", timestamp);
            entrataMsg.addProperty("telepass_id", telepassId);
            entrataMsg.addProperty("targa", targa);
            entrataMsg.addProperty("dispositivo_id", getDispositivoId());

            String jsonString = gson.toJson(entrataMsg);
            client.publish(this.topicInviaDati, jsonString.getBytes(), 1, false);

            System.out.println("Informazioni di uscita inviate al server centrale");

        } catch (MqttException e) {
            System.err.println("Errore nell'invio al server: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato in questo esempio
    }

    public void shutdown() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("Casello Uscita Automatico " + this.getNome() + " disconnesso");
            }
        } catch (MqttException e) {
            System.err.println("Errore durante la disconnessione: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Casello Uscita Automatico ===");
        System.out.print("Inserisci il nome del casello (es: MI_BARRIERA): ");
        String nomeCasello = scanner.nextLine().trim().toUpperCase();

        Casello caselloScelto = CaselliJsonDAO.getCaselloByName(nomeCasello);
        if (caselloScelto == null) {
            System.err.println("Casello non trovato. Uscita.");
            scanner.close();
            return;
        }

        CaselloUscitaAutomatico casello = new CaselloUscitaAutomatico(caselloScelto);
        Thread threadCasello = new Thread(casello);
        threadCasello.start();

        System.out.println("\n=== Casello Uscita Automatico Attivo ===");
        System.out.println("Il dispositivo è in attesa di richieste di uscita telepass...");
        System.out.println("Premi 'q' per terminare il programma");

        // Mantiene il programma attivo
        while (true) {
            String input = scanner.nextLine();
            if ("q".equalsIgnoreCase(input.trim())) {
                break;
            }
        }

        casello.shutdown();
        scanner.close();
        System.out.println("Programma terminato.");
    }

    @Override
    public void run() {
    }
}
