package org.example.dao;

import org.example.models.Veicolo;
import org.example.models.VeicoloInTransito;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VeicoliJsonDAOTest {

    private String originalPathRegistrati;
    private String originalPathInTransito;
    private final String testPathRegistrati = "test_veicoli_registrati.json";
    private final String testPathInTransito = "test_veicoli_in_transito.json";

    @BeforeEach
    void setUp() {
        originalPathRegistrati = VeicoliJsonDAO.PATH_VEICOLI_REGISTRATI;
        originalPathInTransito = VeicoliJsonDAO.PATH_VEICOLI_IN_TRANSITO;

        VeicoliJsonDAO.PATH_VEICOLI_REGISTRATI = testPathRegistrati;
        VeicoliJsonDAO.PATH_VEICOLI_IN_TRANSITO = testPathInTransito;

        new File(testPathRegistrati).delete();
        new File(testPathInTransito).delete();
    }

    @AfterEach
    void tearDown() {
        VeicoliJsonDAO.PATH_VEICOLI_REGISTRATI = originalPathRegistrati;
        VeicoliJsonDAO.PATH_VEICOLI_IN_TRANSITO = originalPathInTransito;

        new File(testPathRegistrati).delete();
        new File(testPathInTransito).delete();
    }


    private void setStaticFinalField(String fieldName, String newValue) throws Exception {
        Field field = VeicoliJsonDAO.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        // Rimuovo final
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        field.set(null, newValue);
    }

    @Test
    void testSalvaVeicoli() {
        Veicolo v1 = new Veicolo(false);
        Veicolo v2 = new Veicolo(true);

        VeicoliJsonDAO.salvaVeicoli(List.of(v1, v2));

        assertTrue(new File(testPathRegistrati).exists(),
                "Il file dei veicoli registrati dovrebbe esistere");
    }

    @Test
    void testSaveAndExtractVeicoloInTransito() {
        VeicoloInTransito v = new VeicoloInTransito(
                "AB123CD",
                "biglietto1",
                "telepass1",
                "A1",
                "2025-08-13T10:00:00",
                "entrata"
        );

        VeicoliJsonDAO.saveVeicoloInTransito(v);

        // Verifico che sia presente
        VeicoloInTransito trovato = VeicoliJsonDAO.getVeicoloInTransito("AB123CD");
        assertNotNull(trovato);
        assertEquals("AB123CD", trovato.getTarga());

        // Verifico la ricerca per biglietto
        VeicoloInTransito trovatoBiglietto = VeicoliJsonDAO.getVeicoloInTransitoByBiglietto("biglietto1");
        assertNotNull(trovatoBiglietto);
        assertEquals("AB123CD", trovatoBiglietto.getTarga());

        // Estraggo il veicolo
        VeicoloInTransito estratto = VeicoliJsonDAO.extractVeicoloInTransito("AB123CD");
        assertNotNull(estratto);
        assertEquals("AB123CD", estratto.getTarga());

        // Ora il file deve essere vuoto
        VeicoloInTransito nonTrovato = VeicoliJsonDAO.getVeicoloInTransito("AB123CD");
        assertNull(nonTrovato);
    }

    @Test
    void testTrovaTargaManualeDisponibile() {
        Veicolo v1 = new Veicolo(false);
        Veicolo v2 = new Veicolo(true);
        VeicoloInTransito vInTransito = new VeicoloInTransito(
                "T123AB",
                "biglietto2",
                "telepass2",
                "A1",
                "2025-08-13T11:00:00",
                "entrata"
        );

        VeicoliJsonDAO.salvaVeicoli(List.of(v1, v2));
        VeicoliJsonDAO.saveVeicoloInTransito(vInTransito);

        String targaDisponibile = VeicoliJsonDAO.trovaTargaManualeDisponibile();
        assertNotNull(targaDisponibile, "Deve esserci almeno una targa manuale disponibile");
    }
}
