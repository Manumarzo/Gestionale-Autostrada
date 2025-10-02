package org.example.models;

import java.time.LocalDate;
import java.util.UUID;

public class Multa {
    private final String id;
    private final String idViaggio;
    private final String motivazione;
    private final double importo;
    private final String scadenza;

    public Multa(String idViaggio, String motivazione, double importo) {
        this.id = UUID.randomUUID().toString();
        this.idViaggio = idViaggio;
        this.motivazione = motivazione;
        this.importo = importo;
        this.scadenza = LocalDate.now().plusWeeks(1).toString();
    }

    // Getters
    public String getId() { return id; }
    public String getIdViaggio() { return idViaggio; }
    public String getMotivazione() { return motivazione; }
    public double getImporto() { return importo; }
    public LocalDate getScadenza() { return LocalDate.parse(scadenza); }

    @Override
    public String toString() {
        return "Multa{" +
                "id='" + id + '\'' +
                ", idViaggio='" + idViaggio + '\'' +
                ", motivazione='" + motivazione + '\'' +
                ", importo=" + importo +
                ", scadenza=" + scadenza +
                '}';
    }
}
