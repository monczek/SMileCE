package de.fau.cs.mad.smile.android.encryption.crypto;


import android.util.Log;

import org.joda.time.DateTime;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.X509Extension;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.spongycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;

import de.fau.cs.mad.smile.android.encryption.SMileCrypto;

public class SelfSignedCertificateCreator {

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static int validateName(String name) {
        if(name != null) {
            if(name.length() > 0) {
                if(name.contains("=") ||name.contains(",")) {
                    return SMileCrypto.STATUS_NAME_INVALID_CHARACTER;
                } else {
                    return SMileCrypto.STATUS_NAME_OK;
                }
            } else {
                return SMileCrypto.STATUS_NAME_EMPTY;
            }
        } else {
            return SMileCrypto.STATUS_NO_NAME;
        }
    }

    public static int validateEmail(String email) {
        if(email != null) {
            if(email.length() > 0) {
                if(email.contains("=") || email.contains(",")) {
                    return SMileCrypto.STATUS_EMAIL_INVALID_CHARACTER;
                } else {
                    if(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        return SMileCrypto.STATUS_EMAIL_OK;
                    } else {
                        return SMileCrypto.STATUS_EMAIL_INVALID;
                    }
                }
            } else {
                return SMileCrypto.STATUS_EMAIL_EMPTY;
            }
        } else {
            return SMileCrypto.STATUS_NO_EMAIL;
        }
    }

    public static int createCert(String name, String email, String expert, DateTime end , String passphrase) {
        if(passphrase == null || passphrase.length() == 0) {
            return SMileCrypto.STATUS_NO_PASSPHRASE;
        }

        X500Name x500Name;
        if(expert != null && expert.length() > 0) {
            x500Name = new X500Name(expert);
        } else {
            int vname = validateName(name);
            if (vname != SMileCrypto.STATUS_NAME_OK) {
                return vname;
            }

            int vemail = validateEmail(email);
            if (vemail != SMileCrypto.STATUS_EMAIL_OK) {
                return vemail;
            }

            X500NameBuilder nameBuilder = new X500NameBuilder();
            nameBuilder.addRDN(BCStyle.CN, name);
            nameBuilder.addRDN(BCStyle.E, email);
            x500Name = nameBuilder.build();
        }
        SecureRandom random = new SecureRandom();
        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4);
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(spec, random);
            KeyPair pair = generator.generateKeyPair();

            JcaX509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(x500Name,
                    BigInteger.valueOf(1), DateTime.now().toDate(), end.toDate(), x500Name,
                    pair.getPublic());
            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA1WithRSA");
            signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
            ContentSigner contentSigner = signerBuilder.build(pair.getPrivate());
            v3CertGen.addExtension(X509Extension.basicConstraints, true, new BasicConstraints(1));

            JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

            SubjectKeyIdentifier subjectKeyIdentifier = extensionUtils.createSubjectKeyIdentifier(pair.getPublic());
            v3CertGen.addExtension(X509Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);

            KeyUsage keyUsage = new KeyUsage(KeyUsage.keyCertSign);
            v3CertGen.addExtension(X509Extension.keyUsage, true, keyUsage);

            ExtendedKeyUsage extendedKeyUsage  = new ExtendedKeyUsage(KeyPurposeId.anyExtendedKeyUsage);
            v3CertGen.addExtension(X509Extension.extendedKeyUsage, false, extendedKeyUsage);

            X509CertificateHolder certificateHolder = v3CertGen.build(contentSigner);
            JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
            certificateConverter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate certificate = certificateConverter.getCertificate(certificateHolder);
            KeyManagement km = KeyManagement.getInstance();
            if(km.addPrivateKeyFromCert(certificate, pair.getPrivate(), passphrase)) {
                return SMileCrypto.STATUS_SAVED_CERT;
            } else {
                return SMileCrypto.STATUS_FAILED_SAVE_CERT;
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | OperatorCreationException | InvalidAlgorithmParameterException | CertificateException | KeyStoreException | IOException e) {
            Log.d(SMileCrypto.LOG_TAG, "Error creating self signed certificate. " + e.getMessage());
            return SMileCrypto.STATUS_FAILED_SAVE_CERT;
        }
    }
}
