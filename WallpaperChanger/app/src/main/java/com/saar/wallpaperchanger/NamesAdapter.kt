package com.saar.wallpaperchanger


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NamesAdapter : RecyclerView.Adapter<NamesAdapter.NameViewHolder>() {

    private val names = mutableListOf<String>()

    class NameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.name_item, parent, false)
        return NameViewHolder(view)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        holder.nameTextView.text = names[position]
    }

    override fun getItemCount(): Int = names.size

    fun setNames(newNames: List<String>) {
        names.clear()
        names.addAll(newNames)
        notifyDataSetChanged()
    }
}