package com.example.indoor_localization_front_end

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.indoor_localization_front_end.databinding.ActivityWifiBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient
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
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private val wifiManager: WifiManager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                doScan()

                if (rssiRecord[bssidList[0]]!!.size >= 100
                    && rssiRecord[bssidList[1]]!!.size >= 100
                    && rssiRecord[bssidList[2]]!!.size >= 100
                ) {
                    unregisterReceiver(this)
                    Toast.makeText(applicationContext, "Scanning is finished.", Toast.LENGTH_SHORT).show()
                    binding.scanButton.isEnabled = false
                    binding.sendButton.isEnabled = true
                    binding.localizationButton.isEnabled = false
                }
            }
        }
    }

    private val bssidList = mutableListOf<String>()
    private val rssiRecord = mutableMapOf<String, MutableList<Int>>()

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

        pref = getSharedPreferences("bssid", Activity.MODE_PRIVATE)
        editor = pref.edit()

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

        binding.scanButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_bssid, null)
            dialogView.findViewById<EditText>(R.id.bssidEditText1).setText(pref.getString("bssid1", ""))
            dialogView.findViewById<EditText>(R.id.bssidEditText2).setText(pref.getString("bssid2", ""))
            dialogView.findViewById<EditText>(R.id.bssidEditText3).setText(pref.getString("bssid3", ""))

            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val bssid1 = dialogView.findViewById<EditText>(R.id.bssidEditText1).text.toString()
                    val bssid2 = dialogView.findViewById<EditText>(R.id.bssidEditText2).text.toString()
                    val bssid3 = dialogView.findViewById<EditText>(R.id.bssidEditText3).text.toString()
                    editor.putString("bssid1", bssid1).apply()
                    editor.putString("bssid2", bssid2).apply()
                    editor.putString("bssid3", bssid3).apply()
                    bssidList.clear()
                    bssidList.add(bssid1)
                    bssidList.add(bssid2)
                    bssidList.add(bssid3)
                    rssiRecord.clear()
                    rssiRecord[bssid1] = mutableListOf()
                    rssiRecord[bssid2] = mutableListOf()
                    rssiRecord[bssid3] = mutableListOf()

                    binding.scanButton.isEnabled = false
                    binding.sendButton.isEnabled = false
                    binding.localizationButton.isEnabled = false

                    val filter = IntentFilter()
                    filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                    registerReceiver(wifiReceiver, filter)
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }

        binding.sendButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_section, null)

            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    try {
                        val section = dialogView.findViewById<EditText>(R.id.sectionEditText).toString().toInt()
                        if (saveExcel(section)) {
                            runBlocking {
                                saveSensorData(section)
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(application, "Wrong section number", Toast.LENGTH_SHORT).show()
                    }

                    binding.scanButton.isEnabled = true
                    binding.sendButton.isEnabled = false
                    binding.localizationButton.isEnabled = true
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }

        binding.localizationButton.setOnClickListener {
            // TODO

            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_bssid, null)
            dialogView.findViewById<EditText>(R.id.bssidEditText1).setText(pref.getString("bssid1", ""))
            dialogView.findViewById<EditText>(R.id.bssidEditText2).setText(pref.getString("bssid2", ""))
            dialogView.findViewById<EditText>(R.id.bssidEditText3).setText(pref.getString("bssid3", ""))

            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val bssid1 = dialogView.findViewById<EditText>(R.id.bssidEditText1).text.toString()
                    val bssid2 = dialogView.findViewById<EditText>(R.id.bssidEditText2).text.toString()
                    val bssid3 = dialogView.findViewById<EditText>(R.id.bssidEditText3).text.toString()
                    editor.putString("bssid1", bssid1).apply()
                    editor.putString("bssid2", bssid2).apply()
                    editor.putString("bssid3", bssid3).apply()
                    bssidList.clear()
                    bssidList.add(bssid1)
                    bssidList.add(bssid2)
                    bssidList.add(bssid3)

                    bssidList.clear()
                    bssidList.add(bssid1)
                    bssidList.add(bssid2)
                    bssidList.add(bssid3)
                    rssiRecord.clear()
                    rssiRecord[bssid1] = mutableListOf()
                    rssiRecord[bssid2] = mutableListOf()
                    rssiRecord[bssid3] = mutableListOf()
                    doScan()
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(wifiReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doScan() {
        if (wifiManager.startScan()) {
            val results = if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null
            } else {
                wifiManager.scanResults
            }

            results?.let {
                binding.resultTextView.text = buildString {
                    val info = wifiManager.connectionInfo
                    append("Current Network\n")
                    append("SSID: ${info.ssid}\n")
                    append("BSSID: ${info.bssid}\n")
                    append("RSSI: ${info.rssi}\n\n")

                    it.forEach {
                        append("SSID: ${it.SSID}\n")
                        append("BSSID: ${it.BSSID}\n")
                        append("RSSI: ${it.level}\n\n")

                        if (it.BSSID in bssidList) {
                            rssiRecord[it.BSSID]?.add(it.level)
                        }
                    }
                }
            }
        } else {
            Toast.makeText(applicationContext, "Please try again later.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExcel(section: Int): Boolean {
        if (!isExternalStorageWritable()) {
            return false
        }

        val workbook = HSSFWorkbook()

        with(workbook.createSheet("RSSI")) {
            var row = this.createRow(0)
            row.createCell(0).setCellValue("RSSI")
            row.createCell(1).setCellValue("X")
            row.createCell(2).setCellValue("Y")
            row.createCell(3).setCellValue("Z")

            for (i in 0..99) {
                row = this.createRow(i + 1)
                row.createCell(0).setCellValue(0.0)
                for (j in bssidList.indices) {
                    row.createCell(j + 1).setCellValue(rssiRecord[bssidList[j]]!![i].toDouble())
                }
            }
        }

        val sdCard = Environment.getExternalStorageDirectory()
        val dir = sdCard.absolutePath + "/Indoor Positioning System"
        if (!File(dir).exists()) {
            File(dir).mkdirs()
        }
        val excelFile = File(dir, "${section}.xls")

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

    private suspend fun saveSensorData(section: Int) {
        val file = File(
            Environment.getExternalStorageDirectory().absolutePath +
                    "/Indoor Positioning System/${section}.xls"
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