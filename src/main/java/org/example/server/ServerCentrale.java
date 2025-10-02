package org.example.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.models.Casello;
import org.example.models.VeicoloInTransito;
import org.example.models.Viaggio;
import org.example.utils.*;


import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.rmi.server.RMISocketFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.rmi.server.RMISocketFactory.*;

public class ServerCentrale{

    public static final String BROKER_URL = "ssl://localhost:8883";
    private static final String CLIENT_ID = "ServerCentrale";
    private static final String MQTT_SERVER_USERNAME = "serverCentrale";
    private static final String MQTT_SERVER_PASSWORD = "server1883centrale";
    public static final String CA_FILE_PATH = "CA"+ File.separator + "ca.crt";
    public static final String CLIENT_CRT_FILE_PATH = "CA"+ File.separator + "server.crt";
    public static final String CLIENT_KEY_FILE_PATH = "CA"+ File.separator + "server.key";


    private MqttClient client;
    private Gson gson;

    // Strutture dati per gestire le informazioni
    private Map<String, VeicoloInTransito> veicoliInTransito;
    private Map<String, Viaggio> viaggi;
    //nuovi topic
    private String topicBigliettoErogato;
    private String topicRichiestaInfo;
    private String topicPagamentoEffettuato;
    private String topicRiceviDatiIng;
    private String topicRiceviDatiUsc;

    public ServerCentrale() {
        this.gson = new Gson();
        this.veicoliInTransito = new ConcurrentHashMap<>();
        this.viaggi = new HashMap<>();
        // Inizializza i dati di base

        try {
            // Inizializza il client MQTT
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            client.setCallback(new ServerCentraleCallbacks(client, gson, veicoliInTransito, viaggi));

            // Opzioni di connessione
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_SERVER_USERNAME);
            options.setPassword(MQTT_SERVER_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);


            // Connessione al broker
            client.connect(options);
            System.out.println("Server Centrale connesso al broker MQTT");

            // Sottoscrizione ai topic
            initializeTopics();
            subscribeToTopics();

        } catch (MqttException e) {
            System.err.println("Errore durante l'inizializzazione del client MQTT: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics(){
        this.topicBigliettoErogato = TopicManager.getTicketPrintedTopic("+");
        this.topicRichiestaInfo = TopicManager.getRequestInfoTopic("+");
        this.topicPagamentoEffettuato = TopicManager.getPaymentDoneTopic("+");
        this.topicRiceviDatiIng = TopicManager.getEnteringDataTopic("+");
        this.topicRiceviDatiUsc = TopicManager.getExitDataTopic("+");
    }

    private void subscribeToTopics() {
        try {

            client.subscribe(this.topicBigliettoErogato);
            client.subscribe(this.topicRichiestaInfo);
            client.subscribe(this.topicPagamentoEffettuato);
            client.subscribe(this.topicRiceviDatiIng);
            client.subscribe(this.topicRiceviDatiUsc);

            System.out.println("Sottoscrizione ai topic completata:");
            System.out.println("- " + this.topicBigliettoErogato);
            System.out.println("- " + this.topicRichiestaInfo);
            System.out.println("- " + this.topicPagamentoEffettuato);
            System.out.println("- " + this.topicRiceviDatiIng);
            System.out.println("- " + this.topicRiceviDatiUsc);
        } catch (MqttException e) {
            LogUtils.logErr("Errore durante la sottoscrizione ai topic", e);
        }
    }

    public void shutdown() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("Server Centrale disconnesso");
            }
        } catch (MqttException e) {
            System.err.println("Errore durante la disconnessione: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Server Centrale Sistema Autostradale ===");

        ServerCentrale server = new ServerCentrale();
        ServerApp.start();
        FrontendApp.start();

        System.out.println("\n=== Server Centrale Attivo ===");
        System.out.println("Il server è in attesa di messaggi dai caselli...");
        System.out.println("Comandi disponibili:");
        System.out.println("- 'q' per terminare il server");

        // Mantiene il programma attivo
        while (true) {
            String input = scanner.nextLine();
            if ("q".equalsIgnoreCase(input.trim())) {
                break;
            }
        }

        server.shutdown();
        scanner.close();
    }
}