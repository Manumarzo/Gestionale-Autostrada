package org.example.dao;

import org.example.models.Casello;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaselliJsonDAOTest {

    @Test
    void testGetCaselliNonNull() {
        List<Casello> caselli = CaselliJsonDAO.getCaselli();
        assertNotNull(caselli, "La lista di caselli non deve essere null");
    }

    @Test
    void testGetCaselloByNome() {
        Casello first = CaselliJsonDAO.getCaselli().getFirst();
        Casello found = CaselliJsonDAO.getCaselloByName(first.getNome());
        assertNotNull(found, "Il casello trovato non deve essere null");
        assertEquals(first.getNome(), found.getNome(), "I nomi devono corrispondere");
    }

    @Test
    void testPickRandomCasello() {
        Casello random = CaselliJsonDAO.pickRandomCasello();
        assertNotNull(random, "Il casello random non deve essere null");
    }
}
