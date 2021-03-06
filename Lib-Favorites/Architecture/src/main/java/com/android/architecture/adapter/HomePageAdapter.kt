package com.android.architecture.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.architecture.R
import com.android.architecture.data.HomeData
import com.android.architecture.demolist.databinding.DataBindingActivity
import com.android.architecture.demolist.lifecycle.LifecycleActivity
import com.android.architecture.demolist.livedata.LiveDataActivity
import com.android.architecture.demolist.navigation.NavigationActivity
import com.android.architecture.demolist.paging.PagingActivity
import com.android.architecture.demolist.room.RoomActivity
import com.android.architecture.demolist.viewmodel.ViewModelActivity
import com.android.architecture.demolist.workmanager.WorkManagerActivity

class HomePageAdapter(private val mContext: Context, private val mItemData: List<HomeData.ItemView>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mHeaderViews = SparseArrayCompat<Int>()

    private val headersCount: Int
        get() = mHeaderViews.size()

    val isHaveHeaderView: Boolean
        get() = mHeaderViews.size() > 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (mHeaderViews.get(viewType) != null) {
            val v = LayoutInflater.from(mContext).inflate(mHeaderViews.get(viewType)!!, parent,
                false)
            HeadViewHolder(v)
        } else if (ITEM_TYPE_TITLE == viewType) {
            val v = LayoutInflater.from(mContext).inflate(R.layout.activity_home_page_title,
                parent, false)
            TitleViewHolder(v)
        } else {
            val v = LayoutInflater.from(mContext).inflate(R.layout.activity_home_page_item,
                parent, false)
            ItemViewHolder(v)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        var position = position
        val viewType = getItemViewType(position)
        if (mHeaderViews.get(viewType) != null) {
            val headViewHolder = viewHolder as HeadViewHolder
            headViewHolder.scan.setOnClickListener { }
            return
        } else {
            position -= headersCount
        }
        if (viewType == ITEM_TYPE_TITLE) {
            val titleViewHolder = viewHolder as TitleViewHolder
            titleViewHolder.title.text = mItemData[position].desc
        } else {
            val itemViewHolder = viewHolder as ItemViewHolder
            itemViewHolder.name.text = mItemData[position].desc
            //            itemViewHolder.icon.setText(mItemData.get(position).icon);
            val pos = position
            itemViewHolder.name.setOnClickListener {
                val intent = Intent()
                when (pos) {
                    1 -> {
                        intent.setClass(mContext, LifecycleActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    2 -> {
                        intent.setClass(mContext, ViewModelActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    3 -> {
                        intent.setClass(mContext, LiveDataActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    4 -> {
                        intent.setClass(mContext, DataBindingActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    5 -> {
                        intent.setClass(mContext, PagingActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    6 -> {
                        intent.setClass(mContext, RoomActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    7 -> {
                        intent.setClass(mContext, NavigationActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    8 -> {
                        intent.setClass(mContext, WorkManagerActivity::class.java)
                        mContext.startActivity(intent)
                    }
                    9 -> {

                    }
                    10 -> {

                    }
                    else -> {
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isHeaderViewPos(position) -> {
                mHeaderViews.keyAt(position)
            }
            isTitle(position) -> {
                ITEM_TYPE_TITLE
            }
            else -> {
                ITEM_TYPE_SECOND
            }
        }
    }

    override fun getItemCount(): Int {
        return mItemData.size + headersCount
    }

    private fun isHeaderViewPos(position: Int): Boolean {
        return position < headersCount
    }

    fun addHeaderView(view: Int) {
        mHeaderViews.put(mHeaderViews.size() + ITEM_TYPE_HEADER, view)
    }

    fun removeHeaderView() {
        mHeaderViews.clear()
    }

    private fun isTitle(position: Int): Boolean {
        return "" == mItemData[position - headersCount].icon
    }

    internal inner class ItemViewHolder @SuppressLint("WrongViewCast")
    constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.tv_item_name)
        var icon: TextView = itemView.findViewById(R.id.tv_item_icon)
    }

    internal inner class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = itemView.findViewById(R.id.tv_title)
    }

    internal inner class HeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var scan: TextView = itemView.findViewById(R.id.tv_scan)
    }

    companion object {
        private val ITEM_TYPE_HEADER = 100000
        private val ITEM_TYPE_TITLE = 111110
        private val ITEM_TYPE_SECOND = 111111
    }
}
