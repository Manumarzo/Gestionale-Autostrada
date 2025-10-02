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
import java.util.UUID;

import static org.example.server.ServerCentrale.*;

public class CaselloUscitaManuale extends Casello implements Runnable, MqttCallback {

    private static final Gson gson = new Gson();
    private static final int QOS = 1;
    private static final String MQTT_USC_MAN_USERNAME = "uscitaMan";
    private static final String MQTT_USC_MAN_PASSWORD = "man456";

    private static int contatoreDispositivi = 0;
    private MqttClient client;

    private UUID bigliettoCorrenteId;
    private String targaCorrente;
    private double importoDaPagare;
    private boolean pagamentoInCorso;
    private Telecamera telecamera;
    private Sbarra sbarra;

    //nuovi topic
    private String topicInserimentoBiglietto;
    private String topicRichiestaTarga;
    private String topicRispostaTarga;
    private String topicRichiestaInfo;
    private String topicRispostaInfo;
    private String topicInserimentoDenaro;
    private String topicPagamentoEffettuato;

    public CaselloUscitaManuale(String nome, String regione, double latitudine, double longitudine) {
        super(nome, regione, latitudine, longitudine);
        contatoreDispositivi++;
        setDispositivoId(getNome() + "_USC_MAN_" + String.format("%02d", contatoreDispositivi));
        this.pagamentoInCorso = false;
        init();
    }

    public CaselloUscitaManuale(Casello c) {
        this(c.getNome(), c.getRegione(), c.getLatitudine(), c.getLongitudine());
    }

    private void init() {
        this.telecamera = new Telecamera(getDispositivoId(), "uscita");
        Thread thTelecamera = new Thread(this.telecamera);
        thTelecamera.start();

        this.sbarra = new Sbarra(getDispositivoId());
        Thread thSbarra = new Thread(this.sbarra);
        thSbarra.start();

        initializeTopics();
        try {
            this.client = new MqttClient(BROKER_URL, getDispositivoId(), new MemoryPersistence());
            this.client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_USC_MAN_USERNAME);
            options.setPassword(MQTT_USC_MAN_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            this.client.connect(options);
            System.out.println("Dispositivo Uscita Manuale " + getDispositivoId() + " avviato correttamente");
            subscribeToTopics();

        } catch (MqttException e) {
            System.err.println("Errore nell'inizializzazione del dispositivo MQTT: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics() {
        this.topicInserimentoBiglietto = TopicManager.getInsertTicketTopic(getDispositivoId());
        this.topicRichiestaTarga = TopicManager.getManOutPlateRequestTopic(getDispositivoId());
        this.topicRispostaTarga = TopicManager.getManoutPlateResponseTopic(getDispositivoId());
        this.topicRichiestaInfo = TopicManager.getRequestInfoTopic(getDispositivoId());
        this.topicRispostaInfo = TopicManager.getResponseInfoTopic(getDispositivoId());
        this.topicInserimentoDenaro = TopicManager.getInsertMoneyTopic(getDispositivoId());
        this.topicPagamentoEffettuato = TopicManager.getPaymentDoneTopic(getDispositivoId());
    }

    private void subscribeToTopics() throws MqttException {
        client.subscribe(topicInserimentoBiglietto, QOS);
        client.subscribe(topicRispostaTarga, QOS);
        client.subscribe(topicRispostaInfo, QOS);
        client.subscribe(topicInserimentoDenaro, QOS);
        
        System.out.println("Sottoscrizione ai topic completata per dispositivo " + getDispositivoId());
        System.out.println("- " + topicInserimentoBiglietto);
        System.out.println("- " + topicRispostaTarga);
        System.out.println("- " + topicRispostaInfo);
        System.out.println("- " + topicInserimentoDenaro);
        System.out.println("\n");
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
                if (topic.equals(topicInserimentoBiglietto)) gestisciInserimentoBiglietto(jsonMessage);
                else if(topic.equals(this.topicRispostaTarga)) gestisciTargaRicevuta(jsonMessage);
                else if(topic.equals(this.topicRispostaInfo)) gestisciRispostaInfo(jsonMessage);
                else if(topic.equals(this.topicInserimentoDenaro)) gestisciInserimentoDenaro(jsonMessage);
            } catch (Exception e) {
                System.err.println("Errore nella gestione del messaggio: " + payload);
                e.printStackTrace();
            }
        };
        new Thread(task).start();
    }


