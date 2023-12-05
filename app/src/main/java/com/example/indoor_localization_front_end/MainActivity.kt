package com.example.indoor_localization_front_end

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.indoor_localization_front_end.databinding.ActivityMainBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(this)

            val dialogView = layoutInflater.inflate(R.layout.dialog_ip, null)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val url = dialogView.findViewById<EditText>(R.id.addressEditText).text.toString()

                    try {
                        val retrofitService = RetrofitClient.getApiService2(url)
                        retrofitService.isConnected().enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                if (response.isSuccessful) {
                                    val result = response.body()
                                    if (result == "connected") {
                                        val intent = Intent(this@MainActivity, LocalizationActivity::class.java)
                                        intent.putExtra("url", url)
                                        startActivity(intent)
                                    }
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
                        Toast.makeText(applicationContext, "Wrong IP Address", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }

        // TODO: 삭제 필요?
        binding.button2.setOnClickListener {
            startActivity(Intent(this, LocalizationActivity::class.java))
        }
    }
}