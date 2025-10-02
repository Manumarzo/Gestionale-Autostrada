package org.example;

import org.example.microservices.*;
import org.example.models.Casello;
import org.example.models.Veicolo;
import org.example.dao.CaselliJsonDAO;
import org.example.utils.LogUtils;
import org.example.dao.TratteJsonDAO;
import org.example.dao.VeicoliJsonDAO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String PATH_VEICOLI_JSON = "src" + File.separator + "main" + File.separator + "java" + File.separator + "org" + File.separator + "example" + File.separator + "resources" + File.separator + "veicoli.json";

    public static void main(String[] args) {
        LogUtils.simulaSvolgimento("AVVIO SIMULAZIONE AUTOSTRADALE");

        // 1. Avvio dei servizi di infrastruttura
        System.out.println("\nAvvio dei servizi di infrastruttura...");
        //new ServerCentrale();
        Casello caselloTemp;
        List<Casello> listaCaselli = new ArrayList<>();
        for(Casello temp: CaselliJsonDAO.getCaselli()){
            caselloTemp = new CaselloIngressoManuale(temp);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloIngressoAutomatico(temp);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloUscitaManuale(temp);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloUscitaAutomatico(temp);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
        }

        CaselliJsonDAO.addCaselliInUso(listaCaselli);
        //aggiunta file con tratte (distanza e pedaggi tra coppie di caselli)
        TratteJsonDAO.creaTutteLeTratteDaCaselli(CaselliJsonDAO.getCaselli());

        // 2. Preparazione e registrazione dei veicoli
        System.out.println("\nRegistrazione veicoli nel sistema...");
        List<Veicolo> veicoliDaAvviare = new ArrayList<>();

        veicoliDaAvviare.add(new Veicolo(true));
        veicoliDaAvviare.add(new Veicolo(false));

        //da vedere la risposta del server per i caselli automatici, diversa rispetto ai manuali
        VeicoliJsonDAO.salvaVeicoli(veicoliDaAvviare);
        // 3. Avvio della simulazione
        LogUtils.simulaSvolgimento("Avvio viaggi");
        try {
            Thread.sleep(3000);
            for (Veicolo v : veicoliDaAvviare) {
                System.out.println("Parte il veicolo: " + v.getTarga() + (v.hasTelepass() ? " (Telepass)" : " (Manuale)"));
                new Thread(v).start();
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}