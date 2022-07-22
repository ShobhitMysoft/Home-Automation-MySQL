package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.mysofttechnology.homeautomation.activities.WorkDoneActivity
import com.mysofttechnology.homeautomation.databinding.FragmentEditRoomBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val ARG_ROOM_ID = "roomID"
private const val ARG_DEVICE_ID = "deviceID"
private const val ARG_ROOM_NAME = "roomName"
private const val TAG = "EditRoomFragment"

class EditRoomFragment : Fragment() {
    private var roomId: String? = null
    private var deviceId: String? = null
    private var roomName: String? = null

    private lateinit var loadingDialog: LoadingDialog
    private var currentUserId: String? = null
    private var sharedPref: SharedPreferences? = null

    private var _binding: FragmentEditRoomBinding? = null
    private val bind get() = _binding!!

    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            roomId = it.getString(ARG_ROOM_ID)
            deviceId = it.getString(ARG_DEVICE_ID)
            roomName = it.getString(ARG_ROOM_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditRoomBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")

        loadUI()

        bind.submitBtn.setOnClickListener { submitRoomDetails() }

        bind.backBtn.setOnClickListener {
            bind.backBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_editRoomFragment_to_roomsFragment)
        }
    }

    private fun submitRoomDetails() {
        val roomName = bind.roomNameEt.text.toString().trim()

        if (roomName.isNotBlank()) {
            showConfirmDialog(roomName)
        } else bind.roomNameEt.error = "Room name is required"
    }

    private fun showConfirmDialog(
        roomName: String
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Update Room")
            .setMessage("Are you sure you want to update $roomName room?")
            .setPositiveButton(
                "Ok"
            ) { _, _ ->
                updateRoom(roomName)
            }
            .setNegativeButton("No") { _, _ -> }

        builder.create()
        builder.show()
    }

    private fun updateRoom(roomName: String) {
        val roomUrl = getString(R.string.base_url) + getString(R.string.url_room)

        loadingDialog.show(childFragmentManager, TAG)
        val roomUpdateRequest = object : StringRequest(Method.POST, roomUrl,
            { response ->
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        showToast("Room updated.")
                        Log.d(TAG, "updateRoom: Message - $msg")

                        val intent = Intent(requireContext(), WorkDoneActivity::class.java)
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        requireContext().startActivity(intent)
                    } else {
                        loadingDialog.dismiss()
                        showToast("Unable to update room.")
//                        showErrorScreen()
                        Log.e(TAG, "updateRoom: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateUI: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId.toString()
                params["user_id"] = currentUserId.toString()
                params["room_name"] = roomName
                params["room_id"] = roomId.toString()
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        requestQueue.add(roomUpdateRequest)
    }

    private fun loadUI() {
        bind.roomIdTv.text = deviceId
        bind.roomNameEt.setText(roomName)
    }

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }
}