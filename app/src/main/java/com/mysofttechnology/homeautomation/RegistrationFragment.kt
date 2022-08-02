package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.volley.toolbox.StringRequest
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.mysofttechnology.homeautomation.databinding.FragmentRegistrationBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "RegistrationFragment"

class RegistrationFragment : Fragment() {

    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exitAppDialog = ExitAppDialog()
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            exitAppDialog.show(childFragmentManager, "Exit App")
        }

        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.regLoginBtn.setOnClickListener {
            binding.regLoginBtn.isEnabled = false
            gotoLogin("")
        }
        binding.regRegisterBtn.setOnClickListener {
            binding.regRegisterBtn.isEnabled = false
            validateUserInputData()
        }
    }

    private fun gotoLogin(phoneNumber: String) {
        val action = RegistrationFragmentDirections.actionRegistrationFragmentToLoginFragment(phoneNumber)
        findNavController().navigate(action)
    }

    private fun validateUserInputData() {
        val fullName = binding.regFullName.text.toString().trim()
        val email = binding.regEmail.text.toString().trim()
        val phone = binding.regPhoneNo.text.toString().trim()

        val builder = AlertDialog.Builder(requireActivity())

        if (fullName.isNotBlank()) {
            if (email.isNotBlank()) {
                if (phone.isNotBlank()) {
                    if (phone.length == 10 && phone.isDigitsOnly()) {
                        builder.setTitle("Verify phone number")
                            .setMessage("We will send an SMS message to verify your phone number.")
                            .setPositiveButton("Ok"
                            ) { _, _ ->
                                loadingDialog.show(childFragmentManager, TAG)
                                checkUserData(fullName, email, phone)
                            }
                            .setNegativeButton("No") { _, _ -> }
                        builder.create()
                        builder.show()
                        binding.regRegisterBtn.isEnabled = true
                    } else binding.regPhoneNo.error = "Enter a proper phone number"
                } else binding.regPhoneNo.error = "Phone number is required"
            } else binding.regEmail.error = "Email address is required"
        } else binding.regFullName.error = "Full name is required"
    }

    private fun checkUserData(fullName: String, email: String, phone: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_profile)

        val builder = AlertDialog.Builder(requireActivity())

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        loadingDialog.dismiss()

                        builder.setMessage("$phone is already registered. Please login to continue.")
                            .setPositiveButton("Login"
                            ) { _, _ ->
                                gotoLogin(phone)
                            }
                            .setNeutralButton("Cancel") { _, _ -> }
                        builder.create()
                        builder.show()

                        Log.d(TAG, "checkUserData: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        registerUser(fullName, email, phone)
                        Log.d(TAG, "checkUserData: Message - $msg")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Exception in checkUserData: $e")
                    if (e.message != null) Toast.makeText(requireActivity(), e.message, Toast.LENGTH_SHORT).show()
                }
            }, {
                loadingDialog.dismiss()
                Toast.makeText(requireActivity(), "Something went wrong.", Toast.LENGTH_SHORT).show()
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

    private fun registerUser(fullName: String, email: String, phoneNumber: String) {
        // inform user that they might receive an SMS message for verification and standard rates apply

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted:$credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.e(TAG, "onVerificationFailed: Invalid request", e)
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.e(TAG, "onVerificationFailed: The SMS quota for the project has been exceeded", e)
                } else {
                    Toast.makeText(requireActivity(), "Verification failed.", Toast.LENGTH_SHORT)
                        .show()
                    Log.e(TAG, "onVerificationFailed: ", e)
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Toast.makeText(requireActivity(), "The SMS verification code has been sent to the provided phone number.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token

                val action =
                    RegistrationFragmentDirections.actionRegistrationFragmentToVerifyCodeFragment(
                        verificationId, fullName, email, phoneNumber, 1)
                findNavController().navigate(action)
                loadingDialog.dismiss()
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

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val action =
                RegistrationFragmentDirections.actionRegistrationFragmentToDashbordFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}