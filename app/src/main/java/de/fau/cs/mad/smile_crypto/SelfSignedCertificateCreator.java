package de.fau.cs.mad.smile_crypto;


import android.util.Log;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

public class SelfSignedCertificateCreator {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private X509v1CertificateBuilder v1CertGen;
    private PrivateKey key;

    public SelfSignedCertificateCreator() throws OperatorCreationException, IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(
                new BigInteger("b4a7e46170574f16a97082b22be58b6a2a629798419be12872a4bdba626cfae9900f76abfb12139dce5de56564fab2b6543165a040c606887420e33d91ed7ed7", 16),
                new BigInteger("11", 16));

        RSAPrivateCrtKeySpec privKeySpec = new RSAPrivateCrtKeySpec(
                new BigInteger("b4a7e46170574f16a97082b22be58b6a2a629798419be12872a4bdba626cfae9900f76abfb12139dce5de56564fab2b6543165a040c606887420e33d91ed7ed7", 16),
                new BigInteger("11", 16),
                new BigInteger("9f66f6b05410cd503b2709e88115d55daced94d1a34d4e32bf824d0dde6028ae79c5f07b580f5dce240d7111f7ddb130a7945cd7d957d1920994da389f490c89", 16),
                new BigInteger("c0a0758cdf14256f78d4708c86becdead1b50ad4ad6c5c703e2168fbf37884cb", 16),
                new BigInteger("f01734d7960ea60070f1b06f2bb81bfac48ff192ae18451d5e56c734a5aab8a5", 16),
                new BigInteger("b54bb9edff22051d9ee60f9351a48591b6500a319429c069a3e335a1d6171391", 16),
                new BigInteger("d3d83daf2a0cecd3367ae6f8ae1aeb82e9ac2f816c6fc483533d8297dd7884cd", 16),
                new BigInteger("b8f52fc6f38593dabb661d3f50f8897f8106eee68b1bce78a95b132b4e5b5d19", 16));

        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        PrivateKey privKey = kf.generatePrivate(privKeySpec);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        key = privKey;
        //
        // signers name
        //
        String  issuer = "C=AU, O=SMile-crypto, OU=SMile Primary Certificate";

        //
        // subjects name - the same as we are self signed.
        //
        String  subject = "C=AU, O=SMile-crypto, OU=SMile Primary Certificate";

        //
        // create the certificate - version 1
        //
        v1CertGen = new JcaX509v1CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(1),
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
                new X500Name(subject), pubKey);
    }

    public void create() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, OperatorCreationException, NoSuchProviderException {
        X509CertificateHolder ch = v1CertGen.build(new JcaContentSignerBuilder("SHA1WithRSA").setProvider("SC").build(key));
        Log.e(SMileCrypto.LOG_TAG, "Holder created");
        X509Certificate c = new JcaX509CertificateConverter().setProvider( "SC" ).getCertificate(ch);
        Log.e(SMileCrypto.LOG_TAG, "Certificate created");
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        Log.e(SMileCrypto.LOG_TAG, "Got KeyStore instance");
        ks.load(null, null);
        Log.e(SMileCrypto.LOG_TAG, "Loaded");
        long importTime = System.currentTimeMillis();
        String alias = "SMile_crypto_selfsigned" + Long.toString(importTime); //TODO: other alias?
        Log.e(SMileCrypto.LOG_TAG, "Alias created: " + alias);
        ks.setKeyEntry(alias, key, null, new Certificate[]{c});
        Log.e(SMileCrypto.LOG_TAG, "KeyEntry set");
    }
}
