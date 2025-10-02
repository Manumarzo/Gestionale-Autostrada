package org.example.models;

import org.example.microservices.Telecamera;
import org.example.server.ServerCentrale;

public class Casello implements Runnable{
    protected static final String BROKER_URL = ServerCentrale.BROKER_URL;
    protected static final String CLIENT_ID = "CaselloGenerico";
    private transient Telecamera telecamera;
    private String nome;
    private String regione;
    private double latitudine;
    private double longitudine;
    private transient String dispositivoId;

    public Casello(){};

    public Casello(String nome, String regione, double lat, double lon) {
        this.nome = nome;
        this.regione = regione;
        this.latitudine = lat;
        this.longitudine = lon;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRegione() {
        return regione;
    }

    public void setRegione(String regione) {
        this.regione = regione;
    }

    public double getLatitudine() {
        return latitudine;
    }

    public void setLatitudine(double latitudine) {
        this.latitudine = latitudine;
    }

    public double getLongitudine() {
        return longitudine;
    }

    public void setLongitudine(double longitudine) {
        this.longitudine = longitudine;
    }

    public Telecamera getTelecamera() {
        return telecamera;
    }

    public void setTelecamera(Telecamera telecamera) {
        this.telecamera = telecamera;
    }
    public String getDispositivoId() {
        return dispositivoId;
    }

    public void setDispositivoId(String dispositivoId) {
        this.dispositivoId = dispositivoId;
    }

    @Override
    public void run() {

    }
}
