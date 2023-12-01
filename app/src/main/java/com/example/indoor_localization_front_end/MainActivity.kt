package com.example.indoor_localization_front_end

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.indoor_localization_front_end.databinding.ActivityMainBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //private val retrofitService = RetrofitClient.getApiService()

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
                    val retrofitService = RetrofitClient.getApiService2(url)

                    retrofitService.isConnected().enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful) {
                                val result = response.body()
                                if (result == "connected") {
                                    startActivity(Intent(this@MainActivity, LocalizationActivity::class.java))
                                }
                            } else {
                                Toast.makeText(applicationContext, "Connection error", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            t.printStackTrace()
                            Toast.makeText(applicationContext, "Failed connection", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.create().show()
        }
    }
}