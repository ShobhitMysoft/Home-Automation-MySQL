package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.commit
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.mysofttechnology.homeautomation.databinding.FragmentAddDeviceBinding

private const val TAG = "AddDeviceFragment"
class AddDeviceFragment : Fragment() {

    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

//    private lateinit var progressBar: View

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exitAppDialog = ExitAppDialog()

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            exitAppDialog.show(childFragmentManager, "Exit App")
        }

        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)
//        progressBar = requireActivity().findViewById(R.id.dashboard_pbar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = db.getReference("root/users/$cuPhoneNo/profile")

//        binding.addDeviceBtn.visibility = View.GONE
//        progressBar.visibility = View.VISIBLE
        checkDeviceAvailability()

//        binding.addDeviceBtn.setOnClickListener {
////            Navigation.findNavController(it).navigate(R.id.action_addDeviceFragment_to_scanDeviceFragment)
//            requireActivity().supportFragmentManager.commit {
//                replace(R.id.scanDeviceFragment, DashbordFragment())
//                setReorderingAllowed(true)
//                addToBackStack(null)
//            }
//        }
    }

    private fun checkDeviceAvailability() {
        dbRef.get().addOnSuccessListener {
            if (it.hasChild("devices")){
                val action = AddDeviceFragmentDirections.actionAddDeviceFragmentToRoomControlsFragment()
                findNavController().navigate(action)
//                progressBar.visibility = View.GONE
//                binding.addDeviceBtn.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "checkDeviceAvailability: No device available")
                binding.msg.visibility = View.VISIBLE
//                progressBar.visibility = View.GONE
//                binding.addDeviceBtn.visibility = View.VISIBLE
            }
        }
    }
}