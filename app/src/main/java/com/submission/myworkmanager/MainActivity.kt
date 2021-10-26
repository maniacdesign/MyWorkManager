package com.submission.myworkmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.work.*
import androidx.work.WorkInfo.State.*
import com.submission.myworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    /*
    Terdapat 2 macam request yang dapat dibuat
    1. OneTimeRequest, untuk membuatnya menggunakan baris kode berikut:
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java).build()
        workManager.enqueue(oneTimeWorkRequest)
    2. PeriodicWorkRequest, untuk membuatnya menggunakan baris kode berikut:
         val periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES).build()
         workManager.enqueue(periodicWorkRequest)
         *dengan catatan interval minimal adalah 15 menit
     */

    private lateinit var binding: ActivityMainBinding
    private lateinit var workManager: WorkManager
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workManager = WorkManager.getInstance(this)
        binding.btnOneTimeTask.setOnClickListener(this)
        binding.btnPeriodicTask.setOnClickListener(this)
        binding.btnCancelTask.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnOneTimeTask -> startOneTimeTask()
            R.id.btnPeriodicTask -> startPeriodictask()
            R.id.btnCancelTask -> cancelPeriodicTask()
        }
    }

    /*
    Kode ini digunakan untuk membatalkan task berdasarkan id request. Selain menggunakan id, kita juga bisa menambahkan tag pada request
    kelebihan dari penggunaan tag yaitu kita bisa membatalkan task lebih dari 1 task
     */
    private fun cancelPeriodicTask() {
        workManager.cancelWorkById(periodicWorkRequest.id)
    }

    private fun startPeriodictask() {
        binding.textStatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        periodicWorkRequest =
            PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .build()
        workManager.enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity, { workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
                binding.btnCancelTask.isEnabled = false
                if (workInfo.state == ENQUEUED) {
                    binding.btnCancelTask.isEnabled = true
                }
            })
    }

    private fun startOneTimeTask() {
        binding.textStatus.text = getString(R.string.status)
        /*
        Fungsi ini untuk membuat onetime request. saat membuat request kita bisa menambahkan data untuk dikirimkan dengan membuat objek Data
        yang berisi data key-value, key yang dipakai disini yaitu MyWorker.EXTRA_CITY dengan tipe data string
         */
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()
        /*
        baris kode ini untuk memberikan syarat kapan task ini dijalankan
         */
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        /*
        Setelah data didapat pada variabel data, maka dikirim melalui setInputData. Ada beberapa kondisi yang bisa kita set disini
        1. setRequiredNetworkType, ketika bernilai CONNECTED berarti harus terhubung ke koneksi internet apapun jenisnya, jika hanya ingin terhubung
            ke WiFi maka nilai yang harus diset UNMETERED
        2. setRequiresDeviceldle, menentukan apakah task akan dijalankan ketika perangkat dalam keadaan sedang digunakan atau tidak. Secara default parameter ini
            bernilai false, jika kita ingin task dijalankan ketika perangkat dalam kondisi tidak digunakan maka kita beri nilai true
        3. setRequiresCharging, menentukan apakah task akan dijalankan ketika baterai sedang diisi atau tidak. Nilai true akan mengindikasikan bahwa task hanya
            berjalan ketika baterai sedang diisi. Kondisi ini dapat digunakan bila task yang dijalankan akan memakan waktu yang lama.
        4. setRequiresStorageNotLow, menentukan apakah task yang dijalankan membutuhkan ruang storage yang tidak sedikit. Secara default, nilainya bersifat false.
         */
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        workManager.enqueue(oneTimeWorkRequest)
        /*
        baris kode ini digunakan utnuk mengetahui status task yang dieksekusi, kita dapat membaca status secara live dengan menggunakan getWorkInfoByIdLiveData
         */
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity, { workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
            })
    }
}