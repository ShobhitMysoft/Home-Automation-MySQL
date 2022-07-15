package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.mysofttechnology.homeautomation.LoadingDialog.Companion.LOADING_DIALOG
import com.mysofttechnology.homeautomation.databinding.FragmentRegistrationBinding
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
            Navigation.findNavController(it)
                .navigate(R.id.action_registrationFragment_to_loginFragment)
        }
        binding.regRegisterBtn.setOnClickListener {
            binding.regRegisterBtn.isEnabled = false
            validateUserInputData()
//            Navigation.findNavController(it).navigate(R.id.action_registrationFragment_to_dashbordFragment)
        }
    }

    private fun validateUserInputData() {
        val fullName = binding.regFullName.text.toString().trim()
        val email = binding.regEmail.text.toString().trim()
        val phone = binding.regPhoneNo.text.toString().trim()

        val builder = AlertDialog.Builder(requireActivity())

        // TODO: Check if username is available or not
        // TODO: Check if phone number is available or not

        if (fullName.isNotBlank()) {
            if (email.isNotBlank()) {
                if (phone.isNotBlank()) {
                    if (phone.length == 10 && phone.isDigitsOnly()) {
                        builder.setTitle("Verify phone number")
                            .setMessage("We will send an SMS message to verify your phone number.")
                            .setPositiveButton("Ok"
                            ) { _, _ ->
//                                                progressBar.visibility = View.VISIBLE
                                loadingDialog.show(childFragmentManager, LOADING_DIALOG)
                                registerUser(fullName, email, phone)
                            }
                            .setNegativeButton("No") { _, _ -> }
                        // Create the AlertDialog object and return it
                        builder.create()
                        builder.show()
                    } else binding.regPhoneNo.error = "Enter a proper phone number"
                } else binding.regPhoneNo.error = "Phone number is required"
            } else binding.regEmail.error = "Email address is required"
        } else binding.regFullName.error = "Full name is required"
    }

    private fun registerUser(fullName: String, email: String, phoneNumber: String) {
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
                    RegistrationFragmentDirections.actionRegistrationFragmentToVerifyCodeFragment(
                        verificationId, fullName, email, phoneNumber, 1)
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

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")

                val user = it.result?.user
            } else {
                // Sign in failed, display a message and update the UI
                Log.w(TAG, "signInWithCredential:failure", it.exception)
                if (it.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                }
                // Update UI
            }
        }
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
}