package com.roklab.btcface.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.roklab.btcface.R
import com.roklab.btcface.worker.BTCPriceSyncWorker

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootPreferenceKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootPreferenceKey)
            
            // Listen for sync interval changes
            findPreference<androidx.preference.ListPreference>("sync_interval")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val intervalMin = (newValue as String).toIntOrNull() 
                        ?: BTCPriceSyncWorker.DEFAULT_SYNC_INTERVAL_MIN
                    
                    // Reschedule with new interval
                    BTCPriceSyncWorker.cancel(requireContext())
                    BTCPriceSyncWorker.schedule(requireContext(), intervalMin)
                    true
                }
            }
        }
    }
}
