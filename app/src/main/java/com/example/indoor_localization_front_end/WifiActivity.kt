package com.example.indoor_localization_front_end

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.indoor_localization_front_end.databinding.ActivityWifiBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream


class WifiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWifiBinding
    private lateinit var retrofitService: RetrofitInterface

    private val wifiManager: WifiManager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    //TODO : 버튼누르면 원하는 ssid값 3개 설정할수있게 해주세요
    val desiredSsids = arrayOf("SK_WiFiGIGA06FD","SK_WiFiGIGA1818","SK_WiFiGIGA2207")
    private val apRSSIRecord = desiredSsids.associateWith { mutableListOf<Int>() }

    var flag = true

    val wifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION && flag) {

                if (wifiManager.startScan()) {
                    val results = if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        null
                    } else {
                        wifiManager.scanResults
                    }



                    results?.let {
                        binding.resultTextView.text = buildString {
                            it.forEach {
                                append(it.SSID)
                                append(": ")
                                append(it.level)
                                append("\n")
                            }
                        }
                    }


                    results?.let {
                        val desiredAps = it.filter { scanResult ->
                            // Check if the SSID is in the list of desired SSIDs
                            desiredSsids.contains(scanResult.SSID)
                        }
                        if (desiredAps.isNotEmpty()) {
                            // Map each SSID to a list of RSSI values
                            recordRealTimeRssi(desiredAps)
                            val rssisMap = desiredSsids.associateWith { ssid ->
                                desiredAps.filter { it.SSID == ssid }.map { it.level }
                            }
                            binding.resultTextView.text = buildString {
                                rssisMap.forEach { (ssid, rssis) ->
                                    append("RSSI values for $ssid: $rssis\n")
                                }
                            }
                        }
                    }

                    } else {
                    Toast.makeText(applicationContext, "Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }


        }
    }

    /*
    private val wifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

        }
    }
    */

    // permissions
    private var permissionAccepted = false
    private var permissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.CHANGE_WIFI_STATE

    )

    companion object {
        private const val REQUEST_PERMISSIONS = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request permissions
        requestPermissions(permissions, REQUEST_PERMISSIONS)


        if(!Environment.isExternalStorageManager()){
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData((Uri.parse("package:"+applicationContext.packageName)));
            startActivityForResult(intent,123);
        }

        //registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))


        val filter = IntentFilter()
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiReceiver, filter)

        binding.scanButton.setOnClickListener {


        }
    }

    /*
    override fun onStop() {
        super.onStop()
        //unregisterReceiver(wifiReceiver)
    }
    */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionAccepted = if (requestCode == REQUEST_PERMISSIONS) {
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        } else {
            false
        }

        if (!permissionAccepted) {
            Toast.makeText(applicationContext, "Please allow app permissions.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun recordRealTimeRssi(desiredAps: List<ScanResult>) {
        // 현재 시간을 milliseconds로 얻기
        val currentTimeMillis = System.currentTimeMillis()
        var index = 1
        // 각 desiredAp의 RSSI 값을 기록
        desiredAps.forEach { scanResult ->
            val ssid = scanResult.SSID
            val rssi = scanResult.level
            val targetRecord = apRSSIRecord.get(ssid)
            if(targetRecord!=null && targetRecord.size<100)
                apRSSIRecord.get(ssid)?.add(rssi)
        }
        var count = 0
        if(apRSSIRecord.get(desiredSsids[0])!!.size == 100 &&
            apRSSIRecord.get(desiredSsids[1])!!.size == 100 &&
            apRSSIRecord.get(desiredSsids[2])!!.size == 100){
            if (saveExcel()) {
                runBlocking {
                    saveSensorData()
                }
            }
        }


    }

    private fun saveExcel(): Boolean {
        if (!isExternalStorageWritable()) {
            return false
        }

        val workbook = HSSFWorkbook()

        with(workbook.createSheet("rssi")) {
            var row = this.createRow(0)
            row.createCell(0).setCellValue("Time (s)")
            row.createCell(1).setCellValue(desiredSsids[0])
            row.createCell(2).setCellValue(desiredSsids[1])
            row.createCell(3).setCellValue(desiredSsids[2])

            for (i in 0..99) {
                row = this.createRow(i + 1)
                row.createCell(0).setCellValue(0.0)
                for (j in 0..2) {
                    var targetSSID = desiredSsids[j]
                    var value = apRSSIRecord.get(targetSSID)!!.get(i)
                    row.createCell(j + 1).setCellValue(value.toDouble())
                }
            }
        }
        val sdCard = Environment.getExternalStorageDirectory()
        val dir = sdCard.absolutePath + "/Indoor Positioning System"
        if (!File(dir).exists()) {
            File(dir).mkdirs()
        }
        val excelFile = File(dir, "rssi_data.xls")
        return try {
            workbook.write(FileOutputStream(excelFile))
            workbook.close()
            flag = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Failed to save the data.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private suspend fun saveSensorData() {
        val path = Environment.getExternalStorageDirectory().absolutePath +
                "/Indoor Positioning System/rssi_data.xls";
        val file = File(
            Environment.getExternalStorageDirectory().absolutePath +
                    "/Indoor Positioning System/rssi_data.xls"
        )

        // 1st parameter: MediaType 으로 보내는 파일의 타입을 정하는 것
        // 2nd parameter: File 에 해당
        val requestFile = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            file
        )

        // 1st parameter: 서버에서 받는 KEY 값
        // 2nd parameter: 파일 이름
        // 3rd parameter: RequestBody 에 해당
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            try {
                val response = retrofitService.sendSensorData(body)
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "The file is saved Successfully.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "Response Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isExternalStorageWritable()
            = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

}