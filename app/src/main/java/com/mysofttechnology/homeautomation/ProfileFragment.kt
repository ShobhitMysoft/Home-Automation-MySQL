package com.mysofttechnology.homeautomation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.mysofttechnology.homeautomation.databinding.FragmentProfileBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "ProfileFragment"
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val bind get() = _binding!!

    private var sharedPref: SharedPreferences? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var currentUserEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")
        currentUserName = sharedPref!!.getString(getString(R.string.current_user_name), "")
        currentUserEmail = sharedPref!!.getString(getString(R.string.current_user_email), "")

        bind.profileBackBtn.setOnClickListener {
            bind.profileBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_profileFragment_to_dashbordFragment)
        }

        bind.fullName.text = currentUserName
        bind.emailAddress.text = currentUserEmail
        bind.phoneNumber.text = currentUserId

        if (currentUserName.isNullOrBlank() || currentUserEmail.isNullOrBlank()) loadProfile()
    }

    private fun loadProfile() {
        Log.i(TAG, "deleteRoom: Room Id - $currentUserId")
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val profileUrl = getString(R.string.base_url) + getString(R.string.url_profile)

        val spEditor = sharedPref?.edit()

        val liveDataRequest = object : StringRequest(Method.POST, profileUrl,
            { response ->
                Log.i(TAG, "loadProfile: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        bind.fullName.text = mData.getString("name")
                        bind.emailAddress.text = mData.getString("email")

                        spEditor?.putString(getString(R.string.current_user_name), mData.getString("name"))
                        spEditor?.putString(getString(R.string.current_user_email), mData.getString("email"))
                        spEditor?.apply()

                        Log.d(TAG, "loadProfile: Message - $msg")
                    } else {
                        // TODO: Show snackbar to retry
//                        showToast("Profile data re.")
//                        showErrorScreen()
                        Log.e(TAG, "loadProfile: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in loadProfile: $e")
                    showToast(e.message)
                }
            }, {
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["mobile_no"] = currentUserId.toString()
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
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }
}