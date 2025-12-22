package com.example.trecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SelectedDatesAdapter(
    private var dates: List<Date>,
    private val onDateRemove: (Date) -> Unit
) : RecyclerView.Adapter<SelectedDatesAdapter.DateViewHolder>() {

    private val dateFormatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru"))

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val removeButton: TextView = itemView.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        holder.dateText.text = dateFormatter.format(date)

        holder.removeButton.setOnClickListener {
            onDateRemove(date)
        }
    }

    override fun getItemCount() = dates.size

    fun updateDates(newDates: List<Date>) {
        dates = newDates.sorted()
        notifyDataSetChanged()
    }
}