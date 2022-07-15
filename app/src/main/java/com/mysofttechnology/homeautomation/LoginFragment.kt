package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.mysofttechnology.homeautomation.databinding.FragmentLoginBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "LoginFragment"
class LoginFragment : Fragment() {

    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        auth = FirebaseAuth.getInstance()

        binding.loginBackBtn.setOnClickListener {
            binding.loginBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        binding.loginRegisterBtn.setOnClickListener {
            binding.loginRegisterBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        binding.loginLoginBtn.setOnClickListener { validateUserInputData() }

    }

    private fun validateUserInputData() {
        val phone = binding.loginNumberEmail.text.toString().trim()

        val builder = AlertDialog.Builder(requireActivity())

        if (phone.isNotBlank()) {
            if (phone.length == 10 && phone.isDigitsOnly()) {
                builder.setTitle("Verify phone number")
                    .setMessage("We will send an SMS message to verify your phone number.")
                    .setPositiveButton("Ok"
                    ) { _, _ ->
//                                                progressBar.visibility = View.VISIBLE
                        loadingDialog.show(childFragmentManager, TAG)
                        checkUserData(phone)
                    }
                    .setNegativeButton("No") { _, _ -> }
                // Create the AlertDialog object and return it
                builder.create()
                builder.show()
            } else binding.loginNumberEmail.error = "Enter a proper phone number"
        } else binding.loginNumberEmail.error = "Phone number is required"
    }

    private fun checkUserData(phone: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_profile)

        /*val stringRequest = object : StringRequest(Method.GET, url,
            { response ->
                val mData = JSONObject(response)
                val resp = mData.get("response")
                val name = mData.get("name").toString()
                val email = mData.get("email").toString()

                if (resp == 1) {
                    loginUser(name, email,phone)
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(requireActivity(), "No user found. Please register first.",
                        Toast.LENGTH_LONG).show()
                }
            },
            {
                loadingDialog.dismiss()
                Toast.makeText(requireActivity(), "Network Error!", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["mobile_no"] = phone
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        queue.add(stringRequest)*/

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        val name = mData.get("name").toString()
                        val email = mData.get("email").toString()
                        loginUser(name, email,phone)
                        Log.d(TAG, "checkUserData: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(requireActivity(), "No user found. Please register first.",
                            Toast.LENGTH_LONG).show()
                        Log.d(TAG, "checkUserData: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Exception: $e")
                }
            }, {
                loadingDialog.dismiss()
                Toast.makeText(requireActivity(), "Login Failed", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["mobile_no"] = phone
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

    private fun loginUser(fullName: String, email: String, phoneNumber: String) {
        // TODO:  inform user that they might receive an SMS message for verification and standard rates apply

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")
//                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token

                val action =
                    LoginFragmentDirections.actionLoginFragmentToVerifyCodeFragment(
                        verificationId, fullName, email, phoneNumber, 2)
                findNavController().navigate(action)
//                progressBar.visibility = View.GONE
                loadingDialog.dismiss()
//                Navigation.findNavController().navigate(R.id.action_registrationFragment_to_verifyCodeFragment)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")              // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS)       // Timeout and unit
            .setActivity(requireActivity())                 // Activity (for callback binding)
            .setCallbacks(callbacks)                        // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}