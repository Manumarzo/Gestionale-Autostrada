package org.example.microservices;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.models.Biglietto;
import org.example.utils.TopicManager;
import org.example.models.Casello;
import org.example.dao.CaselliJsonDAO;
import org.example.utils.LogUtils;

import javax.net.ssl.SSLSocketFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static org.example.server.ServerCentrale.*;

public class CaselloIngressoManuale extends Casello implements Runnable, MqttCallback {
    private static final Gson gson = new Gson();
    private static final int QOS = 1;
    private static final String MQTT_ING_MAN_USERNAME = "ingressoMan";
    private static final String MQTT_ING_MAN_PASSWORD = "man123";
    private CountDownLatch latch;

    // Contatore statico per id sequenziali
    private static int contatoreDispositivi = 0;

    private MqttClient client;
    private String targaAttuale;
    private Telecamera telecamera;
    private Sbarra sbarra;

    //nuovi topic
    private String topicRichiestaBiglietto;
    private String topicRichiestaTarga;
    private String topicRispostaTarga;
    private String topicBigliettoErogato;



    public CaselloIngressoManuale(String nome, String regione, double latitudine, double longitudine) {
        super(nome, regione, latitudine, longitudine);
        contatoreDispositivi++;
        setDispositivoId(getNome() + "_INGR_MAN_" + String.format("%02d", contatoreDispositivi));
        init();
    }

    public CaselloIngressoManuale(Casello c) {
        this(c.getNome(), c.getRegione(), c.getLatitudine(), c.getLongitudine());
    }

    private void init() {
        this.telecamera = new Telecamera(getDispositivoId(), "ingresso");
        Thread thTelecamera = new Thread(this.telecamera);
        thTelecamera.start();

        this.sbarra = new Sbarra(getDispositivoId());
        Thread thSbarra = new Thread(this.sbarra);
        thSbarra.start();

        initializeTopics();

        try {
            // Inizializzazione client MQTT
            this.client = new MqttClient(BROKER_URL, getDispositivoId(), new MemoryPersistence());
            this.client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_ING_MAN_USERNAME);
            options.setPassword(MQTT_ING_MAN_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            this.client.connect(options);
            System.out.println("Dispositivo Ingresso Manuale " + getDispositivoId() + " avviato correttamente");

            // Sottoscrizione ai topic necessari
            subscribeToTopics();

        } catch (MqttException e) {
            System.err.println("Errore nell'inizializzazione del dispositivo MQTT: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics() {
        this.topicRichiestaBiglietto = TopicManager.getTicketRequestTopic(getDispositivoId());
        this.topicRichiestaTarga = TopicManager.getManInPlateRequestTopic(getDispositivoId());
        this.topicRispostaTarga = TopicManager.getManInPlateResponseTopic(getDispositivoId());
        this.topicBigliettoErogato = TopicManager.getTicketPrintedTopic(getDispositivoId());
    }

    private void subscribeToTopics() {
        try {
            client.subscribe(topicRichiestaBiglietto, QOS);
            client.subscribe(topicRispostaTarga, QOS);

            System.out.println("Sottoscrizione ai topic completata per dispositivo " + getDispositivoId());
            System.out.println("- " + topicRichiestaBiglietto);
            System.out.println("- " + topicRispostaTarga);
            System.out.println("\n");
        } catch (MqttException e) {
            System.err.println("Errore durante la sottoscrizione ai topic: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        cause.printStackTrace();
        System.err.println("Connessione MQTT persa: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        LogUtils.logMessageArrived(topic, payload);

        Runnable task = () -> {
            try {
                JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);
                if (topic.equals(topicRichiestaBiglietto)) gestisciRichiestaBiglietto(jsonMessage);
                else if (topic.equals(topicRispostaTarga)) gestisciTargaRicevuta(jsonMessage);
                
            } catch (Exception e) {
                LogUtils.logErr("Errore nella gestione del messaggio: " + payload, e);
            }
        };
        new Thread(task).start();
    }


    private void gestisciRichiestaBiglietto(JsonObject messaggio) throws MqttException {
        System.out.println("Ricevuta richiesta biglietto da utente");

        // Genera ID biglietto univoco
        Biglietto biglietto = new Biglietto(this.getNome());

        // Richiedi targa alla telecamera
        try{
            LogUtils.simulaSvolgimento("In attesa della lettura targa");
            Thread.sleep(2000);
            richiediTargaTelecamera(messaggio.get("targa").getAsString());
            this.latch = new CountDownLatch(1);
            latch.await();
        } catch(Exception e) {
            LogUtils.logErr("Errore durante la richiesta della targa", e);
        }
        // Prepara messaggio di risposta per l'utente
        Map<String, Object> response = new HashMap<>();
        response.put("biglietto_id", biglietto.getId().toString());
        response.put("biglietto_data", biglietto.getTs().toString());
        response.put("casello", getNome());
        response.put("id_casello", getDispositivoId());
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("targa", this.targaAttuale);

        client.publish(topicBigliettoErogato, gson.toJson(response).getBytes(), QOS, false);
        System.out.println("Biglietto " + biglietto.getId() + " preparato e comunicato all'utente");
    }

    private void richiediTargaTelecamera(String targa) throws MqttException {
        Map<String, Object> richiesta = new HashMap<>();
        richiesta.put("tipo", "richiesta_targa");
        richiesta.put("targa", targa);
        richiesta.put("casello", getNome());
        richiesta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        richiesta.put("tipo_dispositivo", "ingresso");
        richiesta.put("dispositivo_id", getDispositivoId());

        client.publish(topicRichiestaTarga, gson.toJson(richiesta).getBytes(), QOS, false);
        System.out.println("Richiesta targa inviata alla telecamera");
    }

    private void gestisciTargaRicevuta(JsonObject messaggio) throws MqttException {
        this.targaAttuale = messaggio.get("targa").getAsString();
        System.out.println("Targa ricevuta dalla telecamera: " + this.targaAttuale);
        this.latch.countDown();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato
    }

    public void shutdown() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("Dispositivo " + getDispositivoId() + " disconnesso");
            }
        } catch (MqttException e) {
            System.err.println("Errore nella disconnessione: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Casello Ingresso Manuale ===");
        System.out.print("Inserisci il nome del casello (es: AL_EST): ");
        String nomeCasello = scanner.nextLine().trim().toUpperCase();

        Casello caselloScelto = CaselliJsonDAO.getCaselloByName(nomeCasello);
        if (caselloScelto == null) {
            System.err.println("Casello non trovato. Uscita.");
            scanner.close();
            return;
        }

        CaselloIngressoManuale casello = new CaselloIngressoManuale(caselloScelto);
        Thread threadCasello = new Thread(casello);
        threadCasello.start();

        System.out.println("\n=== Casello Ingresso Manuale Attivo ===");
        System.out.println("Premi 'q' per terminare il programma");

        while (true) {
            if ("q".equalsIgnoreCase(scanner.nextLine().trim())) {
                break;
            }
        }

        casello.shutdown();
        scanner.close();
        System.out.println("Programma terminato.");
    }

    @Override
    public void run() {
        // Il thread principale può rimanere inattivo, la logica è guidata dagli eventi MQTT
    }
}