package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.databinding.FragmentProfileBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "ProfileFragment"

class ProfileFragment : Fragment() {

    private var spEditor: SharedPreferences.Editor? = null
    private var updateDialog: AlertDialog? = null
    private var _binding: FragmentProfileBinding? = null
    private val bind get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

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

        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")
        currentUserName = sharedPref!!.getString(getString(R.string.current_user_name), "")
        currentUserEmail = sharedPref!!.getString(getString(R.string.current_user_email), "")

        spEditor = sharedPref?.edit()

        bind.profileBackBtn.setOnClickListener {
            bind.profileBackBtn.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_profileFragment_to_dashbordFragment)
        }

        bind.editProfileFab.setOnClickListener {
            showEditProfileDialog(currentUserName.toString(), currentUserEmail.toString())
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

        loadingDialog.show(childFragmentManager, TAG)

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

                        spEditor?.putString(getString(R.string.current_user_name),
                            mData.getString("name"))
                        spEditor?.putString(getString(R.string.current_user_email),
                            mData.getString("email"))
                        spEditor?.apply()

                        loadingDialog.dismiss()

                        Log.d(TAG, "loadProfile: Message - $msg")
                    } else {
                        Snackbar.make(bind.pfRootView, "Failed to load profile",
                            Snackbar.LENGTH_LONG)
                            .setAction("Retry") {
                                loadProfile()
                            }
                            .show()
                        loadingDialog.dismiss()
                        Log.e(TAG, "loadProfile: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in loadProfile: $e")
                    if (e.message != null) showToast(e.message)
                    loadingDialog.dismiss()
                }
            }, {
                loadingDialog.dismiss()
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

    private fun showEditProfileDialog(name: String, email: String) {
        val builder = AlertDialog.Builder(requireActivity())

        val view = requireActivity().layoutInflater.inflate(R.layout.edit_profile_layout, null)

        val userNameET = view.findViewById<TextView>(R.id.user_name_et)
        val userEmailET = view.findViewById<TextView>(R.id.user_email_et)
        val submitBtn = view.findViewById<TextView>(R.id.ep_submit_btn)

        userNameET.text = name
        userEmailET.text = email

        builder.setView(view).setTitle("Edit Profile")

        submitBtn.setOnClickListener {
            val userName = userNameET.text.toString()
            val userEmail = userEmailET.text.toString()
            if (userName.isNotBlank()) {
                if (userEmail.isNotBlank()) {
                    updateProfile(userName, userEmail)
                } else userEmailET.error = "Email address is required"
            } else userNameET.error = "Full name is required"
        }

        updateDialog = builder.create()
        updateDialog?.show()
    }

    private fun updateProfile(userName: String, userEmail: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val updateProfileUrl = getString(R.string.base_url) + getString(R.string.url_profile_update)

        loadingDialog.show(childFragmentManager, TAG)

        val roomUpdateRequest = object : StringRequest(Method.POST, updateProfileUrl,
            { response ->
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        updateDialog?.dismiss()
                        showToast("Profile updated.")
                        loadingDialog.dismiss()

                        spEditor?.putString(getString(R.string.current_user_name), "")
                        spEditor?.putString(getString(R.string.current_user_email), "")
                        spEditor?.apply()

                        loadProfile()
                        Log.d(TAG, "updateProfile: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showToast("Failed to update profile.")
                        Log.e(TAG, "updateProfile: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateProfile: $e")
                    if (e.message != null) showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["mobile"] = currentUserId.toString()
                params["name"] = userName
                params["email"] = userEmail
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

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}