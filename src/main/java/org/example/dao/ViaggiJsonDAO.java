package org.example.dao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import org.example.models.Viaggio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.utils.LogUtils;

public class ViaggiJsonDAO {
    public static String PATH_VIAGGI = "data" + File.separator + "viaggi.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ViaggiJsonDAO() {}

    public static String aggiungiViaggio(Viaggio viaggio) {
        try {
            File file = new File(PATH_VIAGGI);
            List<Viaggio> viaggi = new ArrayList<>();

            // Se il file esiste leggiamo i viaggi esistenti
            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    Type listType = new TypeToken<List<Viaggio>>() {}.getType();
                    viaggi = gson.fromJson(reader, listType);
                    if (viaggi == null) viaggi = new ArrayList<>();
                }
            } else {
                // Se non esiste creiamo le directory mancanti
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            // Aggiungiamo il nuovo viaggio
            viaggi.add(viaggio);

            // Sovrascriviamo il file
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(viaggi, writer);
            }
            return viaggio.getId();

        } catch (IOException e) {
            LogUtils.logErr("Errore durante il salvataggio del viaggio", e);
            return null;
        }
    }

    public static Viaggio findViaggio(String idViaggio) {
        try (Reader reader = new FileReader(PATH_VIAGGI)) {
            Type listType = new TypeToken<List<Viaggio>>() {}.getType();
            List<Viaggio> viaggi = gson.fromJson(reader, listType);
            if (viaggi == null) return null;

            return viaggi.stream()
                    .filter(v -> idViaggio.equals(v.getId()))
                    .findFirst()
                    .orElse(null);

        } catch (IOException e) {
            LogUtils.logErr("Errore durante la lettura dei viaggi", e);
            return null;
        }
    }

    public static List<Viaggio> leggiViaggi() {
        File file = new File(PATH_VIAGGI);
        if (!file.exists()) {
            return new ArrayList<>(); // Se il file non esiste, ritorna una lista vuota
        }
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Viaggio>>() {}.getType();
            List<Viaggio> viaggi = gson.fromJson(reader, listType);
            return (viaggi != null) ? viaggi : new ArrayList<>();
        } catch (IOException e) {
            LogUtils.logErr("Errore durante la lettura dei viaggi", e);
            return new ArrayList<>(); // Ritorna lista vuota in caso di errore
        }
    }
}
