package com.mysofttechnology.homeautomation.adapters

import android.app.Activity
import android.content.res.TypedArray
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.mysofttechnology.homeautomation.R

class IconListAdapter(
    private val context: Activity,
    private val namesList: Array<String>,
    private val iconsList: TypedArray
) : ArrayAdapter<String>(context, R.layout.icons_list_layout, namesList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.icons_list_layout, null, true)

        val imageView = rowView.findViewById(R.id.icon) as ImageView
        val titleText = rowView.findViewById(R.id.name) as TextView

        titleText.text = namesList[position]
        imageView.setImageResource(iconsList.getResourceId(position, 0))

        return rowView
    }
}