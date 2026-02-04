package com.roklab.btcface.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roklab.btcface.R
import com.roklab.btcface.sync.BTCPriceFetcher
import com.roklab.btcface.sync.DataLayerSender
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var priceTextView: TextView
    private lateinit var lastUpdateTextView: TextView
    private lateinit var syncButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.tv_btc_price)
        lastUpdateTextView = findViewById(R.id.tv_last_update)
        syncButton = findViewById(R.id.btn_sync_now)
        settingsButton = findViewById(R.id.btn_settings)

        syncButton.setOnClickListener { syncNow() }
        settingsButton.setOnClickListener { 
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        displayPrice()
    }

    override fun onResume() {
        super.onResume()
        displayPrice()
    }

    private fun displayPrice() {
        val cached = BTCPriceFetcher.getCachedPrice(this)
        if (cached != null) {
            priceTextView.text = cached.priceUsdFormatted
            val time = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                .format(Date(cached.timestamp))
            lastUpdateTextView.text = "Last update: $time"
        } else {
            priceTextView.text = "Loading..."
            lastUpdateTextView.text = ""
        }
    }

    private fun syncNow() {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncButton.text = "Syncing..."
            
            val price = BTCPriceFetcher.fetchPrice()
            if (price != null) {
                BTCPriceFetcher.cachePrice(this@MainActivity, price)
                DataLayerSender.sendPriceToWatch(this@MainActivity, price)
                displayPrice()
            }
            
            syncButton.isEnabled = true
            syncButton.text = "Sync Now"
        }
    }
}
