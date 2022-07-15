package com.mysofttechnology.homeautomation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.mysofttechnology.homeautomation.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginBackBtn.setOnClickListener {
            binding.loginBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        binding.loginRegisterBtn.setOnClickListener {
            binding.loginRegisterBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        binding.loginForgotPwBtn.setOnClickListener {
            binding.loginForgotPwBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

    }
}