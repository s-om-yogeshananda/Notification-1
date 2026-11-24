package com.glass.notifpanel

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotifAdapter(private val onClick: (NotifItem) -> Unit) :
    RecyclerView.Adapter<NotifAdapter.VH>() {

    private var data: List<NotifItem> = emptyList()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val app: TextView = v.findViewById(R.id.app)
        val time: TextView = v.findViewById(R.id.time)
        val title: TextView = v.findViewById(R.id.title)
        val text: TextView = v.findViewById(R.id.text)
    }

    fun submit(list: List<NotifItem>) {
        data = list
        notifyDataSetChanged()
    }

    fun itemAt(p: Int): NotifItem? = data.getOrNull(p)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_notif, parent, false))

    override fun getItemCount() = data.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val n = data[position]
        h.icon.setImageDrawable(n.icon)
        h.app.text = n.app
        h.time.text = DateFormat.format("hh:mm a", n.time).toString()
        h.title.text = n.title
        h.title.visibility = if (n.title.isEmpty()) View.GONE else View.VISIBLE
        h.text.text = n.text
        h.text.visibility = if (n.text.isEmpty()) View.GONE else View.VISIBLE
        h.itemView.setOnClickListener { onClick(n) }
    }
}
