package org.example.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.example.models.Veicolo;
import org.example.models.VeicoloInTransito;
import org.example.utils.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class VeicoliJsonDAO {

    public static String PATH_VEICOLI_IN_TRANSITO = "data" + File.separator + "veicoli_in_transito.json";
    public static String PATH_VEICOLI_REGISTRATI = "src" + File.separator + "main" + File.separator + "java" + File.separator + "org" + File.separator + "example" + File.separator + "resources" + File.separator + "veicoli.json";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Random random = new Random();

    private VeicoliJsonDAO() {}

    private static class VeicoloRegistratoData {
        String targa;
        boolean hasTelepass;
    }

    private static List<VeicoloRegistratoData> getVeicoliRegistrati() {
        try (FileReader reader = new FileReader(PATH_VEICOLI_REGISTRATI)) {
            Type listType = new TypeToken<ArrayList<VeicoloRegistratoData>>(){}.getType();
            List<VeicoloRegistratoData> veicoli = gson.fromJson(reader, listType);
            return veicoli != null ? veicoli : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Errore leggendo la master list dei veicoli (veicoli.json): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static String trovaTargaManualeDisponibile() {
        List<VeicoloRegistratoData> tuttiVeicoli = getVeicoliRegistrati();
        List<VeicoloInTransito> veicoliInTransitoList = getTuttiVeicoliInTransito();

        Set<String> targheInTransito = veicoliInTransitoList.stream()
                .map(VeicoloInTransito::getTarga)
                .collect(Collectors.toSet());

        List<VeicoloRegistratoData> veicoliDisponibili = tuttiVeicoli.stream()
                .filter(v -> !v.hasTelepass && !targheInTransito.contains(v.targa))
                .collect(Collectors.toList());

        if (veicoliDisponibili.isEmpty()) {
            return null;
        }

        return veicoliDisponibili.get(random.nextInt(veicoliDisponibili.size())).targa;
    }

    private static List<VeicoloInTransito> getTuttiVeicoliInTransito() {
        try (FileReader reader = new FileReader(PATH_VEICOLI_IN_TRANSITO)) {
            Type listType = new TypeToken<List<VeicoloInTransito>>() {}.getType();
            List<VeicoloInTransito> veicoli = gson.fromJson(reader, listType);
            return veicoli != null ? veicoli : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static VeicoloInTransito getVeicoloInTransito(String targa) {
        return getTuttiVeicoliInTransito().stream()
                .filter(v -> v.getTarga().equalsIgnoreCase(targa))
                .findFirst()
                .orElse(null);
    }


    public static VeicoloInTransito getVeicoloInTransitoByBiglietto(String bigliettoId) {
        if (bigliettoId == null || bigliettoId.trim().isEmpty()) {
            return null;
        }
        return getTuttiVeicoliInTransito().stream()
                .filter(v -> bigliettoId.equals(v.getBigliettoId()))
                .findFirst()
                .orElse(null);
    }

    public static VeicoloInTransito extractVeicoloInTransito(String targa) {
        List<VeicoloInTransito> veicoli = getTuttiVeicoliInTransito();
        VeicoloInTransito veicoloTrovato = null;

        for (VeicoloInTransito v : veicoli) {
            if (v.getTarga().equalsIgnoreCase(targa)) {
                veicoloTrovato = v;
                break;
            }
        }

        if (veicoloTrovato != null) {
            veicoli.remove(veicoloTrovato);
            try (FileWriter writer = new FileWriter(PATH_VEICOLI_IN_TRANSITO)) {
                gson.toJson(veicoli, writer);
            } catch(Exception e) {
                System.err.println("Errore durante il salvataggio dopo l'estrazione: " + e.getMessage());
            }
        }
        return veicoloTrovato;
    }

    public static void saveVeicoloInTransito(VeicoloInTransito veicolo) {
        List<VeicoloInTransito> veicoli = getTuttiVeicoliInTransito();
        veicoli.add(veicolo);

        try (Writer writer = new FileWriter(PATH_VEICOLI_IN_TRANSITO, false)) {
            gson.toJson(veicoli, writer);
            System.out.println("Salvato veicolo " + veicolo.getTarga() + " in " + PATH_VEICOLI_IN_TRANSITO);
        } catch (IOException e) {
            LogUtils.logErr("Errore durante il salvataggio dei veicoli", e);
        }
    }

    public static void salvaVeicoli(List<Veicolo> veicoli){
        Gson gson = new GsonBuilder()
                // Questa configurazione risolve il problema di reflection con Java 9+
                .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
                .setPrettyPrinting()
                .create();

        try (Writer writer = new FileWriter(PATH_VEICOLI_REGISTRATI)) {
            gson.toJson(veicoli, writer);
            System.out.println("Veicoli registrati correttamente su " + PATH_VEICOLI_REGISTRATI);
        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio dei veicoli: " + e.getMessage());
        }
    }
}