package com.gravassist

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.gravassist.api.LinkFetcherService
import com.gravassist.databinding.ActivityMainBinding
import com.gravassist.services.LoggerService
import com.gravassist.ui.adb.AdbControllerFragment
import com.gravassist.ui.google.GoogleAuthFragment
import com.gravassist.ui.home.HomeFragment
import com.gravassist.ui.logs.LogsFragment
import com.gravassist.ui.onboarding.ApiKeyActivity
import com.gravassist.ui.profile.ProfileFragment
import com.gravassist.ui.settings.SettingsFragment
import com.gravassist.ui.usage.UsageFragment
import com.gravassist.utils.PermissionManager
import com.gravassist.utils.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)
        permissionManager = PermissionManager(this)

        // Check if API key exists, otherwise launch onboarding
        if (!prefManager.hasApiKey()) {
            startActivity(Intent(this, ApiKeyActivity::class.java))
            finish()
            return
        }

        // Request runtime permissions if missing
        if (!permissionManager.hasAllPermissions()) {
            permissionManager.requestPermissions(this)
        }

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.nav_home, R.string.nav_home
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), "Home")
            binding.navView.setCheckedItem(R.id.nav_home)
        }

        // Auto-fetch Cloudflare Tunnel URL on startup
        fetchCloudflareTunnel()
    }

    private fun fetchCloudflareTunnel() {
        lifecycleScope.launch {
            val fetchedUrl = LinkFetcherService.fetchCloudflareLink()
            val headerStatusTv = binding.navView.getHeaderView(0).findViewById<TextView>(R.id.tv_header_status)

            if (!fetchedUrl.isNullOrEmpty()) {
                prefManager.saveCachedTunnelUrl(fetchedUrl)
                headerStatusTv?.text = "Tunnel Active: $fetchedUrl"
                LoggerService.log("MainActivity", "Tunnel ready: $fetchedUrl")
            } else {
                val cached = prefManager.getCachedTunnelUrl()
                if (cached.isNotEmpty()) {
                    headerStatusTv?.text = "Using Cached: $cached"
                } else {
                    headerStatusTv?.text = "Tunnel Fetch Failed"
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> loadFragment(HomeFragment(), getString(R.string.nav_home))
            R.id.nav_profile -> loadFragment(ProfileFragment(), getString(R.string.nav_profile))
            R.id.nav_settings -> loadFragment(SettingsFragment(), getString(R.string.nav_settings))
            R.id.nav_usage -> loadFragment(UsageFragment(), getString(R.string.nav_usage))
            R.id.nav_google -> loadFragment(GoogleAuthFragment(), getString(R.string.nav_google))
            R.id.nav_adb -> loadFragment(AdbControllerFragment(), getString(R.string.nav_adb))
            R.id.nav_logs -> loadFragment(LogsFragment(), getString(R.string.nav_logs))
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        binding.toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
