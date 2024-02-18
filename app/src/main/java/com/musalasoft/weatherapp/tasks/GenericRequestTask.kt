package com.musalasoft.weatherapp.tasks

import com.musalasoft.weatherapp.R
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.musalasoft.weatherapp.constant.Constants
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.utils.Language
import com.musalasoft.weatherapp.utils.certificate.CertificateUtils
import com.musalasoft.weatherapp.weatherapi.WeatherStorage
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

abstract class GenericRequestTask(
    protected var context: Context,
    activity: MainActivity,
    progressDialog: ProgressDialog
) :
    AsyncTask<String?, String?, TaskOutput>() {
    var progressDialog: ProgressDialog
    protected var activity: MainActivity
    protected var weatherStorage: WeatherStorage
    var loading = 0

    init {
        this.activity = activity
        this.progressDialog = progressDialog
        weatherStorage = WeatherStorage(activity)
    }

    override fun onPreExecute() {
        incLoadingCounter()
        if (!progressDialog.isShowing) {
            progressDialog.setMessage(context.getString(R.string.downloading_data))
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
        }
    }

    override fun doInBackground(vararg params: String?): TaskOutput {
        val output = TaskOutput()
        var response = ""
        var reqParams = arrayOf<String>()
        if (params != null && params.size > 0) {
            val zeroParam = params[0]
            if ("cachedResponse" == zeroParam) {
                response = params[1]!!
                // Actually we did nothing in this case :)
                output.taskResult = TaskResult.SUCCESS
            } else if ("coords" == zeroParam) {
                val lat = params[1]
                val lon = params[2]
                reqParams = arrayOf("coords", lat!!, lon!!)
            } else if ("city" == zeroParam) {
                reqParams = arrayOf("city", params[1]!!)
            }
        }
        if (response.isEmpty()) {
            response = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                makeRequest(output, response, reqParams)
            } else {
                makeRequestWithCheckForCertificate(output, response, reqParams)
            }
        }
        if (TaskResult.SUCCESS.equals(output.taskResult)) {
            // Parse JSON data
            val parseResult: ParseResult = parseResponse(response)
            if (ParseResult.CITY_NOT_FOUND.equals(parseResult)) {
                // Retain previously specified city if current one was not recognized
                restorePreviousCity()
            }
            output.parseResult = parseResult
        }
        return output
    }

    private fun makeRequest(
        output: TaskOutput,
        response: String,
        reqParams: Array<String>
    ): String {
        var response = response
        try {
            val url = provideURL(reqParams)
            Log.i("URL", url.toString())
            val urlConnection = url.openConnection() as HttpURLConnection
            if (urlConnection is HttpsURLConnection) {
                try {
                    certificateCountDownLatch.await()
                    if (sslContext != null) {
                        val socketFactory = sslContext!!.socketFactory
                        urlConnection.sslSocketFactory =
                            socketFactory
                    }
                    certificateCountDownLatch.countDown()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            if (urlConnection.responseCode == 200) {
                val inputStreamReader = InputStreamReader(urlConnection.inputStream)
                val r = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                    stringBuilder.append("\n")
                }
                response += stringBuilder.toString()
                close(r)
                urlConnection.disconnect()
                // Background work finished successfully
                Log.i("Task", "done successfully")
                output.taskResult = TaskResult.SUCCESS
                // Save date/time for latest successful result
                MainActivity.saveLastUpdateTime(
                    PreferenceManager.getDefaultSharedPreferences(
                        context
                    )
                )
            } else if (urlConnection.responseCode == 401) {
                // Invalid API key
                Log.w("Task", "invalid API key")
                output.taskResult = TaskResult.INVALID_API_KEY
            } else if (urlConnection.responseCode == 429) {
                // Too many requests
                Log.w("Task", "too many requests")
                output.taskResult = TaskResult.TOO_MANY_REQUESTS
            } else {
                // Bad response from server
                Log.w("Task", "http error " + urlConnection.responseCode)
                output.taskResult = TaskResult.HTTP_ERROR
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Exception while reading data from url connection
            output.taskResult = TaskResult.IO_EXCEPTION
            output.taskError = e
        }
        return response
    }

    private fun makeRequestWithCheckForCertificate(
        output: TaskOutput,
        response: String,
        reqParams: Array<String>
    ): String {
        var response = response
        var tryAgain = false
        do {
            response = makeRequest(output, response, reqParams)
            if (output.taskResult === TaskResult.IO_EXCEPTION && output.taskError is IOException) {
                if (CertificateUtils.isCertificateException(output.taskError as IOException)) {
                    Log.e("Invalid Certificate", output.taskError!!.message!!)
                    try {
                        certificateCountDownLatch.await()
                        tryAgain = !certificateTried || !certificateFetchTried
                        if (tryAgain) {
                            val doNotRetry = AtomicBoolean(false)
                            sslContext = CertificateUtils.addCertificate(
                                context, doNotRetry,
                                certificateTried
                            )
                            certificateTried = true
                            if (!certificateFetchTried) {
                                certificateFetchTried = doNotRetry.get()
                            }
                            tryAgain = sslContext != null
                        }
                        certificateCountDownLatch.countDown()
                    } catch (ex: InterruptedException) {
                        Log.e("Invalid Certificate", "await had been interrupted")
                        ex.printStackTrace()
                    }
                } else {
                    Log.e("IOException Data", response)
                    tryAgain = false
                }
            } else {
                tryAgain = false
            }
        } while (tryAgain)
        return response
    }

    override fun onPostExecute(output: TaskOutput) {
        if (loading == 1) {
            progressDialog.dismiss()
        }
        decLoadingCounter()
        updateMainUI()
        handleTaskOutput(output)
    }

    protected fun handleTaskOutput(output: TaskOutput) {
        when (output.taskResult) {
            TaskResult.SUCCESS -> {
                val parseResult: ParseResult = output.parseResult!!
                if (ParseResult.CITY_NOT_FOUND.equals(parseResult)) {
                    Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        context.getString(R.string.msg_city_not_found),
                        Snackbar.LENGTH_LONG
                    ).show()
                } else if (ParseResult.JSON_EXCEPTION.equals(parseResult)) {
                    Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        context.getString(R.string.msg_err_parsing_json),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            TaskResult.TOO_MANY_REQUESTS -> Snackbar.make(
                activity.findViewById(android.R.id.content),
                context.getString(R.string.msg_too_many_requests),
                Snackbar.LENGTH_LONG
            ).show()

            TaskResult.INVALID_API_KEY -> Snackbar.make(
                activity.findViewById(android.R.id.content),
                context.getString(R.string.msg_invalid_api_key),
                Snackbar.LENGTH_LONG
            ).show()

            TaskResult.HTTP_ERROR -> Snackbar.make(
                activity.findViewById(android.R.id.content),
                context.getString(R.string.msg_http_error),
                Snackbar.LENGTH_LONG
            ).show()

            TaskResult.IO_EXCEPTION -> Snackbar.make(
                activity.findViewById(android.R.id.content),
                context.getString(R.string.msg_connection_not_available),
                Snackbar.LENGTH_LONG
            ).show()

            null -> {}
        }
    }

    @Throws(UnsupportedEncodingException::class, MalformedURLException::class)
    private fun provideURL(reqParams: Array<String>): URL {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val apiKey = sp.getString("apiKey", context.getString(R.string.apiKey))
        val urlBuilder = StringBuilder("https://api.openweathermap.org/data/2.5/")
        urlBuilder.append(aPIName).append("?")
        if (reqParams.size > 0) {
            val zeroParam = reqParams[0]
            if ("coords" == zeroParam) {
                urlBuilder.append("lat=").append(reqParams[1]).append("&lon=").append(reqParams[2])
            } else if ("city" == zeroParam) {
                urlBuilder.append("q=").append(reqParams[1])
            }
        } else {
            val cityId = sp.getString("cityId", Constants.DEFAULT_CITY_ID)
            urlBuilder.append("id=").append(URLEncoder.encode(cityId, "UTF-8"))
        }
        urlBuilder.append("&lang=").append(Language.owmLanguage)
        urlBuilder.append("&mode=json")
        urlBuilder.append("&appid=").append(apiKey)
        return URL(urlBuilder.toString())
    }

    @SuppressLint("ApplySharedPref")
    private fun restorePreviousCity() {
        if (activity.recentCityId != null) {
            weatherStorage.cityId = activity.recentCityId!!
            activity.recentCityId = null
        }
    }

    private fun incLoadingCounter() {
        loading++
    }

    private fun decLoadingCounter() {
        loading--
    }

    protected open fun updateMainUI() {}
    protected abstract fun parseResponse(response: String?): ParseResult
    protected abstract val aPIName: String?

    companion object {
        private val certificateCountDownLatch = CountDownLatch(0)
        private var certificateTried = false
        private var certificateFetchTried = false
        private var sslContext: SSLContext? = null
        private fun close(x: Closeable?) {
            try {
                x?.close()
            } catch (e: IOException) {
                Log.e("IOException Data", "Error occurred while closing stream")
            }
        }
    }
}
