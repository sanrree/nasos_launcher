package com.alexdev.kiosk
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AppView(val name:String,val icon:Drawable,val packageName: String)

class MyGridAdapter(
    private val context: Context,
    private val values: Array<AppView>
) :
    BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var gridView: View

        if (convertView == null) {
            gridView = inflater.inflate(R.layout.list_item, null)

            val textView = gridView.findViewById<TextView>(R.id.grid_item_label)

            textView.text = values[position].name

            val imageView = gridView.findViewById<ImageView>(R.id.grid_item_image)
            imageView.setImageDrawable(values[position].icon);
        } else {
            gridView = convertView
        }
        return gridView
    }

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

}