package org.example.dao;

import org.example.models.PagamentoTelepass;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagamentiTelepassJsonDAOTest {

    private static final String TEST_PATH = "data" + File.separator + "pagamenti_telepass_test.json";
    private final List<String> pagamentiAggiunti = new ArrayList<>();
    private String originalPath;

    @BeforeEach
    void setUp() {
        // Salviamo il path originale e lo sostituiamo con quello di test
        originalPath = getPath();
        setPath(TEST_PATH);
        // Creiamo file vuoto se non esiste
        clearFile();
    }

    @AfterEach
    void tearDown() {
        // Rimuoviamo solo i pagamenti aggiunti
        List<PagamentoTelepass> tutti = PagamentiTelepassJsonDAO.getAllPagamenti();
        tutti.removeIf(p -> pagamentiAggiunti.contains(p.getId()));
        saveAll(tutti);
        pagamentiAggiunti.clear();

        // Ripristiniamo il path originale
        setPath(originalPath);
        File fileTest = new File(TEST_PATH);
        if (fileTest.exists()) {
            fileTest.delete();
        }
    }

    private String getPath() {
        try {
            var field = PagamentiTelepassJsonDAO.class.getDeclaredField("PATH_PAGAMENTI");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPath(String path) {
        try {
            var field = PagamentiTelepassJsonDAO.class.getDeclaredField("PATH_PAGAMENTI");
            field.setAccessible(true);
            field.set(null, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void clearFile() {
        saveAll(new ArrayList<>());
    }

    private void saveAll(List<PagamentoTelepass> pagamenti) {
        try {
            var method = PagamentiTelepassJsonDAO.class.getDeclaredMethod("saveAllPagamenti", List.class);
            method.setAccessible(true);
            method.invoke(null, pagamenti);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PagamentoTelepass creaPagamentoDiTest(String telepassId, String viaggioId) {
        return new PagamentoTelepass(
                telepassId,
                100.0,
                viaggioId
        );
    }

    @Test
    void testAddAndGetPagamenti() {
        PagamentoTelepass pagamento = creaPagamentoDiTest("T1", "V1");
        PagamentiTelepassJsonDAO.addPagamento(pagamento);
        pagamentiAggiunti.add(pagamento.getId());

        List<PagamentoTelepass> trovati = PagamentiTelepassJsonDAO.getAllPagamenti();
        assertTrue(trovati.stream().anyMatch(p -> p.getId().equals(pagamento.getId())));
    }

    @Test
    void testRiscuotiPagamento() {
        PagamentoTelepass pagamento = creaPagamentoDiTest("T2", "V2");
        PagamentiTelepassJsonDAO.addPagamento(pagamento);
        pagamentiAggiunti.add(pagamento.getId());

        boolean risultato = PagamentiTelepassJsonDAO.riscuotiPagamento(pagamento.getId());
        assertTrue(risultato);

        PagamentoTelepass aggiornato = PagamentiTelepassJsonDAO.getAllPagamenti()
                .stream().filter(p -> p.getId().equals(pagamento.getId()))
                .findFirst().orElse(null);

        assertNotNull(aggiornato);
        assertTrue(aggiornato.isPagato());
    }

    @Test
    void testGetAllPagamenti() {
        PagamentoTelepass p1 = creaPagamentoDiTest("T3", "V3");
        PagamentoTelepass p2 = creaPagamentoDiTest("T4", "V4");

        PagamentiTelepassJsonDAO.addPagamento(p1);
        PagamentiTelepassJsonDAO.addPagamento(p2);
        pagamentiAggiunti.add(p1.getId());
        pagamentiAggiunti.add(p2.getId());

        List<PagamentoTelepass> tutti = PagamentiTelepassJsonDAO.getAllPagamenti();
        assertTrue(tutti.stream().anyMatch(p -> p.getId().equals(p1.getId())));
        assertTrue(tutti.stream().anyMatch(p -> p.getId().equals(p2.getId())));
    }

    @Test
    void testRiscuotiPagamentoInesistente() {
        boolean risultato = PagamentiTelepassJsonDAO.riscuotiPagamento("ID_NON_ESISTE");
        assertFalse(risultato);
    }
}
