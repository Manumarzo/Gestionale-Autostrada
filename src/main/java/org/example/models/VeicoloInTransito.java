package org.example.models;

public class VeicoloInTransito {

    private String targa;
    private String bigliettoId;
    private String telepassId;
    private String caselloIngresso;
    private String timestampIngresso;
    private String tipoIngresso;

    public VeicoloInTransito() {
    }

    public VeicoloInTransito(String targa, String bigliettoId, String telepassId, String caselloIngresso, String timestampIngresso, String tipoIngresso) {
        this.targa = targa;
        this.bigliettoId = bigliettoId;
        this.telepassId = telepassId;
        this.caselloIngresso = caselloIngresso;
        this.timestampIngresso = timestampIngresso;
        this.tipoIngresso = tipoIngresso;
    }

    public String getTarga() {
        return targa;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public String getBigliettoId() {
        return bigliettoId;
    }

    public void setBigliettoId(String bigliettoId) {
        this.bigliettoId = bigliettoId;
    }

    public String getTelepassId() {
        return telepassId;
    }

    public void setTelepassId(String telepassId) {
        this.telepassId = telepassId;
    }

    public String getCaselloIngresso() {
        return caselloIngresso;
    }

    public void setCaselloIngresso(String caselloIngresso) {
        this.caselloIngresso = caselloIngresso;
    }

    public String getTimestampIngresso() {
        return timestampIngresso;
    }

    public void setTimestampIngresso(String timestampIngresso) {
        this.timestampIngresso = timestampIngresso;
    }

    public String getTipoIngresso() {
        return tipoIngresso;
    }

    public void setTipoIngresso(String tipoIngresso) {
        this.tipoIngresso = tipoIngresso;
    }
}
