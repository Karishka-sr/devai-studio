package com.devai.studio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.devai.studio.databinding.ActivityMainBinding
import com.devai.studio.ui.BuilderFragment
import com.devai.studio.ui.ChatFragment
import com.devai.studio.ui.ProvidersFragment
import com.devai.studio.ui.SettingsFragment
import com.devai.studio.ui.WorkspaceFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            show(ChatFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> show(ChatFragment())
                R.id.nav_providers -> show(ProvidersFragment())
                R.id.nav_builder -> show(BuilderFragment())
                R.id.nav_projects -> show(WorkspaceFragment())
                R.id.nav_settings -> show(SettingsFragment())
            }
            true
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}