package com.submission.myworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import com.squareup.moshi.Moshi
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.lang.Exception
import java.text.DecimalFormat
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    /*
    metode doWork adalah metode yang akan dipanggil ketika WorkManager berjalan, dan baris kode didalamnya secara otomatis
    berjalan di background thread. metode ini juga mengembalikan result untuk mengetahui status WorkManager yg berjalan, ada bebrapa status yang bisa dikembalikan
    1. Result.success(), result yang menandakan berhasil.
    2. Result.failure(), result yang menandakan gagal.
    3. Result.retry(), result yang menandakan untuk mengulang task lagi.
    Pada kode ini menggunakan getCurrentWeather yg mengembalikan nilai result, dimana getCurrentWeather menggunakan library LoopJ yg mengimplementasikan 2 method
    yaitu onSuccess dan onFailure, disinilah nilai result pada method doWork diambil melalui method getCurrentWeather
     */
    override fun doWork(): Result {
        /*
        Data yang dikirimkan dari MainActivity ditangkap oleh baris kode dibawah ini, yang perlu diperhatikan adalah
        tipe data dan key yang digunakan harus sama persis
         */
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentWeather(dataCity)
    }

    private fun getCurrentWeather(city: String?): Result {
        Log.d(TAG, "getCurrentWeather: Mulai....")
        Looper.prepare()
        /*
        Penggunaan SyncHttpClient untuk koneksi data yang sifatnya synchronus kita menggunakan SyncHttpClient khusus di WorkManager
        kita perlu menjalankan proses secara synchronus supaya bisa mendapatkan result success, juga karena pada defaultnya WorkManager
        berjalan pada background. Namun jika kita ingin menggunakannya di Activity maka kita pakai AsyncHttpClient agar tidak terjadi error
        NetworkOnMainThread
         */
        val client = SyncHttpClient()
        /*
        Nilai pada bagian city keita beri nilai sesuai dari inputan EditText
         */
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        client.post(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>,
                responseBody: ByteArray
            ) {
                val result = String(responseBody)
                Log.d(TAG, "onSuccess: $result")
                try {
//                    val responseObject = JSONObject(result)
                    /*
                    Terdapat 2 jenis JSON yaitu JSONArray dan JSONObject pada contoh ini kita akan mengambil data main maka kita ambil JSONArray weather,
                    kemudian ambil JSONObject index ke 0, kemudian ambil nilai 'main' atau 'description'
                     */
//                    val currentWeather: String = responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
//                    val description: String = responseObject.getJSONArray("weather").getJSONObject(0).getString("description")
                    /*
                    Untuk mengambil nilai temp kita ambil JSONObject main baru kemudian ambil nilai 'temp'
                    SAAT MENGAMBIL DATA PASTIKAN TEKS DARI NILAI YANG DIAMBIL SAMA DENGAN YANG BERASAL DARI API
                     */
//                    val tempInKelvin = responseObject.getJSONObject("main").getDouble("temp")
                    val moshi = Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                    val jsonAdapter = moshi.adapter(Response::class.java)
                    val response = jsonAdapter.fromJson(result)
                    response?.let {
                        val currentWeather = it.weatherList[0].main
                        val description = it.weatherList[0].description
                        val tempInKelvin = it.main.temperature
                    }
                    val tempInCelcius = response.tempInKelvin - 273
                    val temperature: String = DecimalFormat("##.##").format(tempInCelcius)
                    val title = "Current Weather in $city"
                    val message = "$currentWeather, $description, with $temperature celcius"
                    showNotification(title, message)
                    Log.d(TAG, "onSuccess: Selesai...")
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    showNotification("Get Current Weather Not Success", e.message)
                    Log.d(TAG, "onSuccess: Gagal...")
                    resultStatus = Result.failure()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable
            ) {
                Log.d(TAG, "onFailure: Gagal...")
                showNotification("Get Current Weather Failed", error.message)
                resultStatus = Result.failure()
            }

        })
        return resultStatus as Result
    }

    private fun showNotification(title: String, description: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private var resultStatus: Result? = null

    companion object{
        private val TAG = MyWorker::class.java.simpleName
        const val APP_ID = "ad088534916b06e6608d3e9db5b31f82"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_1"
        const val CHANNEL_NAME = "dicoding channel"
    }

}