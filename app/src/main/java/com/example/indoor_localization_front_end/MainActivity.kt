package com.example.indoor_localization_front_end

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.indoor_localization_front_end.databinding.ActivityMainBinding
import com.example.indoor_localization_front_end.retrofit_utils.RetrofitClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //private val retrofitService = RetrofitClient.getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            IPAddressDialogFragment().show(supportFragmentManager, "IP Address Dialog")
        }
    }

    class IPAddressDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)

                // Get the layout inflater
                val inflater = requireActivity().layoutInflater

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                builder.setView(inflater.inflate(R.layout.dialog_ip, null))
                    .setPositiveButton("OK") { _, _ ->
                        // TODO
                        startActivity(Intent(activity, LocalizationActivity::class.java))
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        dialog?.cancel()
                    }

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null.")
        }
    }
}