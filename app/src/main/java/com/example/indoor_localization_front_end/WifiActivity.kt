package com.example.indoor_localization_front_end

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.indoor_localization_front_end.databinding.ActivityWifiBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitInterface
import com.example.indoor_localization_front_end.retrofit_utils.RssiData
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class WifiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWifiBinding
    private lateinit var retrofitService: RetrofitInterface

    private val wifiManager: WifiManager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
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
                            val info = wifiManager.connectionInfo
                            append("Current Network ==> ")
                            append("SSID: ${info.ssid}\n")
                            append("BSSID: ${info.bssid}\n")
                            append("RSSI: ${info.rssi}\n\n")

                            it.forEach {
                                append("SSID: ${it.SSID}\n")
                                append("BSSID: ${it.BSSID}\n")
                                append("RSSI: ${it.level}\n\n")

                                if (it.SSID in arrayOf("IoTHotspot1", "IoTHotspot2", "IoTHotspot3")) {
                                    apData[it.SSID] = it.level
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val apData = mutableMapOf<String, Int>()
    private val apRSSIRecord = mutableMapOf<String, MutableList<Int>>()

    // permissions
    private var permissionAccepted = false
    private var permissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.CHANGE_WIFI_STATE
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra("url")
        retrofitService = if (url == null) {
            RetrofitClient.getApiService()
        } else {
            Toast.makeText(applicationContext, "$url", Toast.LENGTH_SHORT).show()
            RetrofitClient.getApiService2(url)
        }

        // request permissions
        requestPermissions(permissions, REQUEST_PERMISSIONS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(!Environment.isExternalStorageManager()){
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = (Uri.parse("package:" + applicationContext.packageName))
                startActivityForResult(intent,123)
            }
        }

        // TODO: registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        binding.scanButton.setOnClickListener {
            if (wifiManager.startScan()) {
                val results = if (ActivityCompat.checkSelfPermission(
                        this,
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
                        val info = wifiManager.connectionInfo
                        append("Current Network ==> ")
                        append("SSID: ${info.ssid}\n")
                        append("BSSID: ${info.bssid}\n")
                        append("RSSI: ${info.rssi}\n\n")

                        it.forEach {
                            append("SSID: ${it.SSID}\n")
                            append("BSSID: ${it.BSSID}\n")
                            append("RSSI: ${it.level}\n\n")

                            if (it.SSID in arrayOf("IoTHotspot1", "IoTHotspot2", "IoTHotspot3")) {
                                apData[it.SSID] = it.level
                            }
                        }
                    }
                }

                binding.localizationButton.isEnabled = true
            } else {
                Toast.makeText(applicationContext, "Please try again later.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.localizationButton.setOnClickListener {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_section, null)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    binding.localizationButton.isEnabled = false

                    try {
                        val section = dialogView.findViewById<EditText>(R.id.sectionEditText).text.toString().toInt()

                        val r1 = apData["IoTHotspot1"] ?: 0
                        val r2 = apData["IotHotspot2"] ?: 0
                        val r3 = apData["IoTHotspot3"] ?: 0

                        if (r1 == 0 || r2 == 0 || r3 == 0) {
                            Toast.makeText(applicationContext, "Some rssi values are missing.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        val rssiData = RssiData("IoTHotspot1", r1,
                            "IoTHotspot2", r2,
                            "IoTHotspot3", r3,
                            section
                        )

                        retrofitService.saveRssiAndSectionData(rssiData).enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                if (response.isSuccessful) {
                                    Toast.makeText(applicationContext, "The section data is saved successfully.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(applicationContext, "Response Error", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<String>, t: Throwable) {
                                t.printStackTrace()
                                Toast.makeText(applicationContext, "Connection Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Wrong Section Number", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }
    }

    override fun onStop() {
        super.onStop()
        // TODO: unregisterReceiver(wifiReceiver)
    }

    private fun saveExcel(): Boolean {
        if (!isExternalStorageWritable()) {
            return false
        }

        val workbook = HSSFWorkbook()

        with(workbook.createSheet("Linear Accelerometer")) {
            var row = this.createRow(0)
            row.createCell(0).setCellValue("Time (s)")
            row.createCell(1).setCellValue("X (m/s^2)")
            row.createCell(2).setCellValue("Y (m/s^2)")
            row.createCell(3).setCellValue("Z (m/s^2)")

            for (i in accelDataList[0].indices) {
                row = this.createRow(i + 1)
                row.createCell(0).setCellValue(0.0)
                for (j in accelDataList.indices) {
                    row.createCell(j + 1).setCellValue(accelDataList[j][i])
                }
            }
        }

        with(workbook.createSheet("Gyroscope")) {
            var row = this.createRow(0)
            row.createCell(0).setCellValue("Time (s)")
            row.createCell(1).setCellValue("X (rad/s)")
            row.createCell(2).setCellValue("Y (rad/s)")
            row.createCell(3).setCellValue("Z (rad/s)")

            for (i in gyroDataList[0].indices) {
                row = this.createRow(i + 1)
                row.createCell(0).setCellValue(0.0)
                for (j in gyroDataList.indices) {
                    row.createCell(j + 1).setCellValue(gyroDataList[j][i])
                }
            }
        }
        with(workbook.createSheet("uncal_Accelo")) {
            var row = this.createRow(0)
            row.createCell(0).setCellValue("Time (s)")
            row.createCell(1).setCellValue("X (rad/s)")
            row.createCell(2).setCellValue("Y (rad/s)")
            row.createCell(3).setCellValue("Z (rad/s)")

            for (i in uncalAccelDataList[0].indices) {
                row = this.createRow(i + 1)
                row.createCell(0).setCellValue(0.0)
                for (j in uncalAccelDataList.indices) {
                    row.createCell(j + 1).setCellValue(uncalAccelDataList[j][i])
                }
            }
        }

        val sdCard = Environment.getExternalStorageDirectory()
        val dir = sdCard.absolutePath + "/Indoor Positioning System"
        if (!File(dir).exists()) {
            File(dir).mkdirs()
        }
        val excelFile = File(dir, "sensor_data.xls")

        return try {
            workbook.write(FileOutputStream(excelFile))
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Failed to save the data.", Toast.LENGTH_SHORT).show()
            false
        }
    }

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

    private fun isExternalStorageWritable()
            = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}