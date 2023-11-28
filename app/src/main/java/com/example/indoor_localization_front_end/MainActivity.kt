package com.example.indoor_localization_front_end

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.indoor_localization_front_end.databinding.ActivityMainBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // Sensor 관리자 객체로 lazy 를 사용하여 실제로 사용할 때 초기화 됨.
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var isWorking: Boolean = false

    //private val retrofitService = RetrofitClient.getApiService()

    // 센서 이벤트를 처리하는 리스너
    private val eventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (event.sensor.type != Sensor.TYPE_GRAVITY) return@let

                binding.textView1.text = event.values[0].toDouble().toString()
                binding.textView2.text = event.values[1].toDouble().toString()
                binding.textView3.text = event.values[2].toDouble().toString()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            // 센서의 변화 값을 처리할 리스너를 등록.
            if (!isWorking) {
                sensorManager.registerListener(
                    eventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                sensorManager.unregisterListener(eventListener)
            }
            isWorking = !isWorking
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            sensorManager.unregisterListener(eventListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}