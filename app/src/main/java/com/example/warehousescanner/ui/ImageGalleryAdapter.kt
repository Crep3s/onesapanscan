package com.example.warehousescanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.warehousescanner.R
import com.github.chrisbanes.photoview.PhotoView

class ImageGalleryAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageGalleryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.gallery_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.photoView.load(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size
}