import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neurocom.bustraffic.R

class RouteAdapter(private val routeTimes: List<String>) :
    RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routeTimes[position])
    }

    override fun getItemCount(): Int = routeTimes.size

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRouteTime: TextView = itemView.findViewById(R.id.tvRouteTime)

        fun bind(time: String) {
            tvRouteTime.text = time
        }
    }
}
