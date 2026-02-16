package com.example.winampinspiredmp3player.ui.visualizer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.winampinspiredmp3player.R
import com.example.winampinspiredmp3player.databinding.FragmentVisualizerBinding
import com.example.winampinspiredmp3player.services.MusicService
import kotlin.math.sqrt

class VisualizerFragment : Fragment() {

    private var _binding: FragmentVisualizerBinding? = null
    private val binding get() = _binding!!

    // Service related variables
    private var musicService: MusicService? = null
    private var isBound: Boolean = false
    private var videoIsPrepared: Boolean = false
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var smoothedBass: Float = 0f

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("VisualizerFragment", "Service Connected")
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            musicService?.isPlayingState?.observe(viewLifecycleOwner) { isPlaying ->
                Log.d("VisualizerFragment", "Music isPlayingState changed: $isPlaying, videoIsPrepared: $videoIsPrepared")
                if (videoIsPrepared) {
                    if (isPlaying) {
                        binding.videoViewVisualizer.start()
                    } else {
                        binding.videoViewVisualizer.pause()
                    }
                }
                updateGlowForPlaybackState(isPlaying)
            }

            musicService?.audioSessionId?.observe(viewLifecycleOwner) { sessionId ->
                if (sessionId != null) {
                    setupVisualizer(sessionId)
                }
            }

            // Immediately update video state based on current music state
            if (videoIsPrepared) {
                if (musicService?.isPlayingState?.value == true) {
                    binding.videoViewVisualizer.start()
                } else {
                    binding.videoViewVisualizer.pause()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("VisualizerFragment", "Service Disconnected")
            musicService?.isPlayingState?.removeObservers(viewLifecycleOwner)
            musicService?.audioSessionId?.removeObservers(viewLifecycleOwner)
            musicService = null
            isBound = false
            // Ensure video is paused if service disconnects
            if (binding.videoViewVisualizer.isPlaying) {
                binding.videoViewVisualizer.pause()
            }
            releaseVisualizer()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("VisualizerFragment", "onViewCreated")
        val videoUri = Uri.parse("android.resource://" + requireActivity().packageName + "/" + R.raw.visualization_loop)
        binding.videoViewVisualizer.setVideoURI(videoUri)

        binding.videoViewVisualizer.setOnPreparedListener { mediaPlayer ->
            Log.d("VisualizerFragment", "Video prepared.")
            videoIsPrepared = true
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f) // Mute the video

            // Start video only if music is already playing when video becomes prepared
            if (isBound && musicService?.isPlayingState?.value == true) {
                binding.videoViewVisualizer.start()
                Log.d("VisualizerFragment", "Video started on prepare because music is playing.")
            } else {
                Log.d("VisualizerFragment", "Video prepared, but music not playing or service not bound. Video will not start yet.")
            }
        }

        binding.videoViewVisualizer.setOnErrorListener { mp, what, extra ->
            Log.e("VisualizerFragment", "VideoView Error: what: $what, extra: $extra")
            videoIsPrepared = false
            true
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("VisualizerFragment", "onStart called, binding to service.")
        Intent(requireActivity(), MusicService::class.java).also { intent ->
            requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("VisualizerFragment", "onResume called. isBound: $isBound, musicPlaying: ${musicService?.isPlayingState?.value}, videoIsPrepared: $videoIsPrepared")
        // VideoView might have been paused or stopped.
        // Ensure it's playing if music is playing and video is prepared.
        if (isBound && musicService?.isPlayingState?.value == true && videoIsPrepared) {
            if (!binding.videoViewVisualizer.isPlaying) {
                binding.videoViewVisualizer.start()
                Log.d("VisualizerFragment", "Video explicitly started in onResume.")
            }
        } else if (videoIsPrepared && binding.videoViewVisualizer.isPlaying) {
            // If music is not playing, but video is, pause it.
            binding.videoViewVisualizer.pause()
            Log.d("VisualizerFragment", "Video explicitly paused in onResume as music is not playing.")
        }
        
        // Initialize visualizer only when this fragment becomes visible (onResume)
        if (isBound && musicService != null && visualizer == null) {
            val sessionId = musicService?.audioSessionId?.value ?: 0
            if (sessionId > 0) {
                setupVisualizer(sessionId)
                Log.d("VisualizerFragment", "Visualizer initialized in onResume with sessionId: $sessionId")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("VisualizerFragment", "onPause called.")
        // Release visualizer when fragment is not visible to save battery/resources
        releaseVisualizer()
        // It's generally good practice to pause video when fragment is not visible.
        // The LiveData observer will also pause it if music stops.
        if (binding.videoViewVisualizer.isPlaying) {
            binding.videoViewVisualizer.pause()
            Log.d("VisualizerFragment", "Video paused in onPause.")
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("VisualizerFragment", "onStop called, unbinding from service.")
        if (isBound) {
            musicService?.isPlayingState?.removeObservers(viewLifecycleOwner) // Clean up observer
            musicService?.audioSessionId?.removeObservers(viewLifecycleOwner)
            requireActivity().unbindService(serviceConnection)
            isBound = false
            musicService = null
        }
        releaseVisualizer()
        // Ensure video is paused when fragment stops and potentially service is unbound
        if (binding.videoViewVisualizer.isPlaying) {
            binding.videoViewVisualizer.pause()
            Log.d("VisualizerFragment", "Video paused in onStop as a fallback.")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("VisualizerFragment", "onDestroyView called.")
        if (binding.videoViewVisualizer.isPlaying) {
            binding.videoViewVisualizer.stopPlayback()
        }
        releaseVisualizer()
        videoIsPrepared = false
        _binding = null
        Log.d("VisualizerFragment", "VideoView playback stopped, _binding set to null")
    }

    private fun setupVisualizer(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) {
            return
        }
        releaseVisualizer()
        currentSessionId = sessionId

        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            visualizer = Visualizer(sessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || _binding == null) return
                            val bass = calculateBassIntensity(fft)
                            val target = (bass / 1800f).coerceIn(0f, 1f)
                            smoothedBass = smoothedBass * 0.8f + target * 0.2f
                            val scale = 1f + 0.25f * smoothedBass
                            val alpha = 0.35f + 0.55f * smoothedBass
                            binding.pulseGlow.post {
                                binding.pulseGlow.scaleX = scale
                                binding.pulseGlow.scaleY = scale
                                binding.pulseGlow.alpha = alpha
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = musicService?.isPlayingState?.value == true
            }
        } catch (e: Exception) {
            Log.e("VisualizerFragment", "Failed to init Visualizer", e)
            currentSessionId = 0
        }
    }

    private fun releaseVisualizer() {
        visualizer?.release()
        visualizer = null
        currentSessionId = 0
        smoothedBass = 0f
    }

    private fun updateGlowForPlaybackState(isPlaying: Boolean) {
        visualizer?.enabled = isPlaying
        if (!isPlaying) {
            binding.pulseGlow.post {
                binding.pulseGlow.scaleX = 1f
                binding.pulseGlow.scaleY = 1f
                binding.pulseGlow.alpha = 0.45f
            }
        }
    }

    private fun calculateBassIntensity(fft: ByteArray): Float {
        var sum = 0f
        var count = 0
        var i = 2
        while (i < 24 && i + 1 < fft.size) {
            val re = fft[i].toFloat()
            val im = fft[i + 1].toFloat()
            val mag = sqrt(re * re + im * im)
            sum += mag
            count++
            i += 2
        }
        return if (count == 0) 0f else sum / count
    }
}
