package com.hotwheels.identifier.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivitySettingsBinding
import com.hotwheels.identifier.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private val tag = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
        updateCurrentLanguageDisplay()
        initializeAds()
    }

    private fun initializeAds() {
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error initializing ads", e)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.title_settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnResetCollection.setOnClickListener {
            showResetCollectionDialog()
        }

        binding.languageSettingsLayout.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun showResetCollectionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_dialog_title)
            .setMessage(R.string.reset_dialog_message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.reset_dialog_confirm) { _, _ ->
                Log.d(tag, "User confirmed collection reset")
                viewModel.resetCollection()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_spanish),
            getString(R.string.language_chinese)
        )
        val languageCodes = arrayOf("en", "es", "zh")

        val currentLang = getCurrentLanguage()
        val selectedIndex = languageCodes.indexOf(currentLang)

        AlertDialog.Builder(this)
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                if (selectedLanguage != currentLang) {
                    setLanguage(selectedLanguage)

                    // Show message
                    val languageName = languages[which]
                    Snackbar.make(binding.root,
                        getString(R.string.language_changed, languageName),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Restart app to apply language to all activities
                    android.os.Handler(mainLooper).postDelayed({
                        restartApp()
                    }, 500)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun setLanguage(languageCode: String) {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("language", languageCode).apply()

        Log.d(tag, "Language set to: $languageCode")

        // Update locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getCurrentLanguage(): String {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", null)

        // If no language is set, use system language
        if (lang == null) {
            val systemLang = Locale.getDefault().language
            val defaultLang = when {
                systemLang.startsWith("zh") -> "zh"  // Chinese
                systemLang.startsWith("es") -> "es"  // Spanish
                else -> "en"  // Default to English
            }
            prefs.edit().putString("language", defaultLang).apply()
            Log.d(tag, "Initializing language to system default: $defaultLang (system: $systemLang)")
            return defaultLang
        }

        Log.d(tag, "Current language: $lang")
        return lang
    }

    private fun updateCurrentLanguageDisplay() {
        val currentLang = getCurrentLanguage()
        val languageName = when (currentLang) {
            "es" -> getString(R.string.language_spanish)
            "zh" -> getString(R.string.language_chinese)
            else -> getString(R.string.language_english)
        }
        binding.tvCurrentLanguage.text = languageName
        Log.d(tag, "Language display updated: $languageName ($currentLang)")
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
