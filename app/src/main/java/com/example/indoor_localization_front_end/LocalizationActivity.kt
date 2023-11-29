package com.example.indoor_localization_front_end

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.indoor_localization_front_end.databinding.ActivityLocalizationBinding

class LocalizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocalizationBinding
    //private val retrofitService = RetrofitClient.getApiService()

    // Sensor 관리자 객체로 lazy 를 사용하여 실제로 사용할 때 초기화 됨.
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var isWorking: Boolean = false

    private var num: Long = 0L

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
                            append("z: ${z}\n")
                            append("num: ${num}")
                            num += 1L
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
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
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                }
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
            isWorking = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}