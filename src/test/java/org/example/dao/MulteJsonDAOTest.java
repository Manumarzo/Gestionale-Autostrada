package org.example.dao;

import org.example.models.Multa;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MulteJsonDAOTest {

    private static final String TEST_PATH = "data" + File.separator + "multe_test.json";
    private final List<String> multeAggiunte = new ArrayList<>();
    private String originalPath;

    @BeforeEach
    void setUp() {
        // Salva il path originale e sostituiscilo con quello di test
        originalPath = MulteJsonDAO.PATH_MULTE;
        setPath(TEST_PATH);

        // Se il file non esiste, crealo vuoto
        if (!new File(TEST_PATH).exists()) {
            MulteJsonDAO.saveAllMulte(new ArrayList<>());
        }
    }

    @AfterEach
    void tearDown() {
        // Rimuove solo le multe aggiunte dai test
        List<Multa> tutte = MulteJsonDAO.getAllMulte();
        tutte.removeIf(m -> multeAggiunte.contains(m.getId()));
        MulteJsonDAO.saveAllMulte(tutte);
        multeAggiunte.clear();

        // Ripristina il path originale
        setPath(originalPath);
        File fileTest = new File(TEST_PATH);
        if (fileTest.exists()) {
            fileTest.delete();
        }
    }

    private void setPath(String path) {
        try {
            var field = MulteJsonDAO.class.getDeclaredField("PATH_MULTE");
            field.setAccessible(true);
            field.set(null, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Multa creaMultaDiTest(String idViaggio) {
        return new Multa(
                idViaggio,
                "Eccesso di velocità",
                150.0
        );
    }

    @Test
    void testAddAndGetMulta() {
        Multa multa = creaMultaDiTest("V1");
        MulteJsonDAO.addMulta(multa);
        multeAggiunte.add(multa.getId());

        Multa trovata = MulteJsonDAO.getMultaById(multa.getId());
        assertNotNull(trovata);
        assertEquals("Eccesso di velocità", trovata.getMotivazione());
    }

    @Test
    void testGetAllMulte() {
        Multa m1 = creaMultaDiTest("V2");
        Multa m2 = creaMultaDiTest("V3");

        MulteJsonDAO.addMulta(m1);
        MulteJsonDAO.addMulta(m2);
        multeAggiunte.add(m1.getId());
        multeAggiunte.add(m2.getId());

        List<Multa> tutte = MulteJsonDAO.getAllMulte();
        assertTrue(tutte.stream().anyMatch(m -> m.getId().equals(m1.getId())));
        assertTrue(tutte.stream().anyMatch(m -> m.getId().equals(m2.getId())));
    }

    @Test
    void testRemoveMultaById() {
        Multa m1 = creaMultaDiTest("V4");
        MulteJsonDAO.addMulta(m1);
        multeAggiunte.add(m1.getId());

        MulteJsonDAO.removeMultaById(m1.getId());

        assertNull(MulteJsonDAO.getMultaById(m1.getId()));
        multeAggiunte.remove(m1.getId()); // non serve più rimuoverla in tearDown
    }

    @Test
    void testGetMulteByViaggioId() {
        String idViaggio = "V5";
        Multa m1 = creaMultaDiTest(idViaggio);
        Multa m2 = creaMultaDiTest(idViaggio);
        Multa m3 = creaMultaDiTest("ALTRO");

        MulteJsonDAO.addMulta(m1);
        MulteJsonDAO.addMulta(m2);
        MulteJsonDAO.addMulta(m3);
        multeAggiunte.add(m1.getId());
        multeAggiunte.add(m2.getId());
        multeAggiunte.add(m3.getId());

        List<Multa> trovate = MulteJsonDAO.getMulteByViaggioId(idViaggio);
        assertEquals(2, trovate.size());
        assertTrue(trovate.stream().allMatch(m -> m.getIdViaggio().equals(idViaggio)));
    }
}
