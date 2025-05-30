package com.example.winampinspiredmp3player.ui.playlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.winampinspiredmp3player.R
import com.example.winampinspiredmp3player.data.Track
import com.example.winampinspiredmp3player.databinding.ListItemTrackBinding
import java.util.concurrent.TimeUnit

class PlaylistAdapter(
    private var tracks: MutableList<Track>,
    private val actualOnTrackClickListener: (Track, Int, List<Track>) -> Unit // Renamed for clarity
) : RecyclerView.Adapter<PlaylistAdapter.TrackViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ListItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track, position == selectedPosition)

        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) { // Only update if a new item is selected
                notifyItemChanged(selectedPosition) // Un-highlight old item
                selectedPosition = holder.adapterPosition
                notifyItemChanged(selectedPosition) // Highlight new item
            }
            actualOnTrackClickListener(track, holder.adapterPosition, tracks)
        }
    }

    override fun getItemCount(): Int = tracks.size

    fun updateTracks(newTracks: List<Track>) {
        tracks.clear()
        tracks.addAll(newTracks)
        selectedPosition = RecyclerView.NO_POSITION // Reset selection when list updates
        notifyDataSetChanged()
    }

    // Call this when a track starts playing from outside (e.g. service)
    fun setSelectedTrack(trackUri: String?) {
        val newPosition = tracks.indexOfFirst { it.uri.toString() == trackUri }
        if (newPosition != -1 && newPosition != selectedPosition) {
            notifyItemChanged(selectedPosition)
            selectedPosition = newPosition
            notifyItemChanged(selectedPosition)
        } else if (newPosition == -1 && selectedPosition != RecyclerView.NO_POSITION) {
            // If track is not found (e.g. playback stopped or different list)
            notifyItemChanged(selectedPosition)
            selectedPosition = RecyclerView.NO_POSITION
        }
    }


    inner class TrackViewHolder(private val binding: ListItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track, isSelected: Boolean) {
            val titleToDisplay = if (track.title.isNullOrBlank()) track.fileName else track.title
            val artistToDisplay = if (track.artist.isNullOrBlank()) "<Unknown>" else track.artist

            binding.tvTrackTitle.text = titleToDisplay
            binding.tvTrackArtist.text = artistToDisplay
            binding.tvTrackDuration.text = formatDuration(track.duration)

            itemView.isActivated = isSelected // This will use the list_item_selector.xml for background

            val context = itemView.context
            if (isSelected) {
                binding.tvTrackTitle.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_selected))
                binding.tvTrackArtist.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_selected))
                binding.tvTrackDuration.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_selected))
            } else {
                binding.tvTrackTitle.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_normal))
                binding.tvTrackArtist.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_normal))
                binding.tvTrackDuration.setTextColor(ContextCompat.getColor(context, R.color.winamp_playlist_text_normal))
            }
        }

        private fun formatDuration(millis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(minutes)
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
