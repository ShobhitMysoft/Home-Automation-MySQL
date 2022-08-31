package com.mysofttechnology.homeautomation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mysofttechnology.homeautomation.databinding.FragmentScanQrAnimationBinding

class ScanQrAnimationFragment : Fragment() {

    private var _binding: FragmentScanQrAnimationBinding? = null
    private val bind get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentScanQrAnimationBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.scanQrContinueBtn.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.scanQrAnimationFragment)
                findNavController().navigate(R.id.action_scanQrAnimationFragment_to_scanDeviceFragment) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}