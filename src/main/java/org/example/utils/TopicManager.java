package org.example.utils;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class TopicManager {

    //NUOVI TOPIC
    public static String getTicketRequestTopic(String idCasello){
        return "veicolo/" + idCasello + "/ingresso/manuale/richiesta_biglietto";
    }

    public static String getManInPlateRequestTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/ingresso/manuale/richiesta_targa";
    }

    public static String getManInPlateResponseTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/ingresso/manuale/lettura_targa";
    }
    public static String getManOutPlateRequestTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/uscita/manuale/richiesta_targa";
    }

    public static String getManoutPlateResponseTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/uscita/manuale/lettura_targa";
    }

    public static String getTicketPrintedTopic(String idCasello){
        return "casello/" + idCasello + "/ingresso/manuale/biglietto_erogato";
    }

    public static String getEnteringConfirmTopic(String idCasello){
        return "server/casello/" + idCasello + "/conferma_entrata";
    }

    public static String getOpenBarTopic(String idCasello){
        return "casello/" + idCasello + "/sbarra/ingresso/sbarra_aperta";
    }

    public static String getInsertTicketTopic(String idCasello){
        return "veicolo/" + idCasello + "/uscita/manuale/inserimento_biglietto";
    }

    public static String getRequestInfoTopic(String idCasello){
        return "casello/" + idCasello + "/uscita/manuale/richiesta_info";
    }

    public static String getResponseInfoTopic(String idCasello){
        return "server/casello/" + idCasello + "/uscita/manuale/info_viaggio";
    }

    public static String getInsertMoneyTopic(String idCasello){
        return "veicolo/" + idCasello + "/uscita/manuale/inserimento_denaro";
    }

    public static String getPaymentDoneTopic(String idCasello){
        return "casello/" + idCasello + "/uscita/manuale/pagamento_effettuato";
    }

    public static String getExitConfirmTopic(String idCasello){
        return "server/casello/" + idCasello + "/conferma_uscita";
    }

    //VIAGGIO CON TELEPASS
    public static String getAutoEnteringRequestTopic(String idCasello){
        return "veicolo/" + idCasello + "/ingresso/automatico/richiesta_entrata";
    }

    public static String getAutoInPlateRequestTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/ingresso/automatico/richiesta_targa";
    }

    public static String getAutoInPlateResponseTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/ingresso/automatico/lettura_targa";
    }

    public static String getEnteringDataTopic(String idCasello){
        return "casello/" + idCasello + "/ingresso/automatico/invia_dati";
    }

    public static String getAutoExitRequestTopic(String idCasello){
        return "veicolo/" + idCasello  + "/uscita/automatico/richiesta_uscita";
    }

    public static String getAutoOutPlateRequestTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/uscita/automatico/richiesta_targa";
    }

    public static String getAutoOutPlateResponseTopic(String idCasello){
        return "casello/" + idCasello + "/telecamera/uscita/automatico/lettura_targa";
    }

    public static String getExitDataTopic(String idCasello){
        return "casello/" + idCasello + "/uscita/automatico/invia_dati";
    }

    // Costruttore privato per evitare istanziazione
    private TopicManager() {}

    public static SSLSocketFactory getSocketFactory(final String caCrtFile,
                                                    final String crtFile, final String keyFile, final String password)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // load CA certificate
        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caCrtFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client certificate
        bis = new BufferedInputStream(new FileInputStream(crtFile));
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client private key
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                .build(password.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                .setProvider("BC");
        KeyPair key;
        if (object instanceof PEMEncryptedKeyPair) {
            // System.out.println("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
        } else if (object instanceof PrivateKeyInfo) {
            // System.out.println("Unencrypted PrivateKeyInfo key - no password needed");
            key = converter.getKeyPair(convertPrivateKeyFromPKCS8ToPKCS1((PrivateKeyInfo)object));
        } else {
            // System.out.println("Unencrypted key - no password needed");
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    private static PEMKeyPair convertPrivateKeyFromPKCS8ToPKCS1(PrivateKeyInfo privateKeyInfo) throws Exception {
        // Parse the key wrapping to determine the internal key structure
        ASN1Encodable asn1PrivateKey = privateKeyInfo.parsePrivateKey();
        // Convert the parsed key to an RSA private key
        RSAPrivateKey keyStruct = RSAPrivateKey.getInstance(asn1PrivateKey);
        // Create the RSA public key from the modulus and exponent
        RSAPublicKey pubSpec = new RSAPublicKey(
                keyStruct.getModulus(), keyStruct.getPublicExponent());
        // Create an algorithm identifier for forming the key pair
        AlgorithmIdentifier algId = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        // System.out.println("Converted private key from PKCS #8 to PKCS #1 RSA private key\n");
        // Create the key pair container
        return new PEMKeyPair(new SubjectPublicKeyInfo(algId, pubSpec), new PrivateKeyInfo(algId, keyStruct));
    }



}
