package org.example.dao;

import org.example.models.Casello;
import org.example.models.Tratta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TratteJsonDAOTest {
    private static File tempFile;

    @BeforeAll
    static void setupOnce() throws Exception {
        // Crea file temporaneo con lista vuota
        tempFile = File.createTempFile("tratte_test", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[]");
        }
    }

    @AfterAll
    static void tearDownOnce() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @BeforeEach
    void clearFile() throws Exception {
        // Ogni test parte con lista vuota
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[]");
        }
    }

    @Test
    void testAddAndRemoveTratta() {
        // Copia file temporaneo sopra quello vero
        try {
            Files.copy(tempFile.toPath(), new File(TratteJsonDAO.PATH_TRATTE).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Errore copia file di test: " + e.getMessage());
        }

        Tratta tratta = new Tratta("A1", "A14", 100.0, 35.0, 0);

        // Aggiungi tratta
        TratteJsonDAO.addTratta(tratta);
        List<Tratta> tratte = TratteJsonDAO.getAllTratte();
        assertEquals(1, tratte.size());
        assertEquals("A1", tratte.getFirst().getNomeCaselloIngresso());
        assertEquals("A14", tratte.getFirst().getNomeCaselloUscita());

        // Rimuovi tratta
        TratteJsonDAO.removeTrattaById(tratte.getFirst().getId());
        List<Tratta> tratteDopoRimozione = TratteJsonDAO.getAllTratte();
        assertTrue(tratteDopoRimozione.isEmpty());
    }

    @Test
    void testCalcolaDistanzaEPedaggio() {
        Casello in = new Casello("Casello1", "", 45.0, 9.0);
        Casello out = new Casello("Casello2", "", 46.0, 10.0);

        double distanza = TratteJsonDAO.calcolaDistanza(in, out);
        assertTrue(distanza > 0, "La distanza deve essere positiva");

        double pedaggio = TratteJsonDAO.calcolaPedaggio(distanza);
        assertEquals(distanza * 0.35, pedaggio, 0.0001);
    }
}