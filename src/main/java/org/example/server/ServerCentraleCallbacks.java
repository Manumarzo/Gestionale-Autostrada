package org.example.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.example.dao.*;
import org.example.models.Casello;
import org.example.models.Multa;
import org.example.models.PagamentoTelepass;
import org.example.models.Tratta;
import org.example.models.VeicoloInTransito;
import org.example.models.Viaggio;
import org.example.utils.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ServerCentraleCallbacks implements MqttCallback {

    private static final Random random = new Random();
    private final Gson gson;
    private final MqttClient client;
    private final Map<String, VeicoloInTransito> veicoliInTransito;
    private final Map<String, Viaggio> viaggi;

    private String topicRispostaInfo;

    public ServerCentraleCallbacks(MqttClient client, Gson gson, Map<String, VeicoloInTransito> veicoliInTransito, Map<String, Viaggio> viaggi) {
        this.client = client;
        this.gson = gson;
        this.veicoliInTransito = veicoliInTransito;
        this.viaggi = viaggi;
    }

    @Override
    public void connectionLost(Throwable cause) {
        cause.printStackTrace();
        System.err.println("Connessione MQTT persa: " + cause.getMessage());
        try {
            if (!client.isConnected()) {
                client.reconnect();
                System.out.println("Riconnesso al broker MQTT");
            }
        } catch (MqttException e) {
            System.err.println("Errore durante la riconnessione: " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        LogUtils.logMessageArrived(topic, message);
        try {
            JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);
            if (topic.endsWith("biglietto_erogato")) gestisciEntrataManuale(jsonMessage);
            else if (topic.endsWith("richiesta_info")) gestisciRichiestaInfo(jsonMessage);
            else if (topic.endsWith("pagamento_effettuato")) gestisciPagamentoEffettuato(jsonMessage);
            else if (topic.endsWith("ingresso/automatico/invia_dati")) gestisciInvioDati(jsonMessage, true);
            else if (topic.endsWith("uscita/automatico/invia_dati")) gestisciInvioDati(jsonMessage, false);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore durante l'elaborazione del messaggio: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    // ========== METODI PRIVATI DI GESTIONE ==========

    private void gestisciEntrataManuale(JsonObject message) {
        String nomeCasello = message.get("casello").getAsString();
        String idCasello = message.get("id_casello").getAsString();
        String timestamp = message.get("biglietto_data").getAsString();
        String bigliettoId = message.get("biglietto_id").getAsString();
        String targa = message.get("targa").getAsString();

        VeicoloInTransito veicolo = new VeicoloInTransito();
        veicolo.setTarga(targa);
        veicolo.setBigliettoId(bigliettoId);
        veicolo.setCaselloIngresso(nomeCasello);
        veicolo.setTimestampIngresso(timestamp);
        veicolo.setTipoIngresso("manuale");

        VeicoliJsonDAO.saveVeicoloInTransito(veicolo);
        veicoliInTransito.put(bigliettoId, veicolo);
        System.out.println("*** Veicolo: "
                + targa
                + " con biglietto: "
                +bigliettoId
                + " entrato con successo e salvato su file! ***");

        inviaConfermaEntrata(idCasello, targa);
    }

    private void gestisciRichiestaInfo(JsonObject message) {
        String idCasello = message.get("dispositivo_id").getAsString();
        String bigliettoId = message.get("biglietto_id").getAsString();
        String targa = message.get("targa").getAsString();
        String timestampUscita = message.get("timestamp").getAsString();
        String nomeCasello = message.get("casello").getAsString();

        topicRispostaInfo = TopicManager.getResponseInfoTopic(idCasello);

        VeicoloInTransito veicolo = veicoliInTransito.get(bigliettoId);
        if (veicolo == null || !veicolo.getTarga().equals(targa)) {
            inviaInfoViaggio(nomeCasello, veicolo, "Biglietto non valido o targa non corrispondente");
            return;
        }

        Casello caselloIng = CaselliJsonDAO.getCaselloByName(veicolo.getCaselloIngresso());
        Casello caselloOut = CaselliJsonDAO.getCaselloByName(nomeCasello);

        Tratta t = TratteJsonDAO.getTrattaByCaselli(caselloIng.getNome(), caselloOut.getNome());

        double distanza = t.getDistanza();
        double pedaggio = t.getPedaggio();

        double velocitaMedia = calcolaVelocitaMedia();
        boolean multaVelocita = velocitaMedia > 130.0;

        System.out.printf(
                "***\nVeicolo: %s" +
                        "\ncaselloIngresso: %s" +
                        "\ncaselloUscita: %s" +
                        "\ndistanza: %.2f" +
                        "\npedaggio: %.2f" +
                        "\nvelocitaMedia: %.2f" +
                        "\nmultaVelocita: %b" +
                        "\n***\n",
                targa,
                caselloIng.getNome(),
                caselloOut.getNome(),
                distanza,
                pedaggio,
                velocitaMedia,
                multaVelocita
        );

        inviaInfoViaggio(idCasello, nomeCasello, veicolo, timestampUscita, pedaggio, distanza, velocitaMedia, multaVelocita);
    }

    private void gestisciPagamentoEffettuato(JsonObject message) {
        if (!message.get("tipo").getAsString().equals("pagamento_completato")) return;

        String idCasello = message.get("casello_id").getAsString();
        String targa = message.get("targa").getAsString();
        try {
            JsonObject conferma = new JsonObject();
            conferma.addProperty("tipo", "conferma_uscita");
            conferma.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            conferma.addProperty("targa", targa);

            String topic = TopicManager.getExitConfirmTopic(idCasello);
            client.publish(topic, gson.toJson(conferma).getBytes(), 1, false);
            VeicoliJsonDAO.extractVeicoloInTransito(targa);

            LogUtils.simulaSvolgimento("Veicolo " + targa + " uscito con successo ed estratto dal file");

            Viaggio v = viaggi.remove(targa);
            if(v != null){
                ViaggiJsonDAO.aggiungiViaggio(v);
                Tratta t = TratteJsonDAO.getTrattaByCaselli(v.getCaselloIngresso(), v.getCaselloUscita());
                assert t!= null;
                t.setUtilizzo(t.getUtilizzo() + 1);
                TratteJsonDAO.updateTratta(t);
                LogUtils.simulaSvolgimento("Viaggio del veicolo manuale" + targa + " registrato");
            } else {
                LogUtils.logErr("Viaggio non registrato correttamente", null);
            }

        } catch (MqttException e) {
            LogUtils.logErr("Errore nell'invio della conferma uscita", e);
        }
    }

    private void gestisciInvioDati(JsonObject message, boolean ingresso) {
        String nomeCasello = message.get("casello").getAsString();
        String idCasello = message.get("dispositivo_id").getAsString();
        String telepassId = message.get("telepass_id").getAsString();
        String targa = message.get("targa").getAsString();
        String timestamp = message.get("timestamp").getAsString();

        if (ingresso) {
            VeicoloInTransito veicolo = new VeicoloInTransito();
            veicolo.setTarga(targa);
            veicolo.setTelepassId(telepassId);
            veicolo.setCaselloIngresso(nomeCasello);
            veicolo.setTimestampIngresso(timestamp);
            veicolo.setTipoIngresso("automatico");

            VeicoliJsonDAO.saveVeicoloInTransito(veicolo);
            veicoliInTransito.put(telepassId, veicolo);
            System.out.println("*** Veicolo: "
                    + targa
                    + " con Telepass: "
                    + telepassId
                    +" entrato con successo e salvato su file! ***");


            inviaConfermaEntrata(idCasello, targa);
        } else {
            VeicoloInTransito veicolo = VeicoliJsonDAO.extractVeicoloInTransito(targa);

            Casello caselloIng = CaselliJsonDAO.getCaselloByName(veicolo.getCaselloIngresso());
            Casello caselloOut = CaselliJsonDAO.getCaselloByName(nomeCasello);

            Tratta t = TratteJsonDAO.getTrattaByCaselli(caselloIng.getNome(), caselloOut.getNome());
            t.setUtilizzo(t.getUtilizzo() + 1); //modifichiamo l'utilizzo della tratta

            double distanza = t.getDistanza();
            double pedaggio = t.getPedaggio();

            double velocitaMedia = calcolaVelocitaMedia();
            boolean multaVelocita = velocitaMedia > 130.0;

            System.out.printf(
                    "***\nVeicolo: %s" +
                    "\ncaselloIngresso: %s" +
                    "\ncaselloUscita: %s" +
                    "\ndistanza: %.2f" +
                    "\npedaggio: %.2f" +
                    "\nvelocitaMedia: %.2f" +
                    "\nmultaVelocita: %b" +
                    "\n***\n",
                    targa,
                    caselloIng.getNome(),
                    caselloOut.getNome(),
                    distanza,
                    pedaggio,
                    velocitaMedia,
                    multaVelocita
            );

            LogUtils.simulaSvolgimento("Veicolo " + targa + "con Telepass " + telepassId + " uscito con successo ");

            Viaggio viaggio = new Viaggio("automatico", targa, null, telepassId, veicolo.getCaselloIngresso(), nomeCasello, veicolo.getTimestampIngresso(), timestamp, pedaggio, distanza, velocitaMedia, false);

            try {
                viaggio.setMulta(registraMulte(velocitaMedia, viaggio.getId()));
                ViaggiJsonDAO.aggiungiViaggio(viaggio);
                TratteJsonDAO.updateTratta(t);
                LogUtils.simulaSvolgimento("Viaggio di " + targa + " memorizzato con successo ");
            } catch (Exception e){
                LogUtils.logErr("Errore durante il salvataggio del viaggio", e);
            }

            inviaConfermaUscitaAutomatica(idCasello, telepassId, targa, pedaggio, velocitaMedia, multaVelocita, viaggio.getId());
        }
    }

    // ========== METODI DI SUPPORTO ==========


    private double calcolaVelocitaMedia() {
        if(random.nextDouble(1) < 0.1){
            return random.nextDouble(130, 200);
        } else return random.nextDouble(90, 130);
    }

    private boolean registraMulte(double velocita_media, String idViaggio){
        List<Multa> multe = new ArrayList<>();
        if(velocita_media > 180){
            multe.add(new Multa(idViaggio, "Velocità molto elevata", 340));
        } else if(velocita_media > 140){
            multe.add(new Multa(idViaggio, "Velocità elevata", 150.30));
        } else if(velocita_media > 130){
            multe.add(new Multa(idViaggio, "Velocità oltre il limite", 50.50));
        }
        if(random.nextDouble(1) < 0.1){
            String[] motivazioni = {"Sorpasso illecito", "Assicurazione scaduta", "Guida in stato di ebbrezza"};
            multe.add(new Multa(idViaggio, motivazioni[random.nextInt(motivazioni.length)], 200));
        }
        for(Multa m: multe){
            System.out.println("Multa registrata per viaggio " + idViaggio + ": " + m.getMotivazione());
        }
        if(!multe.isEmpty()){
            for(Multa m: multe) MulteJsonDAO.addMulta(m);
            return true;
        } else return false;
    }

    private void inviaConfermaEntrata(String idCasello, String targa) {
        try {
            JsonObject conferma = new JsonObject();
            conferma.addProperty("tipo", "conferma_entrata");
            conferma.addProperty("targa", targa);
            client.publish(TopicManager.getEnteringConfirmTopic(idCasello), gson.toJson(conferma).getBytes(), 1, false);
        } catch (MqttException e) {
            System.err.println("Errore nell'invio della conferma entrata: " + e.getMessage());
        }
    }

    private void inviaInfoViaggio(String nomeCasello, VeicoloInTransito v, String errore) {
        try {
            JsonObject info = new JsonObject();
            info.addProperty("tipo", "info_viaggio");
            info.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            info.addProperty("status", "ERROR");
            info.addProperty("descrizione", errore);
            if (v != null) {
                info.addProperty("targa", v.getTarga());
                info.addProperty("biglietto_id", v.getBigliettoId());
            }
            client.publish(topicRispostaInfo, gson.toJson(info).getBytes(), 1, false);
        } catch (MqttException e) {
            System.err.println("Errore nell'invio info viaggio: " + e.getMessage());
        }
    }

    private void inviaInfoViaggio(String idCasello, String caselloOut, VeicoloInTransito v, String tOut, double pedaggio, double dist, double vMedia, boolean multa) {
        try {
            JsonObject info = new JsonObject();
            info.addProperty("tipo", "info_viaggio");
            info.addProperty("biglietto_id", v.getBigliettoId());
            info.addProperty("targa", v.getTarga());
            info.addProperty("casello_ingresso", v.getCaselloIngresso());
            info.addProperty("timestamp_ingresso", v.getTimestampIngresso());
            info.addProperty("casello_uscita", caselloOut);
            info.addProperty("timestamp_uscita", tOut);
            info.addProperty("pedaggio", pedaggio);
            info.addProperty("distanza_km", dist);
            info.addProperty("velocita_media", vMedia);
            info.addProperty("multa_velocita", multa);

            Viaggio viaggio = new Viaggio("manuale", v.getTarga(), v.getBigliettoId(), null, v.getCaselloIngresso(), caselloOut, v.getTimestampIngresso(), tOut, pedaggio, dist, vMedia, false);
            viaggio.setMulta(registraMulte(vMedia, viaggio.getId()));
            viaggi.put(v.getTarga(), viaggio);

            client.publish(topicRispostaInfo, gson.toJson(info).getBytes(), 1, false);
        } catch (MqttException e) {
            System.err.println("Errore nell'invio info viaggio: " + e.getMessage());
        }
    }

    private void inviaConfermaUscitaAutomatica(String idCasello, String telepassId, String targa, double pedaggio, double velocitaMedia, boolean multaVelocita, String viaggioId) {
        try {
            JsonObject conferma = new JsonObject();
            conferma.addProperty("tipo", "conferma_uscita");
            conferma.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            conferma.addProperty("status", "OK");
            conferma.addProperty("descrizione", "Uscita autorizzata");
            conferma.addProperty("telepass_id", telepassId);
            conferma.addProperty("targa", targa);
            conferma.addProperty("pedaggio", pedaggio);
            conferma.addProperty("velocita_media", velocitaMedia);
            conferma.addProperty("multa_velocita", multaVelocita);

            PagamentiTelepassJsonDAO.addPagamento(new PagamentoTelepass(telepassId, pedaggio, viaggioId));

            String topic = TopicManager.getExitConfirmTopic(idCasello);
            client.publish(topic, gson.toJson(conferma).getBytes(), 1, false);
        } catch (MqttException e) {
            System.err.println("Errore invio conferma uscita automatica: " + e.getMessage());
        }
    }
}

