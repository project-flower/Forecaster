package projectflower.forecaster

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class AreaListAdapter(context: Context, dataList: ArrayList<Pair<String, String>?>?) :
    ArrayAdapter<Pair<String, String>?>(
        context,
        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
        dataList!!
    ) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position)?.second
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        view.text = getItem(position)?.second
        return view
    }
}