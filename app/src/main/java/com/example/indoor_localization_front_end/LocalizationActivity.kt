package com.example.indoor_localization_front_end

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.example.indoor_localization_front_end.databinding.ActivityLocalizationBinding
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class LocalizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocalizationBinding
    //private val retrofitService = RetrofitClient.getApiService()

    // Sensor 관리자 객체로 lazy 를 사용하여 실제로 사용할 때 초기화 됨.
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var isWorking: Boolean = false

    private var num1 = 0L
    private var num2 = 0L

    // 센서 이벤트를 처리하는 리스너
    private val eventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()

                when (event.sensor.type) {
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        binding.textView1.text = buildString {
                            num1++

                            append("${event.timestamp}\n")
                            append("${num1}\n")
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}")

                            accelDataList[0].add(x)
                            accelDataList[1].add(y)
                            accelDataList[2].add(z)
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        binding.textView2.text = buildString {
                            num2++
                            append("${num2}\n")
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}")

                            gyroDataList[0].add(x)
                            gyroDataList[1].add(y)
                            gyroDataList[2].add(z)
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val accelDataList = Array<MutableList<Double>>(3) { mutableListOf() }
    private val gyroDataList = Array<MutableList<Double>>(3) { mutableListOf() }

    // permissions
    private var permissionAccepted = false
    private var permissions: Array<String> = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    companion object {
        private const val REQUEST_PERMISSIONS = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request permissions
        requestPermissions(permissions, REQUEST_PERMISSIONS)

        binding.button.setOnClickListener {
            // 센서의 변화 값을 처리할 리스너를 등록.
            if (!isWorking) {
                for (i in accelDataList.indices) {
                    accelDataList[i].clear()
                    gyroDataList[i].clear()
                }
                num1 = 0L
                num2 = 0L

                val sensors = arrayOf(
                    Sensor.TYPE_LINEAR_ACCELERATION,
                    Sensor.TYPE_GYROSCOPE
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
                saveExcel()
            }
            isWorking = !isWorking
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

    private fun saveExcel() {
        if (!isExternalStorageWritable()) {
            return
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

        val sdCard = Environment.getExternalStorageDirectory()
        val dir = sdCard.absolutePath + "/Indoor Positioning System"
        if (!File(dir).exists()) {
            File(dir).mkdirs()
        }
        val excelFile = File(dir, "sensor_data.xls")
        try {
            workbook.write(FileOutputStream(excelFile))
            workbook.close()
            Toast.makeText(applicationContext, "The data is saved successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Failed to save the data.", Toast.LENGTH_SHORT).show()
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