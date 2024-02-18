package com.musalasoft.weatherapp.utils.certificate

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * TrustManager to retrieve Open Weather Map certificates.
 */
internal class CertificatesTrustManager : X509TrustManager {
    var certificates = arrayOfNulls<X509Certificate>(0)


    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        throw CertificateException("Client certificates are not trusted")
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate?>, authType: String) {
        certificates = chain
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls(0)
    }
}