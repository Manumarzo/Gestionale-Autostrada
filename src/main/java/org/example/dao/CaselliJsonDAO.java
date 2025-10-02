package org.example.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.*;
import org.example.microservices.CaselloIngressoAutomatico;
import org.example.microservices.CaselloIngressoManuale;
import org.example.microservices.CaselloUscitaAutomatico;
import org.example.microservices.CaselloUscitaManuale;
import org.example.models.Casello;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CaselliJsonDAO {
    private static final Gson gson = new Gson();
    private static final Random random = new Random();
    public static String PATH_CASELLI_IN_USO = "data" + File.separator + "caselli_in_uso.json";
    public static String path = "src" + File.separator + "main" + File.separator + "java" + File.separator + "org" + File.separator + "example" + File.separator + "resources" + File.separator + "caselli.json";

    // Costruttore privato per impedire l'istanziazione della classe di utilità
    private CaselliJsonDAO() {}


    public static List<Casello> getCaselli() {
        List<Casello> listaCaselli = new ArrayList<>();

        try {
            File caselliFile = new File(path);
            if (!caselliFile.exists()) {
                System.err.println("Errore: Il file caselli.json non è stato trovato al percorso: " + caselliFile.getAbsolutePath());
                return listaCaselli;
            }

            JsonArray jsonList = gson.fromJson(new FileReader(caselliFile), JsonObject.class).getAsJsonArray("caselli");
            for (JsonElement element : jsonList) {
                JsonObject obj = element.getAsJsonObject();
                String nome = obj.get("nome").getAsString();
                String regione = obj.get("regione").getAsString();
                double lat = obj.get("latitudine").getAsDouble();
                double lon = obj.get("longitudine").getAsDouble();

                Casello casello = new Casello(nome, regione, lat, lon);
                listaCaselli.add(casello);
            }

        } catch (Exception e) {
            System.err.println("Errore durante la lettura o il parsing del file caselli.json: " + e.getMessage());
            e.printStackTrace();
        }

        return listaCaselli;
    }

    public static void addCasello(Casello casello) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        byte[] jsonData = Files.readAllBytes(Paths.get(path));
        ObjectNode rootNode = (ObjectNode) mapper.readTree(jsonData);

        JsonNode caselliNode = rootNode.get("caselli");
        List<Casello> caselliList;

        if (caselliNode != null && caselliNode.isArray()) {
            caselliList = mapper.convertValue(caselliNode, new TypeReference<List<Casello>>() {});
        } else {
            caselliList = new ArrayList<>();
        }

        caselliList.add(casello);
        TratteJsonDAO.creaTutteLeTratteDaCaselli(caselliList);

        rootNode.set("caselli", mapper.valueToTree(caselliList));

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), rootNode);
    }

    public static boolean removeCasello(String nome) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(path);

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(path));
            ObjectNode rootNode = (ObjectNode) mapper.readTree(jsonData);

            JsonNode caselliNode = rootNode.get("caselli");
            if (caselliNode == null || !caselliNode.isArray()) {
                System.err.println("Il nodo 'caselli' non è valido.");
                return false;
            }

            List<Casello> caselli = mapper.convertValue(caselliNode, new TypeReference<List<Casello>>() {});
            boolean rimosso = caselli.removeIf(c -> c.getNome().equalsIgnoreCase(nome));

            if (rimosso) {
                rootNode.set("caselli", mapper.valueToTree(caselli));
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
                System.out.println("Casello rimosso da caselli.json: " + nome);

                // Rimuove anche da caselli_in_uso.json
                File inUsoFile = new File(PATH_CASELLI_IN_USO);
                if (inUsoFile.exists()) {
                    JsonArray array = JsonParser.parseReader(new FileReader(inUsoFile)).getAsJsonArray();
                    JsonArray aggiornato = new JsonArray();
                    for (JsonElement elem : array) {
                        JsonObject obj = elem.getAsJsonObject();
                        String nomeCasello = obj.get("nome").getAsString();
                        if (!nomeCasello.equalsIgnoreCase(nome)) {
                            aggiornato.add(obj); // Mantieni solo quelli con nome diverso
                        }
                    }

                    // Riscrivi il file con i rimanenti
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    try (FileWriter writer = new FileWriter(inUsoFile)) {
                        gson.toJson(aggiornato, writer);
                    }

                    System.out.println("Casello rimosso anche da caselli_in_uso.json: " + nome);
                }

                //rimuovo le tratte con quel casello
                TratteJsonDAO.removeTratteByCaselloNome(nome);

                return true;
            } else {
                System.out.println("Nessun casello trovato con il nome: " + nome);
                return false;
            }

        } catch (IOException e) {
            System.err.println("Errore durante la rimozione del casello: " + e.getMessage());
            return false;
        }
    }


    public static Casello pickRandomCasello() {
        List<Casello> caselli = getCaselli();
        if (caselli.isEmpty()) {
            return null;
        }
        return caselli.get(random.nextInt(caselli.size()));
    }


    public static Casello getCaselloByName(String nomeCasello) {
        if (nomeCasello == null || nomeCasello.trim().isEmpty()) {
            return null;
        }

        return getCaselli().stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nomeCasello))
                .findFirst()
                .orElse(null);
    }


    public static void addCaselliInUso(List<Casello> caselli) {
        List<Map<String, String>> caselliDaSalvare = new ArrayList<>();

        for (Casello c : caselli) {
            Map<String, String> info = getStringStringMap(c);
            caselliDaSalvare.add(info);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(PATH_CASELLI_IN_USO)) {
            gson.toJson(caselliDaSalvare, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio dei caselli: " + e.getMessage());
        }
    }

    private static Map<String, String> getStringStringMap(Casello c) {
        Map<String, String> info = new HashMap<>();
        info.put("nome", c.getNome());
        info.put("id", c.getDispositivoId());
        if (c instanceof CaselloIngressoAutomatico || c instanceof CaselloIngressoManuale) {
            info.put("tipo_casello", "entrata");
        } else if (c instanceof CaselloUscitaAutomatico || c instanceof CaselloUscitaManuale) {
            info.put("tipo_casello", "uscita");
        }

        if (c instanceof CaselloIngressoAutomatico || c instanceof CaselloUscitaAutomatico) {
            info.put("tipo_ingresso", "automatico");
        } else if (c instanceof CaselloIngressoManuale || c instanceof CaselloUscitaManuale) {
            info.put("tipo_ingresso", "manuale");
        }
        return info;
    }

    public static String getIdCaselloInUso(String nomeCasello, boolean isIngresso, boolean isAutomatico) {
        String tipoCasello = isIngresso ? "entrata" : "uscita";
        String tipoIngresso = isAutomatico ? "automatico" : "manuale";

        try (FileReader reader = new FileReader("data" + File.separator + "caselli_in_uso.json")) {
            JsonArray caselli = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement elem : caselli) {
                JsonObject obj = elem.getAsJsonObject();
                String nome = obj.get("nome").getAsString();
                String tipoC = obj.get("tipo_casello").getAsString();
                String tipoI = obj.get("tipo_ingresso").getAsString();

                if (nome.equalsIgnoreCase(nomeCasello) &&
                        tipoC.equalsIgnoreCase(tipoCasello) &&
                        tipoI.equalsIgnoreCase(tipoIngresso)) {
                    return obj.get("id").getAsString();
                }
            }

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file JSON: " + e.getMessage());
        }

        return null; // Nessun casello trovato
    }

    public static String getNomeCaselloInUsoById(String id) {
        try (FileReader reader = new FileReader(PATH_CASELLI_IN_USO)) {
            JsonArray caselli = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement elem : caselli) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.get("id").getAsString().equals(id)) {
                    return obj.get("nome").getAsString();
                }
            }

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file caselli_in_uso.json: " + e.getMessage());
        }

        return null; // ID non trovato
    }


    public static boolean isCaselliInUsoEmpty() {
        File file = new File(PATH_CASELLI_IN_USO);
        return !file.exists() || file.length() == 0;
    }

    public static boolean updateCasello(String nomeOriginale, Casello caselloModificato) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(path);

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(path));
            ObjectNode rootNode = (ObjectNode) mapper.readTree(jsonData);

            JsonNode caselliNode = rootNode.get("caselli");
            if (caselliNode == null || !caselliNode.isArray()) {
                System.err.println("Il nodo 'caselli' non è valido.");
                return false;
            }

            List<Casello> caselli = mapper.convertValue(caselliNode, new TypeReference<List<Casello>>() {});
            boolean updated = false;

            for (int i = 0; i < caselli.size(); i++) {
                if (caselli.get(i).getNome().equalsIgnoreCase(nomeOriginale)) {
                    // Sostituisce il vecchio oggetto casello con quello modificato
                    caselli.set(i, caselloModificato);
                    updated = true;
                    break; // Trovato e aggiornato, esce dal ciclo
                }
            }

            if (updated) {
                // Salva la lista aggiornata nel file JSON
                rootNode.set("caselli", mapper.valueToTree(caselli));
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
                System.out.println("Casello modificato con successo nel file JSON: " + nomeOriginale);

                return true;
            } else {
                System.out.println("Nessun casello trovato con il nome: " + nomeOriginale);
                return false;
            }

        } catch (IOException e) {
            System.err.println("Errore durante l'aggiornamento del casello: " + e.getMessage());
            return false;
        }
    }
}