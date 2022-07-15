package com.mysofttechnology.homeautomation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mysofttechnology.homeautomation.adapters.RoomsRecyclerAdapter
import com.mysofttechnology.homeautomation.databinding.FragmentRoomsBinding
import com.mysofttechnology.homeautomation.models.RoomsViewModel
import com.mysofttechnology.homeautomation.utils.MyFirebaseDatabase
import java.util.*
import kotlin.collections.ArrayList


private const val TAG = "RoomsFragment"

class RoomsFragment : Fragment() {

    private lateinit var roomAdapter: RoomsRecyclerAdapter
    private lateinit var roomTouchHelper: ItemTouchHelper

    private var _binding: FragmentRoomsBinding? = null
    private val bind get() = _binding!!

    private val myFBDatabase: MyFirebaseDatabase = MyFirebaseDatabase()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRoomsBinding.inflate(inflater, container, false)
        bind.roomRecyclerview.layoutManager = LinearLayoutManager(requireActivity())
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomAdapter = RoomsRecyclerAdapter(requireActivity(), roomsData())

//        if (roomsData().isEmpty()) {
//            bind.msg.visibility = View.VISIBLE
//        } else bind.msg.visibility = View.GONE

        bind.roomsBackBtn.setOnClickListener {
            bind.roomsBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_roomsFragment_to_dashbordFragment)
        }

        bind.addRoomFab.setOnClickListener {
            bind.addRoomFab.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_roomsFragment_to_scanDeviceFragment)
        }

        bind.roomRecyclerview.adapter = roomAdapter
        roomTouchHelper = ItemTouchHelper(roomItemTouchHelper)

        roomTouchHelper.attachToRecyclerView(bind.roomRecyclerview)
    }

    private fun roomsData(): MutableList<RoomsViewModel> {
        val roomsData = ArrayList<RoomsViewModel>()

        myFBDatabase.dbProfileRef.child("devices").orderByChild("order")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        roomsData.clear()
                        snapshot.children.forEach { device ->
                            val deviceName = device.child("name").value.toString()
                            val deviceId = device.child("id").value.toString()
                            val deviceOrder = device.child("order").value.toString()
                            roomsData.add(RoomsViewModel(deviceName, deviceId, deviceOrder))
                            roomAdapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "onCancelled: roomsData() - ${error.message}")
                    }

                })
        return roomsData
    }

    private var roomItemTouchHelper = object :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.START or ItemTouchHelper.END or ItemTouchHelper.DOWN or ItemTouchHelper.UP,
            0
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {

            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            Collections.swap(roomsData(), fromPosition, toPosition)

            bind.roomRecyclerview.adapter?.notifyItemMoved(fromPosition, toPosition)

            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }
}


// TODO: Use this code for fragments
//
//
//private var _binding: FragmentNameBinding? = null
//private val bind get() = _binding!!
//
//override fun onCreateView(
//    inflater: LayoutInflater, container: ViewGroup?,
//    savedInstanceState: Bundle?
//): View? {
//    _binding = FragmentNameBinding.inflate(inflater, container, false)
//    return bind.root
//}
//
//override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//
//    bind.backBtn.setOnClickListener {
//        Navigation.findNavController(it).navigate(R.id.action)
//    }
//}