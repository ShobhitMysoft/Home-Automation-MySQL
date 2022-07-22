package com.mysofttechnology.homeautomation.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.RoomsFragment.Companion.currentUserId
import com.mysofttechnology.homeautomation.RoomsFragmentDirections
import com.mysofttechnology.homeautomation.activities.DeletedActivity
import com.mysofttechnology.homeautomation.models.RoomsViewModel
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "RoomsRecyclerAdapter"
class RoomsRecyclerAdapter(private val context: Context, private val roomList: MutableList<RoomsViewModel>) : RecyclerView.Adapter<RoomsRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rooms_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = roomList[position]

        holder.roomName.text = room.roomName
        holder.roomID.text = room.deviceId

        Log.d(TAG, "onBindViewHolder: Room - $room\nRoom List - $roomList")
        if (room.userValid == "0") holder.shareBtn.visibility = View.VISIBLE
        else holder.shareBtn.visibility = View.GONE

        holder.deleteBtn.setOnClickListener {
            showDeleteDialog(room.roomName, position, room.deviceId)
        }

        holder.shareBtn.setOnClickListener {
            val otp = (100000..999999).random()
            generateOTP(room.deviceId, otp.toString(), holder.roomOTP, holder.shareBtn)
        }

        holder.itemView.setOnClickListener {
            val navController = Navigation.findNavController(holder.itemView)
            val action = RoomsFragmentDirections.actionRoomsFragmentToEditRoomFragment(room.deviceId, room.roomName, room.roomId)
            navController.navigate(action)
        }
    }

    private fun generateOTP(deviceId: String, otp: String, roomOTP: TextView, shareBtn: ImageView) {
        Log.i(TAG, "deleteRoom: Room Id - $deviceId")
        val requestQueue = VolleySingleton.getInstance(context).requestQueue
        val createOtpUrl = context.getString(R.string.base_url) + context.getString(R.string.url_create_opt)

        val liveDataRequest = object : StringRequest(Method.POST, createOtpUrl,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        if (otp.isBlank()) {
                            roomOTP.text = otp
                            roomOTP.visibility = View.GONE
                            shareBtn.visibility = View.VISIBLE
                        } else {
                            roomOTP.text = "OTP : $otp"
                            shareBtn.visibility = View.GONE
                            roomOTP.visibility = View.VISIBLE

                            showToast("OTP will be invalid after 60 seconds.")

                            Handler().postDelayed({
                                generateOTP(deviceId, "", roomOTP, shareBtn)
                            }, 60000)
                        }

                        Log.d(TAG, "generateOTP: Message - $msg")
                    } else {
                        showToast("Problem generating OTP.")
//                        showErrorScreen()
                        Log.e(TAG, "generateOTP: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in generateOTP: $e")
                    showToast(e.message)
                }
            }, {
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                params["otp"] = otp
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        requestQueue.add(liveDataRequest)
    }

    private fun showDeleteDialog(roomName: String, position: Int, deviceId: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Room?").setMessage("Are you sure you want to delete $roomName?")
            .setPositiveButton("Ok"
            ) { _, _ ->
                deleteRoom(position, deviceId)
            }
            .setNegativeButton("No") { _, _ -> }
        builder.create()
        builder.show()
    }

    private fun deleteRoom(position: Int, deviceId: String) {
        Log.i(TAG, "deleteRoom: Room Id - $deviceId")
        val requestQueue = VolleySingleton.getInstance(context).requestQueue
        val roomDeleteUrl = context.getString(R.string.base_url) + context.getString(R.string.url_room_delete)

        val liveDataRequest = object : StringRequest(Method.POST, roomDeleteUrl,
            { response ->
                Log.i(TAG, "deleteRoom: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        roomList.removeAt(position)
                        notifyItemRemoved(position)

                        val intent = Intent(context, DeletedActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)

                        Log.d(TAG, "deleteRoom: Message - $msg")
                    } else {
                        showToast("Room deletion failed.")
//                        showErrorScreen()
                        Log.e(TAG, "deleteRoom: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in deleteRoom: $e")
                    showToast(e.message)
                }
            }, {
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                params["mobile"] = currentUserId.toString()
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        requestQueue.add(liveDataRequest)
    }

    private fun showToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun getItemCount(): Int {
        return roomList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val roomName: TextView = itemView.findViewById(R.id.room_name_tv)
        val roomID: TextView = itemView.findViewById(R.id.room_id_tv)
        val deleteBtn: ImageView = itemView.findViewById(R.id.delete_button)
        val shareBtn: ImageView = itemView.findViewById(R.id.share_button)
        val roomOTP: TextView = itemView.findViewById(R.id.otp_tv)
    }
}