    private void gestisciInserimentoBiglietto(JsonObject messaggio) throws MqttException {
        this.bigliettoCorrenteId = UUID.fromString(messaggio.get("biglietto_id").getAsString());
        LocalDateTime bigliettoData = LocalDateTime.parse(messaggio.get("biglietto_data").getAsString());
        String caselloingresso = messaggio.get("casello_ingresso").getAsString();
        String targa = messaggio.get("targa").getAsString();

        Biglietto bigliettoRicevuto = new Biglietto(this.bigliettoCorrenteId, bigliettoData, caselloingresso);
        System.out.println("Biglietto inserito: " + this.bigliettoCorrenteId);

        richiediTargaTelecamera(targa);
    }

    private void richiediTargaTelecamera(String targa) throws MqttException {
        Map<String, Object> richiesta = new HashMap<>();
        richiesta.put("tipo", "richiesta_targa");
        richiesta.put("targa", targa);
        richiesta.put("casello", getNome());
        richiesta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        richiesta.put("tipo_dispositivo", "uscita");
        richiesta.put("dispositivo_id", getDispositivoId());

        client.publish(this.topicRichiestaTarga, gson.toJson(richiesta).getBytes(), QOS, false);
        System.out.println("Richiesta targa inviata alla telecamera");
    }

    private void gestisciTargaRicevuta(JsonObject messaggio) throws MqttException {
        if (!messaggio.get("dispositivo_richiedente").getAsString().equals(getDispositivoId())) {
            return;
        }
        this.targaCorrente = messaggio.get("targa").getAsString();
        System.out.println("Targa ricevuta dalla telecamera: " + targaCorrente);
        richiediInfoViaggio();
    }

    private void richiediInfoViaggio() throws MqttException {
        Map<String, Object> richiesta = new HashMap<>();
        richiesta.put("tipo", "richiesta_info_uscita");
        richiesta.put("casello", getNome());
        richiesta.put("timestamp", LocalDateTime.now().toString());
        richiesta.put("biglietto_id", this.bigliettoCorrenteId);
        richiesta.put("targa", targaCorrente);
        richiesta.put("dispositivo_id", getDispositivoId());

        client.publish(this.topicRichiestaInfo, gson.toJson(richiesta).getBytes(), QOS, false);
        System.out.println("Richiesta info viaggio inviata al server");
    }

    private void gestisciRispostaInfo(JsonObject messaggio) throws MqttException {
        String targa = messaggio.get("targa").getAsString();
        double pedaggio = messaggio.get("pedaggio").getAsDouble();
        this.pagamentoInCorso = true;
        this.importoDaPagare = pedaggio;
        LogUtils.simulaSvolgimento("In attesa del pagamento di " + String.format("%.2f", pedaggio) + " EUR da veicolo " + targa);
    }

    private void gestisciInserimentoDenaro(JsonObject messaggio) throws MqttException {
        if (!pagamentoInCorso) {
            System.err.println("Tentativo di pagamento senza richiesta attiva");
            return;
        }

        double importoInserito = messaggio.get("importo").getAsDouble();
        String targa = messaggio.get("targa").getAsString();

        if (importoInserito >= importoDaPagare) {
            double resto = importoInserito - importoDaPagare;

            Map<String, Object> confermaPagamento = new HashMap<>();
            confermaPagamento.put("tipo", "pagamento_completato");
            confermaPagamento.put("resto", resto);
            confermaPagamento.put("targa", targa);
            confermaPagamento.put("casello", getNome());
            confermaPagamento.put("casello_id", getDispositivoId());

            System.out.println("Pagamento completato. Importo: " + String.format("%.2f", importoDaPagare) + " EUR, Resto: " + String.format("%.2f", resto) + " EUR");
           
            // Invia conferma pagamento al server
            client.publish(this.topicPagamentoEffettuato, new MqttMessage(gson.toJson(confermaPagamento).getBytes()));

            this.pagamentoInCorso = false;

        } else {
            // Importo insufficiente
            double mancante = importoDaPagare - importoInserito;
            Map<String, Object> msgMancante = new HashMap<>();
            msgMancante.put("tipo", "importo_insufficiente");
            msgMancante.put("importo_mancante", mancante);
            client.publish(this.topicPagamentoEffettuato, gson.toJson(msgMancante).getBytes(), QOS, false);
            System.out.println("Importo insufficiente. Mancante: " + mancante);
        }
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

        System.out.println("=== Casello Uscita Manuale ===");
        System.out.print("Inserisci il nome del casello (es: MI_NORD): ");
        String nomeCasello = scanner.nextLine().trim().toUpperCase();

        Casello caselloScelto = CaselliJsonDAO.getCaselloByName(nomeCasello);
        if (caselloScelto == null) {
            System.err.println("Casello non trovato. Uscita.");
            scanner.close();
            return;
        }

        CaselloUscitaManuale casello = new CaselloUscitaManuale(caselloScelto);
        new Thread(casello).start();

        System.out.println("\n=== Casello Uscita Manuale Attivo ===");
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
    }
}