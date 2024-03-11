package com.example.flybyelens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BirdAdapter(private val onItemClickListener: (Bird) -> Unit) :
    ListAdapter<Bird, BirdAdapter.ViewHolder>(BirdDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_bird, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bird = getItem(position)

        // Load bird image using Glide (you can replace it with Picasso or other libraries)
        Glide.with(holder.itemView.context)
            .load(bird.pictureUrl)
            .into(holder.birdImage)

        holder.birdName.text = bird.name

        // Set click listener
        holder.itemView.setOnClickListener { onItemClickListener.invoke(bird) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdImage: ImageView = itemView.findViewById(R.id.birdImage)
        val birdName: TextView = itemView.findViewById(R.id.birdName)
    }

    private class BirdDiffCallback : DiffUtil.ItemCallback<Bird>() {
        override fun areItemsTheSame(oldItem: Bird, newItem: Bird): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Bird, newItem: Bird): Boolean {
            return oldItem == newItem
        }
    }
}