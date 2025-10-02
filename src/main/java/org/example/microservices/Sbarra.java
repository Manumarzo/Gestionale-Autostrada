package org.example.microservices;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.server.ServerCentrale;
import org.example.utils.LogUtils;
import org.example.utils.TopicManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.SSLSocketFactory;

import static org.example.server.ServerCentrale.*;

public class Sbarra implements Runnable, MqttCallback {
    private static final int QOS = 1;
    private static final Gson gson = new Gson();
    private static final String MQTT_SBARRA_USERNAME = "sbarra";
    private static final String MQTT_SBARRA_PASSWORD = "sbarra444";

    private MqttClient mqttClient;
    private String sbarraId;

    //topic
    private String topicConfermaEntrata;
    private String topicSbarraAperta;
    private String topicConfermaUscita;

    public Sbarra(String dispositivoId) {
        this.sbarraId = "SBARRA_" + dispositivoId;

        try {
            // Inizializzazione client MQTT
            this.mqttClient = new MqttClient(ServerCentrale.BROKER_URL, sbarraId, new MemoryPersistence());
            this.mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_SBARRA_USERNAME);
            options.setPassword(MQTT_SBARRA_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            this.mqttClient.connect(options);
            initializeTopics(dispositivoId);
            sottoscriviTopic(dispositivoId);
            LogUtils.logTelecamera(this.sbarraId, "Avvio effettuato correttamente");

        } catch (MqttException e) {
            LogUtils.logErr("Errore nell'inizializzazione della sbarra MQTT", e);
            throw new RuntimeException("Impossibile inizializzare la sbarra", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics(String caselloId){
        this.topicConfermaEntrata = TopicManager.getEnteringConfirmTopic(caselloId);
        this.topicSbarraAperta = TopicManager.getOpenBarTopic(caselloId);
        this.topicConfermaUscita = TopicManager.getExitConfirmTopic(caselloId);
    }

    private void sottoscriviTopic(String caselloId) throws MqttException {
        mqttClient.subscribe(this.topicConfermaEntrata, QOS);
        mqttClient.subscribe(this.topicConfermaUscita, QOS);
        LogUtils.logSbarra(this.sbarraId, "Sottoscritta al topic " + this.topicConfermaEntrata);
        LogUtils.logSbarra(this.sbarraId, "Sottoscritta al topic" + this.topicConfermaUscita);
    }

    @Override
    public void connectionLost(Throwable cause) {
        LogUtils.logErr("Connessione MQTT persa: " + cause.getMessage(), null);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        LogUtils.logSbarra(this.sbarraId, "Messaggio ricevuto: " + payload);

        new Thread(() -> {
            try {
                JsonObject jsonMessage = JsonParser.parseString(payload).getAsJsonObject();
                if(topic.equals(this.topicConfermaEntrata)) apriSbarra(jsonMessage);
                else if(topic.equals(this.topicConfermaUscita)) apriSbarra(jsonMessage);

            } catch (Exception e) {
                LogUtils.logErr("Errore nella gestione del messaggio", e);
            }
        }).start();
    }

    private void apriSbarra(JsonObject message){
        try{
            LogUtils.simulaSvolgimento("SBARRA APERTA");

            String targa = message.get("targa").getAsString();

            Map<String, Object> messaggioRisposta = new HashMap<>();
            messaggioRisposta.put("sbarra", this.sbarraId);
            messaggioRisposta.put("azione", "aperta");
            messaggioRisposta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messaggioRisposta.put("targa", targa);


            String messaggioJson = gson.toJson(messaggioRisposta);

            mqttClient.publish(topicSbarraAperta, messaggioJson.getBytes(), 1, false);
            Thread.sleep(3000);
            LogUtils.simulaSvolgimento("SBARRA CHIUSA");
        } catch (Exception e) {
            LogUtils.logErr("Errore fatale nella gestione richiesta apertura", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public void disconnetti() {
        LogUtils.logSbarra(this.sbarraId, "Disconnessione in corso");
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                LogUtils.logSbarra(this.sbarraId, "Disconnessione effettuata");
            }
        } catch (MqttException e) {
            LogUtils.logErr("Errore nella disconnessione della sbarra", e);
        }
    }

    @Override
    public void run() {
    }
}
