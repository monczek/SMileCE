package de.fau.cs.mad.smile_crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

public class KeyManagement {
    public KeyManagement() {}

    public Boolean addPrivateKeyFromP12ToKeyStore(String pathToFile, String passphrase) {
        try {
            KeyStore p12 = KeyStore.getInstance("pkcs12");
            p12.load(new FileInputStream(pathToFile), passphrase.toCharArray());
            Enumeration e = p12.aliases();
            while (e.hasMoreElements()) {
                String alias = (String) e.nextElement();
                X509Certificate c = (X509Certificate) p12.getCertificate(alias);
                Log.d(SMileCrypto.LOG_TAG, "Found certificate with alias: " + alias);
                Log.d(SMileCrypto.LOG_TAG, "· SubjectDN: " + c.getSubjectDN().getName());
                Log.d(SMileCrypto.LOG_TAG, "· IssuerDN: " + c.getIssuerDN().getName());

                PrivateKey key = (PrivateKey) p12.getKey(alias, passphrase.toCharArray());
                String new_alias = addCertificateToKeyStore(key, c);

                copyP12ToInternalDir(pathToFile, new_alias);
                return savePassphrase(new_alias, passphrase);
            }
            return true;
        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error while loading keyStore: " + e.getMessage());
            return false;
        }
    }

    /*TODO: add certificate from someone else (without private key) */

