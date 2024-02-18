package com.musalasoft.weatherapp.utils.certificate

import android.content.Context
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertPathValidatorException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManagerFactory

/**
 * Useful methods for work with certificate.
 */
object CertificateUtils {
    /**
     * Check is [IOException] is problem with certificate or something different.
     * @param exception exception
     * @return `true` if exception related to certificate and `false` otherwise
     */
    fun isCertificateException(exception: IOException?): Boolean {
        if (exception !is SSLHandshakeException) return false
        var cause: Throwable? = exception
        do {
            cause = cause!!.cause
        } while (cause != null && cause !is CertPathValidatorException)
        return cause != null
    }

    /**
     * Create [SSLContext] with Open Weather Map certificate as trusted.
     * @param context Android context
     * @param doNotRetry (out) will be set into `true` if this operation shouldn't be repeated
     * @param fetchCertificate if this is `true`, certificate will be fetched even it already exists
     * @return [SSLContext] with Open Weather Map certificate as trusted
     */
    fun addCertificate(
        context: Context,
        doNotRetry: AtomicBoolean,
        fetchCertificate: Boolean
    ): SSLContext? {
        var result: SSLContext?
        try {
            // Load CAs from an InputStream
            val cf = CertificateFactory.getInstance("X.509")
            val certificateDownloader = CertificateDownloader()
            if (!certificateDownloader.isCertificateDownloaded(context) || fetchCertificate) {
                if (!certificateDownloader.fetch(context)) {
                    if (CertificateDownloader.hasCertificateBeenFetchedThisLaunch()) {
                        doNotRetry.set(true)
                    }
                    return null
                } else {
                    doNotRetry.set(CertificateDownloader.hasCertificateBeenFetchedThisLaunch())
                }
            }
            val certificateInputStream: InputStream =
                certificateDownloader.getCertificateInputStream(context)
            val caInput: InputStream = BufferedInputStream(certificateInputStream)
            val ca: Certificate
            try {
                ca = cf.generateCertificate(caInput)
                println("ca=" + (ca as X509Certificate).subjectDN)
            } finally {
                caInput.close()
            }

            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", ca)

            // Create a TrustManager that trusts the CAs in our KeyStore
            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)

            // Create an SSLContext that uses our TrustManager
            result = SSLContext.getInstance("TLS")
            result.init(null, tmf.trustManagers, null)
        } catch (e: IOException) {
            e.printStackTrace()
            doNotRetry.set(false)
            result = null
        } catch (e: CertificateException) {
            e.printStackTrace()
            doNotRetry.set(true)
            result = null
        } catch (e: KeyStoreException) {
            e.printStackTrace()
            doNotRetry.set(true)
            result = null
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            doNotRetry.set(true)
            result = null
        } catch (e: KeyManagementException) {
            e.printStackTrace()
            doNotRetry.set(true)
            result = null
        }
        return result
    }
}