package org.example.server;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.dao.*;
import org.example.keycloak.Auth;
import org.example.microservices.CaselloIngressoAutomatico;
import org.example.microservices.CaselloIngressoManuale;
import org.example.microservices.CaselloUscitaAutomatico;
import org.example.microservices.CaselloUscitaManuale;
import org.example.models.*;
import org.example.utils.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.example.dao.ViaggiJsonDAO.leggiViaggi;

public class ServerApp {
    public static final int PORT = 3000;
    public static void start() {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                });
            });
        });

        app.before(ctx -> {

            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");

            System.out.println("Metodo: " + ctx.method() + " | Path: " + ctx.path());
            if ("OPTIONS".equalsIgnoreCase(ctx.method().toString())) {
                return;
            }

                String authHeader = ctx.header("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new UnauthorizedResponse("Token mancante");
                }

                String token = authHeader.substring(7);
                //System.out.println("TOKEN: " + token);
                try {
                    String[] parts = token.split("\\.");
                    if (parts.length != 3) throw new UnauthorizedResponse("Token malformato");

                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                    //System.out.println(payloadJson);
                    Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, new TypeReference<>() {});
                    //System.out.println(payload);
                    ctx.attribute("access_token", payload);
                } catch (Exception e) {
                    throw new UnauthorizedResponse("Token non valido");
                }
            //}
        });

        app.get("/caselli", ctx -> {
            Auth.requireAnyRole(ctx, "amministratore", "impiegato");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");
            List<Casello> caselli = CaselliJsonDAO.getCaselli();
            ctx.json(caselli);
        });

        app.post("/caselli", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");
            
            String body = ctx.body();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(body);
            String nome = json.get("nome").asText().toUpperCase();
            String regione = json.get("regione").asText();
            double latitudine = Double.parseDouble(json.get("latitudine").asText());
            double longitudine = Double.parseDouble(json.get("longitudine").asText());
            Casello casello = new Casello(nome, regione, latitudine, longitudine);
            addCasello(casello);

            ctx.status(201).result("Casello aggiunto con successo");
        });

        app.put("/caselli/{nomeCasello}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            String nomeOriginale = ctx.pathParam("nomeCasello");

            try {
                // Deserializza il corpo della richiesta in un oggetto Casello
                Casello caselloModificato = ctx.bodyAsClass(Casello.class);

                // Chiama il DAO per eseguire l'aggiornamento
                boolean successo = CaselliJsonDAO.updateCasello(nomeOriginale, caselloModificato);

                if (successo) {
                    ctx.status(200).result("Casello modificato con successo");
                } else {
                    ctx.status(404).result("Casello non trovato da modificare");
                }
            } catch (Exception e) {
                // Gestisce errori di deserializzazione JSON
                ctx.status(400).result("Dati inviati non validi: " + e.getMessage());
            }
        });

        app.delete("/caselli/{nomeCasello}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            
            String nome = ctx.pathParam("nomeCasello");
            boolean rimosso = CaselliJsonDAO.removeCasello(nome);
            
            if (rimosso) {
                ctx.status(200).result("Casello eliminato con successo");
            } else {
                ctx.status(404).result("Casello non trovato");
            }
        });

        app.get("/tratte", ctx -> {
            Auth.requireAnyRole(ctx, "amministratore", "impiegato");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");
            List<Tratta> tratte = TratteJsonDAO.getAllTratte();
            ctx.json(tratte);
        });

        //non usata
        app.get("/tratte/{trattaId}", ctx -> {
            Auth.requireAnyRole(ctx, "amministratore", "impiegato");
            Tratta tratta = TratteJsonDAO.getTrattaById(ctx.pathParam("trattaId"));
            if (tratta != null) {
                ctx.json(tratta);
            } else {
                ctx.status(404).result("Tratta non trovata");
            }
        });

        app.put("/tratte/{trattaId}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            String trattaId = ctx.pathParam("trattaId");
            Tratta trattaOriginale = TratteJsonDAO.getTrattaById(trattaId);

            if (trattaOriginale == null) {
                ctx.status(404).result("Tratta non trovata");
                return;
            }

            try {
                // Leggiamo il corpo JSON per ottenere il nuovo pedaggio
                JsonNode json = new ObjectMapper().readTree(ctx.body());
                if (json.has("pedaggio")) {
                    double nuovoPedaggio = json.get("pedaggio").asDouble();
                    trattaOriginale.setPedaggio(nuovoPedaggio); // Aggiorniamo solo il campo del pedaggio

                    TratteJsonDAO.updateTratta(trattaOriginale); // Salviamo l'intero oggetto aggiornato
                    ctx.status(200).result("Pedaggio aggiornato con successo");
                } else {
                    ctx.status(400).result("Campo 'pedaggio' mancante nel corpo della richiesta");
                }
            } catch (Exception e) {
                ctx.status(400).result("Richiesta non valida: " + e.getMessage());
            }
        });

        app.get("/guadagni", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            List<Viaggio> viaggi = leggiViaggi();
            double guadagnoTotale = viaggi.stream()
                    .mapToDouble(Viaggio::getPedaggio)
                    .sum();
            // Restituisce un oggetto JSON
            ctx.json(Map.of("guadagnoTotale", guadagnoTotale));
        });

        // API per il guadagno su tratta specifica
        app.get("/guadagni/{nomeCaselloIngresso}/{nomeCaselloUscita}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            String caselloIngresso = ctx.pathParam("nomeCaselloIngresso");
            String caselloUscita = ctx.pathParam("nomeCaselloUscita");

            List<Viaggio> viaggi = leggiViaggi();
            double guadagnoTratta = viaggi.stream()
                    .filter(v -> caselloIngresso.equals(v.getCaselloIngresso()) &&
                            caselloUscita.equals(v.getCaselloUscita()))
                    .mapToDouble(Viaggio::getPedaggio)
                    .sum();

            ctx.json(Map.of("guadagnoTratta", guadagnoTratta));
        });

        app.get("/telepass", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");

            List<PagamentoTelepass> pagamenti = PagamentiTelepassJsonDAO.getAllPagamenti();

            List<Map<String, Object>> pagamentiDettagliati = new ArrayList<>();

            for (PagamentoTelepass pagamento : pagamenti) {
                Viaggio viaggio = ViaggiJsonDAO.findViaggio(pagamento.getViaggioId());

                Map<String, Object> pagamentoJson = new HashMap<>();
                pagamentoJson.put("id", pagamento.getId());
                pagamentoJson.put("importo", pagamento.getImporto());
                pagamentoJson.put("pagato", pagamento.isPagato());

                if (viaggio != null) {
                    pagamentoJson.put("targa", viaggio.getTarga());
                    pagamentoJson.put("tratta", viaggio.getCaselloIngresso() + "->" + viaggio.getCaselloUscita());
                } else {
                    LogUtils.logErr("Errore durante il recupero del viaggio", null);
                    ctx.status(404).result("Viaggio non trovato per il pagamento con id " + pagamento.getId());
                    return;
                }
                pagamentiDettagliati.add(pagamentoJson);
            }

            ctx.json(pagamentiDettagliati);
        });

        app.put("/telepass/{telepassId}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");

            String id = ctx.pathParam("telepassId");

            boolean success = PagamentiTelepassJsonDAO.riscuotiPagamento(id);

            if (success) {
                ctx.status(200).result("Pagamento riscosso con successo");
            } else {
                ctx.status(404).result("Pagamento non trovato");
            }
        });

        app.put("/telepass", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");

            List<String> ids = ctx.bodyAsClass(List.class); // Assumendo che arrivi un JSON array di stringhe
            List<String> riscossi = new ArrayList<>();

            for (String id : ids) {
                if (PagamentiTelepassJsonDAO.riscuotiPagamento(id)) {
                    riscossi.add(id);
                }
            }

            if (riscossi.isEmpty()) {
                ctx.status(404).result("Nessun pagamento trovato");
            } else {
                ctx.status(200).json(Map.of("riscossi", riscossi));
            }
        });

        app.get("/multe", ctx -> {
        Auth.requireAnyRole(ctx, "amministratore", "impiegato");
        ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");

        List<Multa> multe = MulteJsonDAO.getAllMulte();

        List<Map<String, Object>> multeConDettagli = new ArrayList<>();

        for (Multa multa : multe) {
            Viaggio viaggio = ViaggiJsonDAO.findViaggio(multa.getIdViaggio());

            Map<String, Object> multaJson = new HashMap<>();
            multaJson.put("id", multa.getId());
            multaJson.put("motivazione", multa.getMotivazione());
            multaJson.put("importo", multa.getImporto());
            multaJson.put("scadenza", multa.getScadenza());
            if (viaggio != null) {
                multaJson.put("targa", viaggio.getTarga());
                multaJson.put("nome", viaggio.getCaselloIngresso() + "->" + viaggio.getCaselloUscita());
            } else {
                ctx.status(404).result("Viaggio non trovato per la multa con id " + multa.getId());
                return;
            }

            multeConDettagli.add(multaJson);
        }

        ctx.json(multeConDettagli);
    });

        app.get("/multe/{targa}", ctx -> {
            Auth.requireRole(ctx, "amministratore");
            ctx.result("Mostra multe per targa specifica");
        });

        app.get("/ruoli", ctx -> {
            ctx.header("Access-Control-Allow-Origin", "http://localhost:8081");

            List<String> ruoli = Auth.extractRoles(ctx);
            if (ruoli == null) {
                ctx.status(401).result("Ruoli non trovati o token non valido");
                return;
            }

            ctx.json(ruoli);
        });

        app.start(PORT);
    }

    private static void addCasello(Casello casello){
        try{
            CaselliJsonDAO.addCasello(casello);
            Casello caselloTemp;
            List<Casello> listaCaselli = new ArrayList<>();
            caselloTemp = new CaselloIngressoManuale(casello);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloIngressoAutomatico(casello);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloUscitaManuale(casello);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            caselloTemp = new CaselloUscitaAutomatico(casello);
            listaCaselli.add(caselloTemp);
            new Thread(caselloTemp).start();
            CaselliJsonDAO.addCaselliInUso(listaCaselli);
        } catch (IOException e){
            LogUtils.logErr("Errore durante l'aggiunta del casello ", e);
        }
    }
}
