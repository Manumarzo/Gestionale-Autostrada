package org.example.models;

import java.util.UUID;

public class PagamentoTelepass {
    private final String id;
    private final String telepassId;
    private final String viaggioId;
    private final double importo;
    private boolean pagato;

    public PagamentoTelepass(String telepassId, double importo, String viaggioId){
        this.id = UUID.randomUUID().toString();
        this.telepassId = telepassId;
        this.viaggioId = viaggioId;
        this.importo = importo;
        this.pagato = false;
    }
    
    public String getId() {
        return id;
    }

    public String getTelepassId() {
        return telepassId;
    }

    public double getImporto() {
        return importo;
    }

    public boolean isPagato() {
        return pagato;
    }

    public void setPagato(boolean pagato) {
        this.pagato = pagato;
    }

    public String getViaggioId() {
        return viaggioId;
    }
}
