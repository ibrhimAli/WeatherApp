package com.musalasoft.weatherapp.utils.certificate

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * Class to get and fetch [X509Certificate] for Open Weather Map.
 */
class CertificateDownloader {
    /**
     * Fetch certificate and save it into file.
     * <br></br>
     * [.hasCertificateBeenFetchedThisLaunch] will be able to return `true` if
     * fetching is failed not due connection problems but a problem with the file or
     * unsupported protocols.
     * @param context Android context
     * @return `true` if certificate has been fetched and saved into the file and
     * `false` otherwise.
     * @see .hasCertificateBeenFetchedThisLaunch
     * @see .isCertificateDownloaded
     * @see .getCertificateInputStream
     */
    fun fetch(context: Context): Boolean {
        Log.d(TAG, "try to fetch certificate")
        var result = false
        val trustManager = CertificatesTrustManager()
        try {
            val sslContext = SSLContext.getInstance("TLS")
            val trustManagers = arrayOfNulls<TrustManager>(1)
            trustManagers[0] = trustManager
            sslContext.init(null, trustManagers, null)
            val connection = URL(OWN_URL).openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.sslSocketFactory = sslContext.socketFactory
            // TODO host name verifier ?
            result = try {
                connection.connect()
                Log.d(
                    TAG,
                    "connection established, try to save certificate if it has been received"
                )
                saveCertificate(context, trustManager)
            } finally {
                connection.disconnect()
            }
            certificateHasBeenFetchedThisLaunch = true
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "certificate fetching is failed", e)
            certificateHasBeenFetchedThisLaunch = true
        } catch (e: KeyManagementException) {
            Log.e(TAG, "certificate fetching is failed", e)
            certificateHasBeenFetchedThisLaunch = true
        } catch (e: MalformedURLException) {
            Log.e(TAG, "certificate fetching is failed", e)
            certificateHasBeenFetchedThisLaunch = true
        } catch (e: IOException) {
            if (CertificateUtils.isCertificateException(e)) {
                Log.d(TAG, "try to save certificate if it has been received")
                result = saveCertificate(context, trustManager)
            } else {
                Log.e(TAG, "certificate fetching is failed", e)
            }
        }
        return result
    }

    private fun saveCertificate(context: Context, trustManager: CertificatesTrustManager): Boolean {
        val certificates: Array<X509Certificate?> = trustManager.certificates
        if (certificates.size == 0) {
            Log.d(TAG, "certificates array is empty")
            certificateHasBeenFetchedThisLaunch = true
            return false
        }
        val certificate = certificates[certificates.size - 1]
        val certificateFile = getCertificateFile(context)
        if (certificateFile.exists()) {
            Log.d(
                TAG,
                "try to remove old file: $certificateFile"
            )
            val deleted = certificateFile.delete()
            if (!deleted) {
                Log.d(TAG, "can not to delete old certificate file")
                certificateHasBeenFetchedThisLaunch = true
                return false
            }
        }
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(certificateFile)
            fos.write(certificate!!.encoded)
            Log.d(TAG, "certificate successfully saved")
        } catch (e: CertificateEncodingException) {
            certificateHasBeenFetchedThisLaunch = true
            e.printStackTrace()
        } catch (e: IOException) {
            certificateHasBeenFetchedThisLaunch = true
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return true
    }

    /**
     * Check is certificate file already downloaded (i.e. exists).
     * @param context Android context
     * @return `true` if file exists and `false` otherwise
     */
    fun isCertificateDownloaded(context: Context): Boolean {
        val certificateFile = getCertificateFile(context)
        return certificateFile.exists()
    }

    /**
     * Returns [InputStream] with certificate.
     * <br></br>
     * [FileNotFoundException] will be thrown if file isn't exist. Use
     * [.isCertificateDownloaded] to check is file exists.
     * @param context Android context
     * @return [InputStream] with certificate
     * @see .isCertificateDownloaded
     * @throws FileNotFoundException will be thrown if file isn't exist. Use
     * [.isCertificateDownloaded] to check is file exists.
     */
    @Throws(FileNotFoundException::class)
    fun getCertificateInputStream(context: Context): InputStream {
        val certificateFile = getCertificateFile(context)
        return FileInputStream(certificateFile)
    }

    private fun getCertificateFile(context: Context): File {
        val dir = context.filesDir
        return File(dir, CERTIFICATE_FILE_NAME)
    }

    companion object {
        private const val TAG = "CertificateDownloader"
        const val CERTIFICATE_FILE_NAME = "openweathermap.crt"
        private const val OWN_URL = "https://api.openweathermap.org"
        private var certificateHasBeenFetchedThisLaunch = false

        /**
         * @return `true` if certificate has been already fetched during this launch and
         * `false` if certificate hasn't ever been downloaded or was downloaded earlier
         */
        fun hasCertificateBeenFetchedThisLaunch(): Boolean {
            return certificateHasBeenFetchedThisLaunch
        }
    }
}