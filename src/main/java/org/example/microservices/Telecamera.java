package org.example.microservices;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.example.server.ServerCentrale;
import org.example.utils.LogUtils;
import org.example.utils.TopicManager;

import javax.net.ssl.SSLSocketFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.example.server.ServerCentrale.*;

public class Telecamera implements Runnable, MqttCallback {

    private static final int QOS = 1;
    private static final Gson gson = new Gson();
    private static final String MQTT_CAM_USERNAME = "telecamera";
    private static final String MQTT_CAM_PASSWORD = "cam333";

    private final String telecameraId;
    private MqttClient mqttClient;
    private final Random random;

    //Topic
    private String topicRichiestaManInTarga;
    private String topicLetturaManInTarga;
    private String topicRichiestaManOutTarga;
    private String topicLetturaManOutTarga;
    private String topicRichiestaAutoInTarga;
    private String topicLetturaAutoInTarga;
    private String topicRichiestaAutoOutTarga;
    private String topicLetturaAutoOutTarga;


    public Telecamera(String idCasello, String tipoTelecamera) {
        // "ingresso" o "uscita"
        this.telecameraId = "CAM_" + idCasello;
        this.random = new Random();

        try {
            // Inizializzazione client MQTT
            this.mqttClient = new MqttClient(ServerCentrale.BROKER_URL, telecameraId, new MemoryPersistence());
            this.mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setUserName(MQTT_CAM_USERNAME);
            options.setPassword(MQTT_CAM_PASSWORD.toCharArray());

            SSLSocketFactory socketFactory = TopicManager.getSocketFactory(CA_FILE_PATH,CLIENT_CRT_FILE_PATH, CLIENT_KEY_FILE_PATH,"");
            options.setSocketFactory(socketFactory);

            this.mqttClient.connect(options);
            initializeTopics(idCasello);
            sottoscriviTopic();
            LogUtils.logTelecamera(this.telecameraId, "Avvio effettuato correttamente");

        } catch (MqttException e) {
            LogUtils.logErr("Errore nell'inizializzazione della telecamera MQTT", e);
            throw new RuntimeException("Impossibile inizializzare la telecamera", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTopics(String idCasello) {
        this.topicRichiestaManInTarga = TopicManager.getManInPlateRequestTopic(idCasello);
        this.topicLetturaManInTarga = TopicManager.getManInPlateResponseTopic(idCasello);
        this.topicRichiestaManOutTarga = TopicManager.getManOutPlateRequestTopic(idCasello);
        this.topicLetturaManOutTarga = TopicManager.getManoutPlateResponseTopic(idCasello);
        this.topicRichiestaAutoInTarga = TopicManager.getAutoInPlateRequestTopic(idCasello);
        this.topicLetturaAutoInTarga = TopicManager.getAutoInPlateResponseTopic(idCasello);
        this.topicRichiestaAutoOutTarga = TopicManager.getAutoOutPlateRequestTopic(idCasello);
        this.topicLetturaAutoOutTarga = TopicManager.getAutoOutPlateResponseTopic(idCasello);
    }

    private void sottoscriviTopic() throws MqttException {
        mqttClient.subscribe(this.topicRichiestaManInTarga, QOS);
        mqttClient.subscribe(this.topicRichiestaManOutTarga, QOS);
        mqttClient.subscribe(this.topicRichiestaAutoInTarga, QOS);
        mqttClient.subscribe(this.topicRichiestaAutoOutTarga, QOS);
        LogUtils.logTelecamera(this.telecameraId, " sottoscritta al topic: " + topicRichiestaManInTarga);
        LogUtils.logTelecamera(this.telecameraId, " sottoscritta al topic: " + topicRichiestaManOutTarga);
        LogUtils.logTelecamera(this.telecameraId, " sottoscritta al topic: " + topicRichiestaAutoInTarga);
        LogUtils.logTelecamera(this.telecameraId, " sottoscritta al topic: " + topicRichiestaAutoOutTarga);
    }

    @Override
    public void connectionLost(Throwable cause) {
        LogUtils.logErr("Connessione MQTT persa: " + cause.getMessage(), null);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        LogUtils.logTelecamera(telecameraId, "Messaggio ricevuto: " + payload);
        System.out.println(topic + " " + this.topicRichiestaManOutTarga + " " + this.topicRichiestaAutoOutTarga);
        // Gestisce ogni richiesta in un nuovo thread per non bloccare il client MQTT
        new Thread(() -> {
            try {
                JsonObject jsonMessage = JsonParser.parseString(payload).getAsJsonObject();
                if (topic.equals(this.topicRichiestaManInTarga)) gestisciRichiestaTargaInMan(jsonMessage);
                else if(topic.equals(this.topicRichiestaManOutTarga)) gestisciRichiestaTargaOutMan(jsonMessage);
                else if(topic.equals(this.topicRichiestaAutoInTarga)) gestisciRichiestaTargaInAuto(jsonMessage);
                else if(topic.equals(this.topicRichiestaAutoOutTarga)) gestisciRichiestaTargaOutAuto(jsonMessage);
            } catch (Exception e) {
                LogUtils.logErr("Errore nella gestione del messaggio", e);
            }
        }).start();
    }

    private void gestisciRichiestaTargaInMan(JsonObject message){
        try{
            if(message.has("targa")){
                String targa = message.get("targa").getAsString();
                String dispositivoRichiedente = message.get("dispositivo_id").getAsString();
                Thread.sleep(500 + random.nextInt(1000));

                Map<String, Object> messaggioRisposta = new HashMap<>();
                messaggioRisposta.put("targa", targa);
                messaggioRisposta.put("dispositivo_richiedente", dispositivoRichiedente);
                messaggioRisposta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messaggioRisposta.put("telecamera_id", telecameraId);

                String messaggioJson = gson.toJson(messaggioRisposta);

                LogUtils.logTelecamera(this.telecameraId, "Invio risposta a " + dispositivoRichiedente + " con targa " + targa);

                mqttClient.publish(topicLetturaManInTarga, new MqttMessage(messaggioJson.getBytes()));
            } else {
                LogUtils.logErr("Impossibile leggere la targa", null);
                return;
            }
        } catch(Exception e){
            LogUtils.logErr("Errore nella gestione della richiesta della targa", e);
        }
    }

    private void gestisciRichiestaTargaOutMan(JsonObject message){
        try{
            if(message.has("targa")){
                LogUtils.logTelecamera(this.telecameraId, "Leggo targa del veicolo");
                String targa = message.get("targa").getAsString();
                String dispositivoRichiedente = message.get("dispositivo_id").getAsString();
                Thread.sleep(500 + random.nextInt(1000));

                Map<String, Object> messaggioRisposta = new HashMap<>();
                messaggioRisposta.put("targa", targa);
                messaggioRisposta.put("dispositivo_richiedente", dispositivoRichiedente);
                messaggioRisposta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messaggioRisposta.put("telecamera_id", telecameraId);

                String messaggioJson = gson.toJson(messaggioRisposta);

                LogUtils.logTelecamera(this.telecameraId, "Invio risposta a " + dispositivoRichiedente + " con targa " + targa);

                mqttClient.publish(topicLetturaManOutTarga, new MqttMessage(messaggioJson.getBytes()));

            } else {
                LogUtils.logErr("Impossibile leggere la targa", null);
            }
        } catch(Exception e){
            LogUtils.logErr("Errore nella gestione della richiesta della targa", e);
        }
    }

    private void gestisciRichiestaTargaInAuto(JsonObject message){
        try{
            if(message.has("targa")){
                String nomeCasello = message.get("casello").getAsString();
                LogUtils.logTelecamera(this.telecameraId, "Richiesta lettura targa da " + nomeCasello);
                String targa = message.get("targa").getAsString();
                String dispositivoRichiedente = message.get("dispositivo_id").getAsString();
                String telepassId = message.get("telepass_id").getAsString();
                Thread.sleep(500 + random.nextInt(1000));

                Map<String, Object> messaggioRisposta = new HashMap<>();
                messaggioRisposta.put("targa", targa);
                messaggioRisposta.put("dispositivo_richiedente", dispositivoRichiedente);
                messaggioRisposta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messaggioRisposta.put("telecamera_id", telecameraId);
                messaggioRisposta.put("telepass_id", telepassId);

                String messaggioJson = gson.toJson(messaggioRisposta);

                LogUtils.logTelecamera(this.telecameraId, "Invio risposta a " + dispositivoRichiedente + " con targa " + targa);

                mqttClient.publish(topicLetturaAutoInTarga, new MqttMessage(messaggioJson.getBytes()));

            } else {
                LogUtils.logErr("Impossibile leggere la targa", null);
            }
        } catch(Exception e){
            LogUtils.logErr("Errore nella gestione della richiesta della targa", e);
        }
    }

    private void gestisciRichiestaTargaOutAuto(JsonObject message){
        try{
            if(message.has("targa")){
                String nomeCasello = message.get("casello").getAsString();
                LogUtils.logTelecamera(this.telecameraId, "Richiesta lettura targa da " + nomeCasello);
                String targa = message.get("targa").getAsString();
                String dispositivoRichiedente = message.get("dispositivo_id").getAsString();
                String telepassId = message.get("telepass_id").getAsString();
                Thread.sleep(500 + random.nextInt(1000));

                Map<String, Object> messaggioRisposta = new HashMap<>();
                messaggioRisposta.put("targa", targa);
                messaggioRisposta.put("dispositivo_richiedente", dispositivoRichiedente);
                messaggioRisposta.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messaggioRisposta.put("telecamera_id", telecameraId);
                messaggioRisposta.put("telepass_id", telepassId);

                String messaggioJson = gson.toJson(messaggioRisposta);

                LogUtils.logTelecamera(this.telecameraId, "Invio risposta a " + dispositivoRichiedente + " con targa " + targa + " e topic " + this.topicLetturaAutoOutTarga);

                mqttClient.publish(topicLetturaAutoOutTarga, new MqttMessage(messaggioJson.getBytes()));

            } else {
                LogUtils.logErr("Impossibile leggere la targa", null);
            }
        } catch(Exception e){
            LogUtils.logErr("Errore nella gestione della richiesta della targa", e);
        }
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato in questo scenario
    }

    public void disconnetti() {
        LogUtils.logTelecamera(this.telecameraId, "Disconnessione in corso");
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                LogUtils.logTelecamera(this.telecameraId, "Disconnessione effettuata");
            }
        } catch (MqttException e) {
            LogUtils.logErr("Errore nella disconnessione della telecamera", e);
        }
    }

    @Override
    public void run() {
    }
}