package com.example.winampinspiredmp3player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.winampinspiredmp3player.ui.ViewPagerAdapter
import com.example.winampinspiredmp3player.services.MusicService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.winampinspiredmp3player.databinding.ActivityMainBinding // Import ViewBinding class

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Declare binding variable
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Initialize binding
        setContentView(binding.root) // Set content view to binding.root

        // Start the MusicService to ensure it stays alive in background
        startService(Intent(this, MusicService::class.java))

        // val viewPager: ViewPager2 = findViewById(R.id.view_pager) // Original
        // val tabLayout: TabLayout = findViewById(R.id.tab_layout) // Original
        // Use binding to access views
        val viewPager: ViewPager2 = binding.viewPager
        val tabLayout: TabLayout = binding.tabLayout


        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Player"
                1 -> "Playlist"
                2 -> "Visualizer"
                else -> null
            }
        }.attach()

        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Public method to switch tabs
    fun switchToPlayerTab() {
        binding.viewPager.currentItem = 0 // Assuming 0 is the index for PlayerFragment
    }
}
