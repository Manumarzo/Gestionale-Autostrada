package org.example.utils;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class LogUtils {
    public static void logMessageArrived(String topic, MqttMessage message){
        String payload = new String(message.getPayload());
        System.out.println("\n--- Messaggio ricevuto ---");
        System.out.println("Topic: " + topic);
        System.out.println("Payload: " + payload);
    }

    public static void logMessageArrived(String topic, String payload){
        System.out.println("\n--- Messaggio ricevuto ---");
        System.out.println("Topic: " + topic);
        System.out.println("Payload: " + payload);
    }

    public static void logTelecamera(String id, String msg){
        System.out.println("[Telecamera " + id + "] " + msg);
    }

    public static void logSbarra(String id, String msg){
        System.out.println("[Sbarra " + id + "] " + msg);
    }

    public static void logErr(String message, Exception e) {
        System.err.println("[ERRORE] " + message);
        if (e != null) {
            System.err.println("Dettagli eccezione: " + e.getMessage());
        }
    }

    public static void simulaSvolgimento(String message){
        System.out.println("\n--- " + message + " ---");
    }

    public static void logVeicolo(String targa, String msg){
        System.out.println("[Veicolo " + targa + "] " + msg);
    }

}
