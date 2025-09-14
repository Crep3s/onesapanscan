package com.example.warehousescanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.warehousescanner.ui.ImageGalleryAdapter

class ImageGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_gallery)

        val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: arrayListOf()
        val viewPager: ViewPager2 = findViewById(R.id.gallery_view_pager)
        viewPager.adapter = ImageGalleryAdapter(imageUrls)
    }

    companion object {
        const val EXTRA_IMAGE_URLS = "image_urls"
    }
}