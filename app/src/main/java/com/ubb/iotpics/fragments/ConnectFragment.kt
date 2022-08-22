package com.ubb.iotpics.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.ubb.iotpics.R
import com.ubb.iotpics.databinding.ConnectFragmentBinding
import com.ubb.iotpics.mqtt.*


class ConnectFragment : Fragment() {

    private var _binding: ConnectFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ConnectFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConnect.setOnClickListener {
            val serverURIFromEditText   = binding.edittextServerUri.text.toString()
            val clientIDFromEditText    = binding.edittextClientId.text.toString()
            val usernameFromEditText    = binding.edittextUsername.text.toString()
            val pwdFromEditText         = binding.edittextPassword.text.toString()

            val mqttCredentialsBundle = bundleOf(MQTT_SERVER_URI_KEY    to serverURIFromEditText,
                MQTT_CLIENT_ID_KEY     to clientIDFromEditText,
                MQTT_USERNAME_KEY      to usernameFromEditText,
                MQTT_PWD_KEY           to pwdFromEditText)


            findNavController().navigate(R.id.action_connectFragment_to_principalFragment,mqttCredentialsBundle)
        }


        binding.buttonPrefill.setOnClickListener {
            binding.edittextServerUri.setText(SERVER)

        }

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}