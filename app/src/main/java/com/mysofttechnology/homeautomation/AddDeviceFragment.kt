package com.mysofttechnology.homeautomation

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
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "AddDeviceFragment"
class AddDeviceFragment : Fragment() {

    private var sharedPref: SharedPreferences? = null
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")
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
                            Navigation.findNavController(requireView()).navigate(R.id.action_addDeviceFragment_to_roomControlsFragment)
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Exception: $e")
                    }
                }, {
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["user_id"] = currentUserId.toString()
                    return params
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }
            requestQueue.add(stringRequest)
        }
    }

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

    override fun onResume() {
        super.onResume()
        
        checkDeviceAvailability()
    }
}