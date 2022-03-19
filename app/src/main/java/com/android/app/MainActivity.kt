package com.android.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.core.utils.dip
import com.android.core.widget.multiimage.ImageEntity
import com.android.core.widget.multiimage.RecMultiImageLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val multiImageLayout = findViewById<RecMultiImageLayout>(R.id.iv_image)
//        val dataList = arrayListOf<ImageEntity>()
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("https://c-ssl.duitang.com/uploads/blog/202105/09/20210509225323_2c0c6.jpeg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        dataList.add(ImageEntity("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg"))
//        multiImageLayout.setMarginArray(intArrayOf(dip(10), dip(10)))
//        multiImageLayout.showImages(dataList)
//        multiImageLayout.showVideo("http://inews.gtimg.com/newsapp_match/0/11697869775/0.jpg")

        findViewById<Button>(R.id.button)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlanetActivity::class.java))
        }
        findViewById<Button>(R.id.button2)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlanetActivity::class.java).apply{
                putExtra("isDemo",false)
            })
        }
    }
}