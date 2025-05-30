package com.example.winampinspiredmp3player.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.winampinspiredmp3player.R // Import R class for drawable resources
import com.example.winampinspiredmp3player.databinding.FragmentPlayerBinding
import com.example.winampinspiredmp3player.services.MusicService
import com.example.winampinspiredmp3player.data.Track

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicService? = null
    private var isBound: Boolean = false
    private lateinit var audioManager: AudioManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("PlayerFragment", "Service Connected")
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            setupVolumeControls()
            updatePlayPauseButtonState()
            observeLiveData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("PlayerFragment", "Service Disconnected")
            musicService?.playbackPosition?.removeObservers(viewLifecycleOwner)
            musicService?.currentTrackDuration?.removeObservers(viewLifecycleOwner)
            musicService?.currentPlayingTrack?.removeObservers(viewLifecycleOwner)
            musicService = null
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupTrackProgressSeekBar()
        setupVolumeControls()
    }

    private fun setupClickListeners() {
        binding.btnPlayPause.setOnClickListener {
            if (isBound && musicService != null) {
                if (musicService!!.isPlaying()) {
                    musicService!!.pauseTrack()
                } else {
                    if (musicService!!.currentTrack != null) {
                        musicService!!.playTrackAtIndex(musicService!!.currentTrackIndex)
                    } else {
                        Log.d("PlayerFragment", "Play clicked, but no track is loaded/selected.")
                        // Optionally play first track from list if available
                        // musicService!!.playTrackAtIndex(0)
                    }
                }
                updatePlayPauseButtonState() // Update image after action
            } else {
                Log.d("PlayerFragment", "Play/Pause button clicked, but service not bound.")
            }
        }

        binding.btnStopTrack.setOnClickListener {
            if (isBound && musicService != null) {
                musicService!!.stopTrack()
                // UI updates for track info and play/pause button will be handled by LiveData observers
            } else {
                Log.d("PlayerFragment", "Stop button clicked, but service not bound.")
            }
        }

        binding.btnPreviousTrack.setOnClickListener {
            if (isBound && musicService != null) {
                musicService!!.playPreviousTrack()
            } else {
                Log.d("PlayerFragment", "Previous Track Clicked, but service not bound.")
            }
        }
        binding.btnNextTrack.setOnClickListener {
            if (isBound && musicService != null) {
                musicService!!.playNextTrack()
            } else {
                Log.d("PlayerFragment", "Next Track Clicked, but service not bound.")
            }
        }

        binding.btnEject.setOnClickListener {
            // For now, just a Toast message.
            // In a future step, this could navigate to PlaylistFragment and trigger a scan,
            // or trigger a scan via a shared ViewModel or service event.
            Toast.makeText(requireContext(), "Eject button clicked - Connect to Scan for Music", Toast.LENGTH_LONG).show()
            Log.d("PlayerFragment", "Eject button clicked.")
        }
    }

    private fun setupTrackProgressSeekBar() {
        binding.sbTrackProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Optionally update a TextView with current progress / duration
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    if (isBound && musicService != null) {
                        musicService!!.seekTo(it.progress)
                    }
                }
            }
        })
    }


    private fun setupVolumeControls() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.sbVolumeControl.max = maxVolume
        binding.sbVolumeControl.progress = currentVolume

        binding.sbVolumeControl.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updatePlayPauseButtonState() {
        if (isBound && musicService != null) {
            if (musicService!!.isPlaying()) {
                binding.btnPlayPause.setImageResource(R.drawable.winamp_btn_pause)
            } else {
                binding.btnPlayPause.setImageResource(R.drawable.winamp_btn_play)
            }
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.winamp_btn_play) // Default state
        }
    }

    private fun observeLiveData() {
        musicService?.playbackPosition?.observe(viewLifecycleOwner) { position ->
            binding.sbTrackProgress.progress = position
        }
        musicService?.currentTrackDuration?.observe(viewLifecycleOwner) { duration ->
            binding.sbTrackProgress.max = duration
        }
        musicService?.currentPlayingTrack?.observe(viewLifecycleOwner) { track ->
            updateTrackInfoUI(track)
            updatePlayPauseButtonState() // Update play/pause button when track changes or stops
        }
    }

    private fun updateTrackInfoUI(track: Track?) {
        if (track != null) {
            val titleToDisplay = if (track.title.isNullOrBlank()) track.fileName else track.title
            val artistToDisplay = if (track.artist.isNullOrBlank()) "<Unknown Artist>" else track.artist
            binding.tvCurrentTrackInfo.text = "$titleToDisplay - $artistToDisplay"
            // Duration is handled by sb_track_progress.max and sb_track_progress.progress via LiveData
        } else {
            binding.tvCurrentTrackInfo.text = "No track playing"
            binding.sbTrackProgress.progress = 0
            binding.sbTrackProgress.max = 100 // Default max when no track
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("PlayerFragment", "onStart called, binding to service.")
        Intent(requireActivity(), MusicService::class.java).also { intent ->
            requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("PlayerFragment", "onStop called, unbinding from service.")
        if (isBound) {
            requireActivity().unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound && musicService != null) {
            musicService?.playbackPosition?.removeObservers(viewLifecycleOwner)
            musicService?.currentTrackDuration?.removeObservers(viewLifecycleOwner)
            musicService?.currentPlayingTrack?.removeObservers(viewLifecycleOwner)
        }
        _binding = null
        Log.d("PlayerFragment", "onDestroyView called, _binding set to null")
    }
}
