package dz.notacompany.riide

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dz.notacompany.riide.databinding.ItemTripBinding

class TripsAdapter(var tripItems: List<TripItem>, private val adapterFunction: (position: Int, partToRun: Int) -> Boolean): RecyclerView.Adapter<TripsAdapter.TripsViewHolder>() {

    inner class TripsViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemTripBinding.inflate(layoutInflater, parent, false)
        return TripsViewHolder(binding)
    }


    override fun onBindViewHolder(holder: TripsViewHolder, position: Int) {
        holder.binding.apply {

            // Change the outline color of the trip that the user is currently in
            if (adapterFunction(position,1))
                itemTripCard.strokeColor = Color.parseColor("#92D8AE")

            itemTripCard.setOnClickListener {
                adapterFunction(position,0)
            }
            itemTripFromToTv.text = tripItems[position].fromTo
            itemTripPriceTv.text = tripItems[position].price
            itemTripSeatsTv.text = tripItems[position].seats
            itemTripDateTv.text = tripItems[position].date
        }
    }

    override fun getItemCount(): Int {
        return  tripItems.size
    }

}