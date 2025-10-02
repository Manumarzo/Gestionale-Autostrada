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

public class CaselloIngressoAutomatico extends Casello implements Runnable, MqttCallback{
    private static final Gson gson = new Gson();
    private static final String MQTT_ING_AUTO_USERNAME = "ingressoAuto";
    private static final String MQTT_ING_AUTO_PASSWORD = "auto123";
    private MqttClient client;
    private Telecamera telecamera;
    private Sbarra sbarra;

    // Contatore statico per id sequenziali
    private static int contatoreDispositivi = 0;

    //nuovi topic
    private String topicRichiestaEntrata;
    private String topicRichiestaTarga;
    private String topicLetturaTarga;
    private String topicInviaDati;

    public CaselloIngressoAutomatico(String nome, String regione, double latitudine, double longitudine){
        super(nome, regione, latitudine, longitudine);
        init();
    }

    public CaselloIngressoAutomatico(Casello c) {
        super(c.getNome(), c.getRegione(), c.getLatitudine(), c.getLongitudine());
        init();
    }

    private void init(){
        //aggiorno il contatore e formatto il contatore con 2 cifre
        contatoreDispositivi++;
        setDispositivoId(this.getNome() + "_INGR_AUTO_" + String.format("%02d", contatoreDispositivi));

        this.telecamera = new Telecamera(getDispositivoId(), "ingresso");
        Thread thTelecamera = new Thread(this.telecamera);
        thTelecamera.start();

        this.sbarra = new Sbarra(getDispositivoId());
        Thread thSbarra = new Thread(this.sbarra);
        thSbarra.start();
        // Inizializza i topic
        initializeTopics();

        try {
            // Inizializza il client MQTT
            client = new MqttClient(BROKER_URL, CLIENT_ID + "_" + this.getNome(), new MemoryPersistence());
            client.setCallback(this);

            // Opzioni di connessione
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_ING_AUTO_USERNAME);
            options.setPassword(MQTT_ING_AUTO_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            // Connessione al broker
            client.connect(options);
            System.out.println("Casello Ingresso Automatico " + this.getNome() + " connesso al broker MQTT");


            // Sottoscrizione ai topic
            subscribeToTopics();

        } catch (MqttException e) {
            System.err.println("Errore durante l'inizializzazione del client MQTT: " + e.getMessage());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics() {

        //nuovi topic
        this.topicRichiestaEntrata = TopicManager.getAutoEnteringRequestTopic(getDispositivoId());
        this.topicRichiestaTarga = TopicManager.getAutoInPlateRequestTopic(getDispositivoId());
        this.topicLetturaTarga = TopicManager.getAutoInPlateResponseTopic(getDispositivoId());
        this.topicInviaDati = TopicManager.getEnteringDataTopic(getDispositivoId());
    }

    private void subscribeToTopics() {
        try {

            //nuovi topic
            client.subscribe(topicRichiestaEntrata, 1);
            client.subscribe(topicLetturaTarga, 1);

            System.out.println("Sottoscrizione ai topic completata:");
            System.out.println("- " + topicRichiestaEntrata);
            System.out.println("- " + topicLetturaTarga);

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

            Runnable task = () -> {
                try {
                    if(topic.equals(this.topicRichiestaEntrata)) gestisciRichiestaEntrata(jsonMessage);
                    else if(topic.equals(this.topicLetturaTarga)) gestisciLetturaTarga(jsonMessage);
                } catch (Exception e) {
                    LogUtils.logErr("Errore nel thread di gestione messaggio", e);
                }
            };
            new Thread(task).start();
        } catch (Exception e) {
            System.err.println("Errore durante l'elaborazione del messaggio: " + e.getMessage());
        }
    }

    private void gestisciRichiestaEntrata(JsonObject message){
        System.out.println("Richiesta di entrata ricevuta da telepass");

        String telepassId = message.get("telepass_id").getAsString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String targa = message.get("targa").getAsString();

        System.out.println("ID Telepass: " + telepassId);
        System.out.println("Timestamp: " + timestamp);

        if(telepassId != null){
            richiestaLetturaTarga(telepassId, targa);
        }
        else LogUtils.logErr("Id telepass non valido. Il veicolo non può entrare", null);
    }

    private void gestisciLetturaTarga(JsonObject message){
        System.out.println("Targa letta dalla telecamera");

        String targa = message.get("targa").getAsString();
        String timestamp = message.get("timestamp").getAsString();
        String telepassId = message.get("telepass_id").getAsString();

        System.out.println("Targa: " + targa);

        inviaEntrataAlServer(telepassId, targa, timestamp);
    }

    private void richiestaLetturaTarga(String telepassId, String targa) {
        try {
            JsonObject richiestaTargaMsg = new JsonObject();
            richiestaTargaMsg.addProperty("tipo", "richiesta_targa");
            richiestaTargaMsg.addProperty("casello", this.getNome());
            richiestaTargaMsg.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            richiestaTargaMsg.addProperty("tipo_dispositivo", "ingresso");
            richiestaTargaMsg.addProperty("dispositivo_id", getDispositivoId());
            richiestaTargaMsg.addProperty("telepass_id", telepassId);
            richiestaTargaMsg.addProperty("targa", targa);

            String jsonString = gson.toJson(richiestaTargaMsg);
            client.publish(this.topicRichiestaTarga, jsonString.getBytes(), 1, false);

            System.out.println("Richiesta lettura targa inviata alla telecamera\n");

        } catch (MqttException e) {
            System.err.println("Errore nell'invio della richiesta targa: " + e.getMessage());
        }
    }

    private void inviaEntrataAlServer(String telepassId, String targa, String timestamp) {
        try {
            JsonObject entrataMsg = new JsonObject();
            entrataMsg.addProperty("tipo", "entrata_automatica");
            entrataMsg.addProperty("casello", this.getNome());
            entrataMsg.addProperty("timestamp", timestamp);
            entrataMsg.addProperty("telepass_id", telepassId);
            entrataMsg.addProperty("targa", targa);
            entrataMsg.addProperty("dispositivo_id", getDispositivoId());

            String jsonString = gson.toJson(entrataMsg);
            client.publish(this.topicInviaDati, jsonString.getBytes(), 1, false);

            System.out.println("Informazioni di entrata inviate al server centrale");

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
                System.out.println("Casello Ingresso Automatico " + this.getNome() + " disconnesso");
            }
        } catch (MqttException e) {
            System.err.println("Errore durante la disconnessione: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Casello Ingresso Automatico ===");
        System.out.print("Inserisci il nome del casello (es: AL_EST): ");
        String nomeCasello = scanner.nextLine().trim().toUpperCase();

        Casello caselloScelto = CaselliJsonDAO.getCaselloByName(nomeCasello);
        if (caselloScelto == null) {
            System.err.println("Casello non trovato. Uscita.");
            scanner.close();
            return;
        }

        CaselloIngressoAutomatico casello = new CaselloIngressoAutomatico(caselloScelto);
        Thread threadCasello = new Thread(casello);
        threadCasello.start();

        System.out.println("\n=== Casello Ingresso Automatico Attivo ===");
        System.out.println("Il dispositivo è in attesa di richieste di entrata telepass...");
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