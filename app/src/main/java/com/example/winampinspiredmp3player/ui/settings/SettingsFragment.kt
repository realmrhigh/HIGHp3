package com.example.winampinspiredmp3player.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.winampinspiredmp3player.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "AppSettings"
        private const val GLOW_EFFECT_KEY = "glow_effect_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load current preference
        val isGlowEnabled = isGlowEffectEnabled()
        binding.switchGlow.isChecked = isGlowEnabled
        
        binding.switchGlow.setOnCheckedChangeListener { _, isChecked ->
            setGlowEffectEnabled(isChecked)
            Log.d("SettingsFragment", "Glow effect toggled: $isChecked")
            
            // Notify any observers that settings changed
            // This will be implemented in PlayerFragment to react to setting changes
        }
    }

    fun isGlowEffectEnabled(): Boolean {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(GLOW_EFFECT_KEY, true) // Default enabled
    }

    fun setGlowEffectEnabled(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(GLOW_EFFECT_KEY, enabled).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
