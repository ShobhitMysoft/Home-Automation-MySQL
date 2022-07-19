package com.mysofttechnology.homeautomation.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.mysofttechnology.homeautomation.DashbordFragment
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.RoomsFragmentDirections
import com.mysofttechnology.homeautomation.StartActivity
import com.mysofttechnology.homeautomation.activities.DeletedActivity
import com.mysofttechnology.homeautomation.models.RoomsViewModel
import com.mysofttechnology.homeautomation.utils.MyFirebaseDatabase

class RoomsRecyclerAdapter(private val context: Context, private val roomList: MutableList<RoomsViewModel>) : RecyclerView.Adapter<RoomsRecyclerAdapter.ViewHolder>() {

    private val myFBDatabase = MyFirebaseDatabase()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rooms_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = roomList[position]

        holder.roomName.text = room.roomName
        holder.roomID.text = room.roomId

        holder.deleteBtn.setOnClickListener {
            showDeleteDialog(room.roomName, position)
        }

        holder.itemView.setOnClickListener {
            val navController = Navigation.findNavController(holder.itemView)
            val action = RoomsFragmentDirections.actionRoomsFragmentToEditRoomFragment(room.roomId, room.roomName)
            navController.navigate(action)
        }
    }

    private fun showDeleteDialog(roomName: String, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Room?").setMessage("Are you sure you want to delete $roomName?")
            .setPositiveButton("Ok"
            ) { _, _ ->
                myFBDatabase.removeRoom(roomName)
                roomList.removeAt(position)
                notifyItemRemoved(position)

                val intent = Intent(context, DeletedActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            .setNegativeButton("No") { _, _ -> }
        // Create the AlertDialog object and return it
        builder.create()
        builder.show()
    }

    override fun getItemCount(): Int {
        return roomList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val roomName: TextView = itemView.findViewById(R.id.room_name_tv)
        val roomID: TextView = itemView.findViewById(R.id.room_id_tv)
        val deleteBtn: ImageView = itemView.findViewById(R.id.delete_button)
    }
}