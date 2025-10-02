package org.example.models;

import java.util.UUID;

public class Tratta {
    private String id;
    private String nomeCaselloIngresso;
    private String nomeCaselloUscita;
    private double distanza;
    private double pedaggio;//tariffa tratta
    private int utilizzo;

    public Tratta() {
    }

    public Tratta(String nomeCaselloIngresso, String nomeCaselloUscita, double distanza, double pedaggio, int utilizzo) {
        this.id = UUID.randomUUID().toString();
        this.nomeCaselloIngresso = nomeCaselloIngresso;
        this.nomeCaselloUscita = nomeCaselloUscita;
        this.distanza = distanza;
        this.pedaggio = pedaggio;
        this.utilizzo = utilizzo;
    }

    public String getId() {
        return id;
    }

    public int getUtilizzo() {
        return utilizzo;
    }

    public void setUtilizzo(int utilizzo) {
        this.utilizzo = utilizzo;
    }

    public String getNomeCaselloIngresso() {
        return nomeCaselloIngresso;
    }

    public void setNomeCaselloIngresso(String nomeCaselloIngresso) {
        this.nomeCaselloIngresso = nomeCaselloIngresso;
    }

    public String getNomeCaselloUscita() {
        return nomeCaselloUscita;
    }

    public void setNomeCaselloUscita(String nomeCaselloUscita) {
        this.nomeCaselloUscita = nomeCaselloUscita;
    }

    public double getDistanza() {
        return distanza;
    }

    public void setDistanza(double distanza) {
        this.distanza = distanza;
    }

    public double getPedaggio() {
        return pedaggio;
    }

    public void setPedaggio(double pedaggio) {
        this.pedaggio = pedaggio;
    }
}
