package com.example.indoor_localization_front_end

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.indoor_localization_front_end.databinding.ActivityMainBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // Sensor 관리자 객체로 lazy 를 사용하여 실제로 사용할 때 초기화 됨.
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val wifiManager: WifiManager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // Requesting permissions
    private var permissionsAccepted = false
    private var permissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    companion object {
        private const val REQUEST_PERMISSIONS = 200
    }

    //private var isWorking: Boolean = false

    //private val retrofitService = RetrofitClient.getApiService()

    /*
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
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}")
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        binding.textView2.text = buildString {
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}")
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        binding.textView3.text = buildString {
                            append("x: ${x}\n")
                            append("y: ${y}\n")
                            append("z: ${z}")
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    */

    private val wifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //val APList = wifiManager.scanResults
            binding.textView1.text = "RSSI: ${wifiManager.connectionInfo.rssi}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions(permissions, REQUEST_PERMISSIONS)

        binding.button.setOnClickListener {
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
            wifiManager.startScan()
            /*
            // 센서의 변화 값을 처리할 리스너를 등록.
            if (!isWorking) {
                val sensors = arrayOf(
                    Sensor.TYPE_LINEAR_ACCELERATION,
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_MAGNETIC_FIELD
                )

                for (sensor in sensors) {
                    sensorManager.registerListener(
                        eventListener,
                        sensorManager.getDefaultSensor(sensor),
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            } else {
                sensorManager.unregisterListener(eventListener)
            }
            isWorking = !isWorking
            */
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(wifiReceiver)
        /*
        try {
            sensorManager.unregisterListener(eventListener)
            isWorking = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsAccepted = if (requestCode == REQUEST_PERMISSIONS) {
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        } else {
            false
        }

        if (!permissionsAccepted) {
            Toast.makeText(applicationContext, "앱 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}