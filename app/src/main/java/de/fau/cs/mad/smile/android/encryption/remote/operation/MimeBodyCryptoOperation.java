package de.fau.cs.mad.smile.android.encryption.remote.operation;

import android.content.Intent;
import android.os.ParcelFileDescriptor;

import org.spongycastle.cms.CMSException;
import org.spongycastle.mail.smime.SMIMEException;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.x509.CertPathReviewerException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.smile.android.encryption.crypto.CryptoParams;
import de.fau.cs.mad.smile.android.encryption.remote.MimeBodyLoaderTaskBuilder;
import korex.mail.MessagingException;
import korex.mail.internet.AddressException;
import korex.mail.internet.MimeBodyPart;

abstract class MimeBodyCryptoOperation extends CryptoOperation<MimeBodyPart> {
    MimeBodyCryptoOperation(Intent data, ParcelFileDescriptor input, ParcelFileDescriptor output) throws IOException, AddressException, CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException {
        super(data, input, output, new MimeBodyLoaderTaskBuilder());
    }

    @Override
    public void execute() throws MessagingException, IOException, OperatorCreationException, GeneralSecurityException, SMIMEException, CMSException, CertPathReviewerException, ExecutionException, InterruptedException {
        final MimeBodyPart source = preProcess();
        final CryptoParams cryptoParams = cryptoParamsLoaderTask.get();
        final MimeBodyPart processed = process(source, cryptoParams);
        if (processed != null && outputStream != null) {
            processed.writeTo(outputStream);
        }
    }
}
