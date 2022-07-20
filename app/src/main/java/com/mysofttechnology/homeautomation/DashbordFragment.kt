package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mysofttechnology.homeautomation.databinding.FragmentDashbordBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "DashbordFragment"
class DashbordFragment : Fragment() {

    private var sharedPref: SharedPreferences? = null
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentDashbordBinding? = null
    private val binding get() = _binding!!

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null
    private var currentUserId: String? = null

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
        _binding = FragmentDashbordBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref?.getString(getString(R.string.current_user_id), "")
        binding.actionbarTv.text = currentUserId

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
            val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
            val url = getString(R.string.base_url)+getString(R.string.url_room_list)

            val stringRequest = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            updateUI(true)
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        } else {
                            loadingDialog.dismiss()
                            updateUI(false)
//                            showToast("No user found. Please register first.")
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        }
                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        Log.d(TAG, "Exception: $e")
                        showToast(e.message)
                    }
                }, {
                    loadingDialog.dismiss()
                    showToast("Something went wrong.")
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["user_id"] = cuPhoneNo.toString()
                    return params
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }
            requestQueue.add(stringRequest)
        } else {
            loadingDialog.dismiss()
            Snackbar.make(binding.dashRootView, "No internet.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    checkDeviceAvailability()
                }.show()
        }
    }

    private fun updateUI(flag: Boolean) {
        if (flag) {
            loadingDialog.dismiss()
            binding.addDeviceBtn.visibility = View.GONE
            binding.fragmentContainerView2.findNavController().navigate(R.id.roomControlsFragment)
        } else {
            Log.d(TAG, "updateUI: No device available")
            loadingDialog.dismiss()
            binding.addDeviceBtn.visibility = View.VISIBLE
            binding.fragmentContainerView2.findNavController().navigate(R.id.addDeviceFragment)
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
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
                            loadingDialog.show(childFragmentManager, TAG)
                            signOutUser()
                            val action = DashbordFragmentDirections.actionDashbordFragmentToRegistrationFragment()
                            findNavController().navigate(action)
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

    private fun signOutUser() {
        FirebaseAuth.getInstance().signOut()
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

    override fun onStart() {
        super.onStart()
        val spEditor = sharedPref?.edit()
//                                                                 TODO: ????
        spEditor?.putString(getString(R.string.current_user_id), cuPhoneNo)
        spEditor?.apply()
    }
}