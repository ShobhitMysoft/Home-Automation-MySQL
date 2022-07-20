package com.mysofttechnology.homeautomation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.mysofttechnology.homeautomation.databinding.FragmentVerifyCodeBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject

private const val TAG = "VerifyCodeFragment"

class VerifyCodeFragment : Fragment() {

    private var sharedPref: SharedPreferences? = null
    private var verificationId: String? = null
    private lateinit var fullName: String
    private lateinit var emailAddress: String
    private lateinit var phoneNumber: String
    private var authFlag: Int = 0

    private var _binding: FragmentVerifyCodeBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        arguments?.let {
            verificationId = it.getString("verificationID").toString()
            fullName = it.getString("fullName").toString()
            emailAddress = it.getString("emailAddress").toString()
            phoneNumber = it.getString("phoneNumber").toString()
            authFlag = it.getInt("authFlag")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVerifyCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return

        if (authFlag == 0) { gotoRegistration() }

        binding.vcBackBtn.setOnClickListener {
            binding.vcBackBtn.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_verifyCodeFragment_to_registrationFragment)
        }

        binding.vcVerifyBtn.setOnClickListener {
            binding.vcVerifyBtn.isEnabled = false
            val code = binding.vcCodeEt.text.toString().trim()

            if (code.isNotEmpty() && code.length == 6) {
                if (code.isDigitsOnly()) {
                    loadingDialog.show(childFragmentManager, TAG)
                    binding.vcVerifyBtn.isEnabled = true
                    verifyPhoneNumberWithCode(code)
                } else {
                    binding.vcVerifyBtn.error = "Enter a proper code"
                    binding.vcVerifyBtn.isEnabled = true
                }
            } else {
                binding.vcVerifyBtn.error = "Enter a proper 6-digit code"
                binding.vcVerifyBtn.isEnabled = true
            }
        }
    }

    private fun verifyPhoneNumberWithCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)

        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")

                val user = it.result?.user
                Log.d(TAG, "signInWithPhoneAuthCredential: $user")
                checkAuthRequest()
            } else {
                // Sign in failed, display a message and update the UI
                Log.w(TAG, "signInWithCredential:failure", it.exception)
                if (it.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    Toast.makeText(requireActivity(),
                        "The verification code you entered was invalid.", Toast.LENGTH_LONG).show()
                } else Toast.makeText(requireActivity(),
                    "Invalid code or this number is already logged on another device.",
                    Toast.LENGTH_LONG).show()
                loadingDialog.dismiss()
            }
        }
    }

    private fun checkAuthRequest() {
        when (authFlag) {
            1 -> { createDatabase() }
            2 -> { gotoDashboard() }
            else -> {
                Toast.makeText(requireActivity(), "Something went wrong.", Toast.LENGTH_SHORT).show()
                gotoRegistration()
            }
        }
    }

    private fun createDatabase() {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_register)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        Toast.makeText(requireActivity(), msg.toString(), Toast.LENGTH_SHORT).show()
                        gotoDashboard()
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        loadingDialog.dismiss()
                        Toast.makeText(requireActivity(), "Registration Failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.d(TAG, "Exception: $e")
                }
            }, {
                loadingDialog.dismiss()
                Toast.makeText(requireActivity(), "Registration Failed", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["name"] = fullName
                params["email"] = emailAddress
                params["mobile_no"] = phoneNumber
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

    private fun gotoDashboard() {
        val spEditor = sharedPref?.edit()

        spEditor?.putString(getString(R.string.current_user_id), phoneNumber)
        spEditor?.apply()

        val action = VerifyCodeFragmentDirections.actionVerifyCodeFragmentToDashbordFragment()
        findNavController().navigate(action)
        loadingDialog.dismiss()
    }

    private fun gotoRegistration() {
        val action = VerifyCodeFragmentDirections.actionVerifyCodeFragmentToRegistrationFragment()
        findNavController().navigate(action)
    }
}