    public ArrayList<KeyInfo> getOwnCertificates() {
        ArrayList<KeyInfo> keylist = new ArrayList<>();
        try {
            Log.d(SMileCrypto.LOG_TAG, "Find all own certificates…");
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration e = ks.aliases();
            while (e.hasMoreElements()) {
                String alias = (String) e.nextElement();
                Log.d(SMileCrypto.LOG_TAG, "Found certificate with alias: " + alias);
                if(alias.equals(App.getContext().getString(R.string.smile_save_passphrases_certificate_alias)))
                    continue;
                Certificate c = ks.getCertificate(alias);
                KeyStore.Entry entry = ks.getEntry(alias, null);
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    KeyInfo ki = new KeyInfo();
                    ki.alias = alias;
                    Log.d(SMileCrypto.LOG_TAG, "· Type: " + c.getType());
                    Log.d(SMileCrypto.LOG_TAG, "· HashCode: " + c.hashCode());
                    ki.type = c.getType();
                    ki.hash = Integer.toHexString(c.hashCode());
                    if(c.getType().equals("X.509")) {
                        X509Certificate cert = (X509Certificate) c;
                        ki.contact = cert.getSubjectX500Principal().getName();
                        //ki.mail; TODO
                        ki.termination_date = cert.getNotAfter();
                        //ki.trust; TODO
                    }
                    keylist.add(ki);
                } else {
                    //--> no private key available for this certificate
                    //currently there are no such entries because yet we cannot import the certs of
                    //others, e.g. by using their signature.
                    Log.d(SMileCrypto.LOG_TAG, "Not an instance of a PrivateKeyEntry");
                    Log.d(SMileCrypto.LOG_TAG, "· Type: " + c.getType());
                    Log.d(SMileCrypto.LOG_TAG, "· HashCode: " + c.hashCode());
                }
            }
        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error while finding certificate: " + e.getMessage());
        }
        return keylist;
    }

    public ArrayList<KeyInfo> getAllCertificates() {
        ArrayList<KeyInfo> keylist = new ArrayList<>();
        try {
            Log.d(SMileCrypto.LOG_TAG, "Find all own certificates…");
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration e = ks.aliases();
            while (e.hasMoreElements()) {
                String alias = (String) e.nextElement();
                Log.d(SMileCrypto.LOG_TAG, "Found certificate with alias: " + alias);
                if(alias.equals(App.getContext().getString(R.string.smile_save_passphrases_certificate_alias)))
                    continue;
                Certificate c = ks.getCertificate(alias);
                KeyStore.Entry entry = ks.getEntry(alias, null);
                KeyInfo ki = new KeyInfo();
                ki.alias = alias;
                Log.d(SMileCrypto.LOG_TAG, "· Type: " + c.getType());
                Log.d(SMileCrypto.LOG_TAG, "· HashCode: " + c.hashCode());
                ki.type = c.getType();
                ki.hash = Integer.toHexString(c.hashCode());
                if(c.getType().equals("X.509")) {
                    X509Certificate cert = (X509Certificate) c;
                    ki.contact = cert.getSubjectX500Principal().getName();
                    //ki.mail; TODO
                    ki.termination_date = cert.getNotAfter();
                    //ki.trust; TODO
                }
                keylist.add(ki);
            }
        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error while finding certificate: " + e.getMessage());
        }
        return keylist;
    }

    public Boolean deleteKey(String alias) {
        try {
            Log.d(SMileCrypto.LOG_TAG, "Delete key with alias: " + alias);
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if(keyStore.containsAlias(alias))
                keyStore.deleteEntry(alias);

            return deletePassphrase(alias) && deleteP12FromInternalDir(alias);
        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error while deleting key: " + e.getMessage());
            return false;
        }
    }

    private Boolean copyP12ToInternalDir(String pathToFile, String alias) {
        File src = new File(pathToFile);
        File certDirectory = App.getContext().getApplicationContext().getDir("smime-certificates", Context.MODE_PRIVATE);
        String filename = alias + ".p12";
        File dst = new File(certDirectory, filename);

        try {
            FileChannel inChannel = new FileInputStream(src).getChannel();
            FileChannel outChannel = new FileOutputStream(dst).getChannel();

            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();

            Log.d(SMileCrypto.LOG_TAG, "Copied p12 to interal storage, filename: " + filename);
            return true;

        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error copying .p12 to internal storage: " + e.getMessage());
            return false;
        }
    }

    private String addCertificateToKeyStore(PrivateKey key, X509Certificate c) {
        try {
            Log.d(SMileCrypto.LOG_TAG, "Import certificate to keyStore.");
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            long importTime = System.currentTimeMillis();
            String alias = "SMile_crypto_" + Long.toString(importTime); //TODO: other alias?
            ks.setKeyEntry(alias, key, null, new Certificate[]{c});

            Toast.makeText(App.getContext(), R.string.import_certificate_successful, Toast.LENGTH_SHORT).show();
            return alias;
        } catch (Exception e){
            Log.e(SMileCrypto.LOG_TAG, "Error while importing certificate: " + e.getMessage());
            Toast.makeText(App.getContext(), R.string.error + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private Boolean savePassphrase(String alias, String passphrase) {
        try {
            PasswordEncryption passwordEncryption = new PasswordEncryption(App.getContext().getResources().getString(R.string.smile_save_passphrases_certificate_alias));

            Log.d(SMileCrypto.LOG_TAG, "Encrypt passphrase for alias: " + alias);
            String encryptedPassphrase = passwordEncryption.encryptString(passphrase);

            if(encryptedPassphrase == null)
                return false;

            Log.d(SMileCrypto.LOG_TAG, "Encrypted passphrase will be saved in preferences:  " + encryptedPassphrase);

            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(App.getContext().getApplicationContext()).edit();
            e.putString(alias + "-passphrase", encryptedPassphrase);
            e.commit();

            return true;
        } catch (Exception e) {
            Log.e(SMileCrypto.LOG_TAG, "Error while saving passphrase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Boolean deleteP12FromInternalDir(String alias) {
        File certDirectory = App.getContext().getApplicationContext().getDir("smime-certificates", Context.MODE_PRIVATE);
        String filename = alias + ".p12";
        File toBeDeleted = new File(certDirectory, filename);
        return toBeDeleted.delete();
    }

    private Boolean deletePassphrase(String alias) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(App.getContext().getApplicationContext()).edit();
        e.remove(alias + "-passphrase");
        e.commit();
        return true;
    }
}