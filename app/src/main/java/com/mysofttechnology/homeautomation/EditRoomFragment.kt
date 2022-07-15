package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.isDigitsOnly
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mysofttechnology.homeautomation.activities.DeletedActivity
import com.mysofttechnology.homeautomation.activities.WorkDoneActivity
import com.mysofttechnology.homeautomation.adapters.IconListAdapter
import com.mysofttechnology.homeautomation.databinding.FragmentEditRoomBinding
import com.mysofttechnology.homeautomation.utils.MyFirebaseDatabase

private const val ARG_ROOM_ID = "roomID"
private const val TAG = "EditRoomFragment"

class EditRoomFragment : Fragment() {
    private var roomId: String? = null

    private var _binding: FragmentEditRoomBinding? = null
    private val bind get() = _binding!!

    private var myDbHandler: MyFirebaseDatabase = MyFirebaseDatabase()

    private lateinit var iconsList: TypedArray
    private lateinit var iconsNameList: Array<String>
    private var switch1Icon: Int = 0
    private var switch2Icon: Int = 0
    private var switch3Icon: Int = 0
    private var switch4Icon: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            roomId = it.getString(ARG_ROOM_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditRoomBinding.inflate(inflater, container, false)
        // TODO : Remove this initialisation
        myDbHandler = MyFirebaseDatabase()
        iconsList = resources.obtainTypedArray(R.array.icons_list)
        iconsNameList = resources.getStringArray(R.array.icons_names)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()


        bind.switch1Icon.setOnClickListener { showChooseIconDialog(1, bind.switch1Icon) }
        bind.switch2Icon.setOnClickListener { showChooseIconDialog(2, bind.switch2Icon) }
        bind.switch3Icon.setOnClickListener { showChooseIconDialog(3, bind.switch3Icon) }
        bind.switch4Icon.setOnClickListener { showChooseIconDialog(4, bind.switch4Icon) }
        bind.submitBtn.setOnClickListener { submitRoomDetails() }

        bind.backBtn.setOnClickListener {
            bind.backBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_editRoomFragment_to_roomsFragment)
        }
    }

    private fun submitRoomDetails() {
        val roomName = bind.roomNameEt.text.toString().trim()
        val switch1Name = bind.switch1NameEt.text.toString().trim()
        val switch2Name = bind.switch2NameEt.text.toString().trim()
        val switch3Name = bind.switch3NameEt.text.toString().trim()
        val switch4Name = bind.switch4NameEt.text.toString().trim()

        if (roomName.isNotBlank()) {
            if (switch1Name.isNotBlank()) {
                if (switch2Name.isNotBlank()) {
                    if (switch3Name.isNotBlank()) {
                        if (switch4Name.isNotBlank()) {
                            showConfirmDialog(
                                roomName,
                                switch1Name,
                                switch2Name,
                                switch3Name,
                                switch4Name
                            )
                        } else bind.switch4NameEt.error = "Switch name is required"
                    } else bind.switch3NameEt.error = "Switch name is required"
                } else bind.switch2NameEt.error = "Switch name is required"
            } else bind.switch1NameEt.error = "Switch name address is required"
        } else bind.roomNameEt.error = "Room name is required"
    }

    private fun showConfirmDialog(
        roomName: String,
        switch1Name: String,
        switch2Name: String,
        switch3Name: String,
        switch4Name: String
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Update Room").setMessage("Are you sure you want to update $roomId room?")
            .setPositiveButton(
                "Ok"
            ) { _, _ ->
                myDbHandler.dbProfileRef.child("devices").child(roomId!!).apply {
                    child("name").setValue(roomName)
                    child("switch1").child("name").setValue(switch1Name)
                    child("switch1").child("icon").setValue(switch1Icon)
                    child("switch2").child("name").setValue(switch2Name)
                    child("switch2").child("icon").setValue(switch2Icon)
                    child("switch3").child("name").setValue(switch3Name)
                    child("switch3").child("icon").setValue(switch3Icon)
                    child("switch4").child("name").setValue(switch4Name)
                    child("switch4").child("icon").setValue(switch4Icon)
                }
//                Navigation.findNavController(requireView()).navigate(R.id.action_editRoomFragment_to_dashbordFragment)
//                Toast.makeText(requireActivity(), "Room updated!", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), WorkDoneActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(intent)
            }
            .setNegativeButton("No") { _, _ -> }

        builder.create()
        builder.show()
    }

    private fun showChooseIconDialog(id: Int, switch: ImageView) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val listDialogInflater = inflater.inflate(R.layout.icons_list_dialog_layout, null)

        val iconListAdapter = IconListAdapter(requireActivity(), iconsNameList, iconsList)
        val listView = listDialogInflater.findViewById<ListView>(R.id.icon_listview)
        listView.adapter = iconListAdapter

        builder.setView(listDialogInflater)
        builder.setPositiveButton("Cancel") { _, _ -> }
        val dialog = builder.create()
        dialog.show()

        listView.setOnItemClickListener { _, _, position, _ ->
            switch.setImageResource(iconsList.getResourceId(position, 0))
            if (id==1) switch1Icon=position
            if (id==2) switch2Icon=position
            if (id==3) switch3Icon=position
            if (id==4) switch4Icon=position
//            myDbHandler.dbProfileRef.child("devices").child(roomId!!).child("switch").child("icon").setValue(position)
            dialog.dismiss()
        }
    }

    private fun updateUI() {
        bind.roomIdTv.text = roomId

        myDbHandler.dbProfileRef.child("devices").child(roomId!!).get().addOnSuccessListener {
            bind.roomNameEt.setText(it.child("name").value.toString())

            bind.switch1NameEt.setText(it.child("switch1").child("name").value.toString())
            switch1Icon = (it.child("switch1").child("icon").value as Long).toInt()
            bind.switch1Icon.setImageResource(iconsList.getResourceId(switch1Icon, 0))

            bind.switch2NameEt.setText(it.child("switch2").child("name").value.toString())
            switch2Icon = (it.child("switch2").child("icon").value as Long).toInt()
            bind.switch2Icon.setImageResource(iconsList.getResourceId(switch2Icon, 0))

            bind.switch3NameEt.setText(it.child("switch3").child("name").value.toString())
            switch3Icon = (it.child("switch3").child("icon").value as Long).toInt()
            bind.switch3Icon.setImageResource(iconsList.getResourceId(switch3Icon, 0))

            bind.switch4NameEt.setText(it.child("switch4").child("name").value.toString())
            switch4Icon = (it.child("switch4").child("icon").value as Long).toInt()
            bind.switch4Icon.setImageResource(iconsList.getResourceId(switch4Icon, 0))

        }
    }
}