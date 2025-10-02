package org.example.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.models.Casello;
import org.example.models.Tratta;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class TratteJsonDAO {

    public static final String PATH_TRATTE = "data" + File.separator + "tratte.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<Tratta> getAllTratte() {
        try (FileReader reader = new FileReader(PATH_TRATTE)) {
            Type listType = new TypeToken<List<Tratta>>() {}.getType();
            List<Tratta> tratte = gson.fromJson(reader, listType);
            return tratte != null ? tratte : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveAllTratte(List<Tratta> tratte) {
        try (FileWriter writer = new FileWriter(PATH_TRATTE)) {
            gson.toJson(tratte, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio delle tratte: " + e.getMessage());
        }
    }

    public static void addTratta(Tratta tratta) {
        List<Tratta> tratte = getAllTratte();
        tratte.add(tratta);
        saveAllTratte(tratte);
    }

    public static void updateTratta(Tratta updatedTratta) {
        List<Tratta> tratte = getAllTratte();
        for (int i = 0; i < tratte.size(); i++) {
            if (tratte.get(i).getId().equals(updatedTratta.getId())) {
                tratte.set(i, updatedTratta);
                break;
            }
        }
        saveAllTratte(tratte);
    }

    public static void removeTrattaById(String id) {
        List<Tratta> tratte = getAllTratte();
        tratte.removeIf(t -> t.getId().equals(id));
        saveAllTratte(tratte);
    }

    public static Tratta getTrattaById(String id) {
        return getAllTratte().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static Tratta getTrattaByCaselli(String nomeIngresso, String nomeUscita) {
        if (nomeIngresso == null || nomeUscita == null) return null;

        return getAllTratte().stream()
                .filter(t -> t.getNomeCaselloIngresso().equalsIgnoreCase(nomeIngresso)
                        && t.getNomeCaselloUscita().equalsIgnoreCase(nomeUscita))
                .findFirst()
                .orElse(null);
    }


    public static double calcolaDistanza(Casello in, Casello out) {
        double dLat = Math.toRadians(out.getLatitudine() - in.getLatitudine());
        double dLon = Math.toRadians(out.getLongitudine() - in.getLongitudine());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(in.getLatitudine()))
                * Math.cos(Math.toRadians(out.getLatitudine()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 6371.0 * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    public static double calcolaPedaggio(double distanza) {
        return distanza * 0.35;
    }

    public static void creaTutteLeTratteDaCaselli(List<Casello> caselli) {
        List<Tratta> tratteEsistenti = getAllTratte(); // recupera quelle già presenti

        for (Casello in : caselli) {
            for (Casello out : caselli) {
                //continue serve saltare il ciclo se la condizione è vera
                if (in.getNome().equals(out.getNome())) continue;

                // Evita duplicati
                if (getTrattaByCaselli(in.getNome(), out.getNome()) != null) continue;

                double distanza = calcolaDistanza(in, out);
                double pedaggio = calcolaPedaggio(distanza);

                Tratta nuovaTratta = new Tratta(in.getNome(), out.getNome(), distanza, pedaggio, 0);
                tratteEsistenti.add(nuovaTratta);
            }
        }

        saveAllTratte(tratteEsistenti); // salva tutto: vecchie + nuove
    }


    public static void removeTratteByCaselloNome(String nomeCasello) {
        List<Tratta> tratte = getAllTratte();
        int originalSize = tratte.size();

        // Rimuove ogni tratta se il nome del casello di ingresso o uscita corrisponde
        tratte.removeIf(tratta ->
                tratta.getNomeCaselloIngresso().equalsIgnoreCase(nomeCasello) ||
                        tratta.getNomeCaselloUscita().equalsIgnoreCase(nomeCasello)
        );

        int removedCount = originalSize - tratte.size();

        if (removedCount > 0) {
            // Salva la lista aggiornata solo se sono state fatte delle modifiche
            saveAllTratte(tratte);
            System.out.println("INFO: Rimosse " + removedCount + " tratte associate al casello '" + nomeCasello + "'.");
        } else {
            System.out.println("INFO: Nessuna tratta trovata da rimuovere per il casello '" + nomeCasello + "'.");
        }
    }

}

