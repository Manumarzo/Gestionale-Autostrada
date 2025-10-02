package org.example.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Biglietto {
    private final UUID id;
    private final LocalDateTime ts;
    private final String idIngresso;

    public Biglietto(String idIngresso) {
        this.id = UUID.randomUUID();
        this.ts = LocalDateTime.now();
        this.idIngresso = idIngresso;
    }

    public Biglietto(UUID id, LocalDateTime ts, String idIngresso){
        this.id = id;
        this.ts = ts;
        this.idIngresso = idIngresso;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getTs() {
        return ts;
    }

    public String getIdIngresso() {
        return idIngresso;
    }
}
