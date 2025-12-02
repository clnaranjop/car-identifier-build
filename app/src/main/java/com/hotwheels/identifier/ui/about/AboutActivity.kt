package com.hotwheels.identifier.ui.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivityAboutBinding
import java.util.Locale

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val tag = "AboutActivity"

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    private fun updateLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "es") ?: "es"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupVersionInfo()
        setupClickListeners()
        initializeAds()
    }

    private fun initializeAds() {
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(tag, "Error initializing ads", e)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.title_about)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = getString(R.string.about_copyright).replace(
                "2025",
                "Versión ${packageInfo.versionName}"
            ).substringBefore(".")
            binding.tvVersion.text = "Versión ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(tag, "Error getting version info", e)
            binding.tvVersion.text = "Versión 2.0.0"
        }
    }

    private fun setupClickListeners() {
        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }

        binding.btnLicenses.setOnClickListener {
            showOpenSourceLicenses()
        }
    }

    private fun showPrivacyPolicy() {
        // TODO: Replace with actual privacy policy URL when published
        val privacyPolicyUrl = "https://your-website.com/privacy-policy"

        AlertDialog.Builder(this)
            .setTitle(R.string.about_privacy_policy)
            .setMessage(getString(R.string.legal_disclaimer_text))
            .setPositiveButton("OK", null)
            .setNeutralButton("Ver en Web") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(tag, "Error opening privacy policy", e)
                }
            }
            .show()
    }

    private fun showOpenSourceLicenses() {
        val licenses = """
            Esta aplicación usa las siguientes bibliotecas de código abierto:

            • AndroidX Libraries (Apache 2.0)
            • Kotlin Standard Library (Apache 2.0)
            • Material Components for Android (Apache 2.0)
            • CameraX (Apache 2.0)
            • OpenCV for Android (Apache 2.0)
            • TensorFlow Lite (Apache 2.0)
            • Google AdMob (Propietario)

            Apache License 2.0:
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.

            ${getString(R.string.legal_images_attribution)}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(R.string.about_licenses)
            .setMessage(licenses)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
