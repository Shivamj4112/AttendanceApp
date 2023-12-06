package com.pankajgaming.attendanceapp.Manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData

class NetworkManager(context : Context) : LiveData<Boolean>() {

    override fun onActive() {
        super.onActive()

        checkNetworkConnectivity()
    }

    override fun onInactive() {
        super.onInactive()
        connectinyManager.unregisterNetworkCallback(networkCallback)
    }

    private var connectinyManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback(){

        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            postValue(true)
        }

        override fun onUnavailable() {
            super.onUnavailable()

            postValue(false)
        }

        override fun onLost(network: Network) {
            super.onLost(network)

            postValue(false)
        }

    }

    private fun checkNetworkConnectivity(){

        val network = connectinyManager.activeNetwork
        if (network == null){
            postValue(false)
        }

        val requestBuilder = NetworkRequest.Builder().apply {
            addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        }.build()

        connectinyManager.registerNetworkCallback(requestBuilder,networkCallback)
    }
            
}