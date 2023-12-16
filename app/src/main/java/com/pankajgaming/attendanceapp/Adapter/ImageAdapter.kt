package com.pankajgaming.attendanceapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pankajgaming.attendanceapp.DataClass.ImageItem
import com.pankajgaming.attendanceapp.R

class ImageAdapter(private val imageList: List<ImageItem>, private val onItemClick: (String, String) -> Unit) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageList[position]

        Glide.with(holder.itemView.context).load(imageItem.imageUrl).into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(imageItem.imageUrl, imageItem.imageName)
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}

