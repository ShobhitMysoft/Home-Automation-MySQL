package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.mysofttechnology.homeautomation.databinding.FragmentDashbordBinding

private const val TAG = "DashbordFragment"
class DashbordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference

    private var _binding: FragmentDashbordBinding? = null
    private val binding get() = _binding!!

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null
    private var roomsList: ArrayList<String> = arrayListOf()

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDashbordBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = db.getReference("root/users/$cuPhoneNo/profile")

        loadingDialog.show(childFragmentManager, TAG)

        checkDeviceAvailability()

        binding.moreMenu.setOnClickListener {
            showPopupMenu(it)
        }

        binding.addDeviceBtn.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_dashbordFragment_to_scanDeviceFragment)
        }
    }

    private fun checkDeviceAvailability() {
        if (isOnline()) {
            dbRef.get().addOnSuccessListener {
                if (it.hasChild("devices")) {
                    it.child("devices").children.forEach { device ->
                        roomsList.add(device.child("name").value.toString())
                    }
                    loadingDialog.dismiss()
                    binding.addDeviceBtn.visibility = View.GONE
                    binding.mainDashboard.visibility = View.VISIBLE
                } else {
                    Log.d(TAG, "checkDeviceAvailability: No device available")
                    dbRef.child("deviceCount").setValue("0")
                    loadingDialog.dismiss()
                    binding.addDeviceBtn.visibility = View.VISIBLE
                    binding.mainDashboard.visibility = View.VISIBLE
                }
            }
        } else {
            loadingDialog.dismiss()
            Snackbar.make(binding.dashRootView, "No internet.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    checkDeviceAvailability()
                }.show()
        }
    }

    private fun showPopupMenu(view: View?) {
        val builder = AlertDialog.Builder(requireActivity())

        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it!!.itemId) {
                R.id.profile -> {
                    val action = DashbordFragmentDirections.actionDashbordFragmentToProfileFragment()
                    findNavController().navigate(action)
                }
                R.id.rooms -> {
                    val action = DashbordFragmentDirections.actionDashbordFragmentToRoomsFragment()
                    findNavController().navigate(action)
                }
                R.id.logout -> {
                    builder.setTitle("Logout").setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes"
                        ) { _, _ ->
                            loadingDialog.show(childFragmentManager, LoadingDialog.LOADING_DIALOG)
                            FirebaseAuth.getInstance().signOut()
                            val action = DashbordFragmentDirections.actionDashbordFragmentToRegistrationFragment()
                            loadingDialog.dismiss()
                        }
                        .setNegativeButton("No") { _, _ -> }
                    builder.create()
                    builder.show()
                }
            }
            true
        }
        popup.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    private fun isOnline(context: Context = requireContext()): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }
}