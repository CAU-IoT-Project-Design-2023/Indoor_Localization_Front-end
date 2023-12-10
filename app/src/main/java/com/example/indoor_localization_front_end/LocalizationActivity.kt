package com.example.indoor_localization_front_end

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.indoor_localization_front_end.databinding.ActivityLocalizationBinding
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

class LocalizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocalizationBinding
    private lateinit var retrofitService: RetrofitInterface

    // Sensor 관리자 객체로 lazy 를 사용하여 실제로 사용할 때 초기화 됨.
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var isWorking: Boolean = false

    // 센서 이벤트를 처리하는 리스너
    private val eventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()

                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroData[0] += x
                        gyroData[1] += y
                        gyroData[2] += z
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        if (gyroData[0] == 0.0 && gyroData[1] == 0.0 && gyroData[2] == 0.0) {
                            return
                        }

                        binding.accelTextView.text = buildString {
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}\n")
                        }
                        accelDataList[0].add(x)
                        accelDataList[1].add(y)
                        accelDataList[2].add(z)

                        binding.gyroTextView.text = buildString {
                            append("x: ${gyroData[0]}\n")
                            append("y: ${gyroData[1]}\n")
                            append("z: ${gyroData[2]}\n")
                        }
                        for (i in gyroData.indices) {
                            gyroDataList[i].add(gyroData[i])
                            uncalAccelDataList[i].add(uncalData[i])
                            gyroData[i] = 0.0
                            uncalData[i] = 0.0
                        }
                    }
                    Sensor.TYPE_STEP_COUNTER ->{

                    }
                    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED ->{
                        var uncal_x = event.values[3].toDouble()
                        var uncal_y =  event.values[4].toDouble()
                        var uncal_z = event.values[5].toDouble()
                        uncalData[0] += uncal_x
                        uncalData[1] += uncal_y
                        uncalData[2] += uncal_z
                    }
                    else -> {}
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val accelDataList = Array<MutableList<Double>>(3) { mutableListOf() }
    private val uncalAccelDataList = Array<MutableList<Double>>(3) { mutableListOf() }
    private val gyroDataList = Array<MutableList<Double>>(3) { mutableListOf() }
    private val gyroData = doubleArrayOf(0.0, 0.0, 0.0)
    private val uncalData = doubleArrayOf(0.0, 0.0, 0.0)
    private val step = 0;

    // permissions
    private var permissionAccepted = false
    private var permissions: Array<String> = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    companion object {
        private const val REQUEST_PERMISSIONS = 200
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalizationBinding.inflate(layoutInflater)
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

        if(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                !Environment.isExternalStorageManager()
            } else {
                TODO("VERSION.SDK_INT < R")
            }
        ){
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData((Uri.parse("package:"+applicationContext.packageName)));
            startActivityForResult(intent,123);
        }


        binding.sensorButton.setOnClickListener {
            // 센서의 변화 값을 처리할 리스너를 등록.
            if (!isWorking) {
                binding.localizationButton.isEnabled = false
                binding.sensorButton.text = "Stop tracking"
                binding.resultText.text = "result: ?"

                for (i in accelDataList.indices) {
                    accelDataList[i].clear()
                    gyroDataList[i].clear()
                    uncalAccelDataList[i].clear()
                    gyroData[i] = 0.0
                }

                val sensors = arrayOf(
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_LINEAR_ACCELERATION,
                    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED
                )

                for (sensor in sensors) {
                    sensorManager.registerListener(
                        eventListener,
                        sensorManager.getDefaultSensor(sensor),
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                }
            } else {
                sensorManager.unregisterListener(eventListener)
                binding.sensorButton.text = "Start tracking"
                if (saveExcel()) {
                    runBlocking {
                        saveSensorData()
                    }
                }
            }
            isWorking = !isWorking
        }

        binding.localizationButton.setOnClickListener {
            runBlocking {
                doLocalization()
                binding.sensorButton.isEnabled = true
                binding.localizationButton.isEnabled = false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            sensorManager.unregisterListener(eventListener)
            isWorking = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private suspend fun saveSensorData() {
        val path = Environment.getExternalStorageDirectory().absolutePath +
                "/Indoor Positioning System/sensor_data.xls";
        val file = File(
            Environment.getExternalStorageDirectory().absolutePath +
                    "/Indoor Positioning System/sensor_data.xls"
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
                        binding.sensorButton.isEnabled = false
                        binding.localizationButton.isEnabled = true
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

    private suspend fun doLocalization() {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            try {
                val response = retrofitService.doIndoorLocalization()
                if (response.isSuccessful) {
                    val result = response.body()
                    val resultStr = """
                        result: ${result?.result},
                        resultX: ${result?.resultX},
                        resultY: ${result?.resultY},
                        resultZ: ${result?.resultZ}
                    """.trimIndent()

                    Handler(Looper.getMainLooper()).post {
                        binding.resultText.text = resultStr
                        Toast.makeText(applicationContext, resultStr, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "Response Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "Localization Error", Toast.LENGTH_SHORT).show()
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

    // Check if a volume containing external storage is available for read and write.
    private fun isExternalStorageWritable()
        = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}