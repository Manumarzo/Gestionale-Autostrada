package org.example.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.models.Multa;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class MulteJsonDAO {

    public static String PATH_MULTE = "data" + File.separator + "multe.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<Multa> getAllMulte() {
        try (FileReader reader = new FileReader(PATH_MULTE)) {
            Type listType = new TypeToken<List<Multa>>() {}.getType();
            List<Multa> multe = gson.fromJson(reader, listType);
            return multe != null ? multe : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveAllMulte(List<Multa> multe) {
        try (FileWriter writer = new FileWriter(PATH_MULTE)) {
            gson.toJson(multe, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio delle multe: " + e.getMessage());
        }
    }

    public static void addMulta(Multa multa) {
        List<Multa> multe = getAllMulte();
        multe.add(multa);
        saveAllMulte(multe);
    }

    public static void removeMultaById(String id) {
        List<Multa> multe = getAllMulte();
        multe.removeIf(m -> m.getId().equals(id));
        saveAllMulte(multe);
    }

    public static Multa getMultaById(String id) {
        return getAllMulte().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static List<Multa> getMulteByViaggioId(String idViaggio) {
        return getAllMulte().stream()
                .filter(m -> m.getIdViaggio().equals(idViaggio))
                .toList();
    }
}
