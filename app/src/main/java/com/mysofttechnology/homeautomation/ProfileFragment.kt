package com.mysofttechnology.homeautomation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mysofttechnology.homeautomation.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val bind get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = db.getReference("root/users/$cuPhoneNo/profile")

        bind.profileBackBtn.setOnClickListener {
            bind.profileBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_profileFragment_to_dashbordFragment)
        }

        dbRef.apply {
            child("fullName").get().addOnSuccessListener { bind.fullName.text = it.value.toString() }
            child("emailAddress").get().addOnSuccessListener { bind.emailAddress.text = it.value.toString() }
            child("username").get().addOnSuccessListener { bind.userName.text = it.value.toString() }
            child("phoneNumber").get().addOnSuccessListener { bind.phoneNumber.text = it.value.toString() }
        }
    }
}