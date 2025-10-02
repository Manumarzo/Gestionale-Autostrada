package org.example.dao;

import org.example.models.Viaggio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ViaggiJsonDAOTest {

    private static String pathOriginale;
    private static File fileTest;

    @BeforeAll
    static void setUpClass() throws Exception {
        pathOriginale = ViaggiJsonDAO.PATH_VIAGGI;

        // Crea file temporaneo per i test
        fileTest = File.createTempFile("viaggi_test", ".json");
        fileTest.deleteOnExit(); // Verrà eliminato alla fine della JVM

        Files.writeString(fileTest.toPath(), "[]");
        ViaggiJsonDAO.PATH_VIAGGI = fileTest.getAbsolutePath();
    }

    @AfterAll
    static void tearDownClass() {
        // Ripristina il path originale
        ViaggiJsonDAO.PATH_VIAGGI = pathOriginale;
    }

    private Viaggio creaViaggioDiTest(String tipo, String targa) {
        return new Viaggio(
                tipo,
                targa,
                "B1",
                "T1",
                "MI-NORD",
                "MI-SUD",
                "2025-08-13T09:00:00",
                "2025-08-13T11:00:00",
                15.5,
                150.0,
                75.0,
                false
        );
    }

    @Test
    void testAggiungiViaggio() {
        Viaggio v1 = creaViaggioDiTest("Auto", "AB123CD");
        String idSalvato = ViaggiJsonDAO.aggiungiViaggio(v1);

        assertEquals(v1.getId(), idSalvato);

        List<Viaggio> viaggi = ViaggiJsonDAO.leggiViaggi();
        assertTrue(viaggi.stream().anyMatch(v -> v.getId().equals(v1.getId())));
    }

    @Test
    void testFindViaggio() {
        Viaggio v1 = creaViaggioDiTest("Moto", "XY987ZT");
        ViaggiJsonDAO.aggiungiViaggio(v1);

        Viaggio trovato = ViaggiJsonDAO.findViaggio(v1.getId());
        assertNotNull(trovato);
        assertEquals("MI-NORD", trovato.getCaselloIngresso());

        Viaggio nonEsistente = ViaggiJsonDAO.findViaggio("NON_EXIST");
        assertNull(nonEsistente);
    }

    @Test
    void testLeggiViaggi() {
        Viaggio v1 = creaViaggioDiTest("Auto", "TO123AB");
        Viaggio v2 = creaViaggioDiTest("Camion", "BO456CD");

        ViaggiJsonDAO.aggiungiViaggio(v1);
        ViaggiJsonDAO.aggiungiViaggio(v2);

        List<Viaggio> viaggi = ViaggiJsonDAO.leggiViaggi();
        assertTrue(viaggi.stream().anyMatch(v -> v.getId().equals(v1.getId())));
        assertTrue(viaggi.stream().anyMatch(v -> v.getId().equals(v2.getId())));
    }
}
