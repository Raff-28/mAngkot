package com.rafif.m_angkot.ui.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rafif.m_angkot.databinding.ItemRouteBinding

class RouteAdapter(
    private val onClick: (String, Int) -> Unit
) : RecyclerView.Adapter<RouteAdapter.RouteHolder>() {
    private val routes = arrayListOf<String>()

    inner class RouteHolder(
        private val binding: ItemRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routes: String, position: Int) {
            binding.tvRoute.text = routes

            itemView.setOnClickListener {
                onClick(routes, position)
            }
        }
    }

    fun setData(routes: List<String>) {
        this.routes.clear()
        this.routes.addAll(routes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRouteBinding.inflate(inflater, parent, false)

        return RouteHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteHolder, position: Int) {
        holder.bind(routes[position], position)
    }

    override fun getItemCount(): Int {
        return routes.size
    }
}