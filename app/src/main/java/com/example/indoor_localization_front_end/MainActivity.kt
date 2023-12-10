package com.example.indoor_localization_front_end

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = getSharedPreferences("ip_address", Activity.MODE_PRIVATE)
        editor = pref.edit()

        binding.startButton.setOnClickListener {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(this)

            val dialogView = layoutInflater.inflate(R.layout.dialog_ip, null)
            dialogView.findViewById<EditText>(R.id.addressEditText).setText(pref.getString("url", ""))

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
                                    editor.putString("url", url).apply()
                                    val intent = Intent(this@MainActivity, LocalizationActivity::class.java)
                                    intent.putExtra("url", url)
                                    startActivity(intent)
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
        binding.startButton2.setOnClickListener {
            startActivity(Intent(this, LocalizationActivity::class.java))
        }

        binding.startButton3.setOnClickListener {

            val builder = AlertDialog.Builder(this)

            val dialogView = layoutInflater.inflate(R.layout.dialog_ip, null)

            dialogView.findViewById<EditText>(R.id.addressEditText).setText(pref.getString("url", ""))
            builder.setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val url = dialogView.findViewById<EditText>(R.id.addressEditText).text.toString()
                    try {
                        val retrofitService = RetrofitClient.getApiService2(url)
                        retrofitService.isConnected().enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                if (response.isSuccessful) {
                                    editor.putString("url", url).apply()
                                    val intent = Intent(this@MainActivity, WifiActivity::class.java)
                                    intent.putExtra("url", url)
                                    startActivity(intent)
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
    }
}