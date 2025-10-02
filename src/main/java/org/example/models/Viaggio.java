package org.example.models;

import java.util.UUID;

public class Viaggio {
    private final String id;
    private String tipo;
    private String targa;
    private String bigliettoId;
    private String telepassId;
    private String caselloIngresso;
    private String caselloUscita;
    private String timestampIngresso;
    private String timestampUscita;
    private double pedaggio;
    private double distanza;
    private double velocita_media;
    private boolean multa;

    public Viaggio(String tipo, String targa, String bigliettoId, String telepassId, String caselloIngresso, String caselloUscita, String timestampIngresso, String timestampUscita, double pedaggio, double distanza, double velocita_media, boolean multa) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.targa = targa;
        this.bigliettoId = bigliettoId;
        this.telepassId = telepassId;
        this.caselloIngresso = caselloIngresso;
        this.caselloUscita = caselloUscita;
        this.timestampIngresso = timestampIngresso;
        this.timestampUscita = timestampUscita;
        this.pedaggio = pedaggio;
        this.distanza = distanza;
        this.velocita_media = velocita_media;
        this.multa = multa;
    }

    public String getId(){
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo){
        this.tipo = tipo;
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

    public String getCaselloUscita() {
        return caselloUscita;
    }

    public void setCaselloUscita(String caselloUscita) {
        this.caselloUscita = caselloUscita;
    }

    public String getTimestampIngresso() {
        return timestampIngresso;
    }

    public void setTimestampIngresso(String timestampIngresso) {
        this.timestampIngresso = timestampIngresso;
    }

    public String getTimestampUscita() {
        return timestampUscita;
    }

    public void setTimestampUscita(String timestampUscita) {
        this.timestampUscita = timestampUscita;
    }

    public double getPedaggio() {
        return pedaggio;
    }

    public void setPedaggio(double pedaggio) {
        this.pedaggio = pedaggio;
    }

    public double getDistanza(){
        return distanza;
    }

    public void setDistanza(double distanza){
        this.distanza = distanza;
    }

    public double getVelocitaMedia(){
        return velocita_media;
    }

    public void setVelocitaMedia(double velocita_media){
        this.velocita_media = velocita_media;
    }

    public boolean getMulta(){
        return multa;
    }

    public void setMulta(boolean multa){
        this.multa = multa;
    }
}
