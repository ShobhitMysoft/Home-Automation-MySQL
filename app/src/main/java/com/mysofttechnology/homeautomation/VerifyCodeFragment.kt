package com.mysofttechnology.homeautomation

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mysofttechnology.homeautomation.databinding.FragmentVerifyCodeBinding

private const val TAG = "VerifyCodeFragment"
class VerifyCodeFragment : Fragment() {

    private var verificationId: String? = null
    private lateinit var fullName: String
    private lateinit var emailAddress: String
    private lateinit var username: String
    private lateinit var phoneNumber: String
    private lateinit var password: String

    private var _binding: FragmentVerifyCodeBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        arguments?.let {
            verificationId = it.getString("verificationID").toString()
            fullName = it.getString("fullName").toString()
            emailAddress = it.getString("emailAddress").toString()
            username = it.getString("username").toString()
            phoneNumber = it.getString("phoneNumber").toString()
            password = it.getString("password").toString()
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
        db = FirebaseDatabase.getInstance()
        dbRef = db.reference

//        binding.vcVerifyBtn.isEnabled = false
//        binding.vcVerifyBtn.alpha = .5f
//        binding.vcVerifyBtn.setBackgroundColor(Color.GRAY)

        binding.vcBackBtn.setOnClickListener {
            binding.vcBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_verifyCodeFragment_to_registrationFragment)
        }

//        binding.vcVerifyBtn.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onTextChanged(c: CharSequence?, start: Int, before: Int, count: Int) {
//                val code = binding.vcCodeEt.text.toString().trim()
//                Toast.makeText(activity, "$code | $c | $start | $before | $count", Toast.LENGTH_SHORT).show()
//                Log.d(TAG, "onTextChanged: $code | $c | $start | $before | $count")
//                if (code.length == 6) {
//                    Log.d(TAG, "onTextChanged: length == 6")
//                    binding.vcVerifyBtn.isEnabled = true
//                    binding.vcVerifyBtn.alpha = 1.0f
//                    binding.vcVerifyBtn.setBackgroundColor(resources.getColor(R.color.colorAccent))
//                } else {
//                    binding.vcVerifyBtn.isEnabled = false
//                    binding.vcVerifyBtn.alpha = .5f
//                    binding.vcVerifyBtn.setBackgroundColor(Color.GRAY)
//                }
//            }
//
//            override fun afterTextChanged(text: Editable?) {
////                if (text.toString().trim().length >= 6) {
////                    binding.vcVerifyBtn.isEnabled = true
////                    binding.vcVerifyBtn.alpha = 0f
////                } else {
////                    binding.vcVerifyBtn.isEnabled = false
////                    binding.vcVerifyBtn.alpha = .5f
////                }
//            }
//
//        })

        binding.vcVerifyBtn.setOnClickListener {
            binding.vcVerifyBtn.isEnabled = false
            val code = binding.vcCodeEt.text.toString().trim()

            if (code.isNotEmpty() && code.length == 6) {
                if (code.isDigitsOnly()) {
                    loadingDialog.show(childFragmentManager, LoadingDialog.LOADING_DIALOG)
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
                createDatabase()
            } else {
                // Sign in failed, display a message and update the UI
                Log.w(TAG, "signInWithCredential:failure", it.exception)
                if (it.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    Toast.makeText(requireActivity(), "Invalid code.", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(requireActivity(), "Invalid code or this number is already logged on another device.", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            }
        }
    }

    private fun createDatabase() {
        dbRef.child("root").child("users").child(phoneNumber).child("profile").apply {
            child("fullName").setValue(fullName)
            child("emailAddress").setValue(emailAddress)
            child("username").setValue(username)
            child("phoneNumber").setValue(phoneNumber)
            child("password").setValue(password)
            child("deviceCount").setValue("0")
        }

        val action = VerifyCodeFragmentDirections.actionVerifyCodeFragmentToDashbordFragment()
        findNavController().navigate(action)
        loadingDialog.dismiss()
    }
}