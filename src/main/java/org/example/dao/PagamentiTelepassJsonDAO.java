package org.example.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.models.PagamentoTelepass;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PagamentiTelepassJsonDAO {
    public static String PATH_PAGAMENTI = "data" + File.separator + "pagamenti_telepass.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private PagamentiTelepassJsonDAO() {}

    public static void addPagamento(PagamentoTelepass pagamento) {
        List<PagamentoTelepass> pagamenti = getAllPagamenti();
        pagamenti.add(pagamento);
        saveAllPagamenti(pagamenti);
    }

    public static boolean riscuotiPagamento(String id) {
        List<PagamentoTelepass> pagamenti = getAllPagamenti();
        for (PagamentoTelepass p : pagamenti) {
            if (p.getId().equals(id)) {
                p.setPagato(true);
                saveAllPagamenti(pagamenti);
                return true;
            }
        }
        return false;
    }

    public static List<PagamentoTelepass> getAllPagamenti() {
        try (Reader reader = new FileReader(PATH_PAGAMENTI)) {
            Type listType = new TypeToken<List<PagamentoTelepass>>() {}.getType();
            List<PagamentoTelepass> pagamenti = gson.fromJson(reader, listType);
            return pagamenti != null ? pagamenti : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveAllPagamenti(List<PagamentoTelepass> pagamenti) {
        try (Writer writer = new FileWriter(PATH_PAGAMENTI)) {
            gson.toJson(pagamenti, writer);
        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio dei pagamenti Telepass: " + e.getMessage());
        }
    }
}
