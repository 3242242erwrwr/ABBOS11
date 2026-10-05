package com.example.xabarsos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class BluetoothSosManager(private val context: Context) {

    companion object {
        // Custom 128-bit UUID for XABARSOS Bluetooth Service
        val SERVICE_UUID: UUID = UUID.fromString("0000FA11-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BluetoothSosManager"
    }

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private val _isBluetoothSupported = MutableStateFlow(false)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private var onMessageReceivedListener: ((SosMessage) -> Unit)? = null

    init {
        _isBluetoothSupported.value = bluetoothAdapter != null
    }

    fun setOnMessageReceivedListener(listener: (SosMessage) -> Unit) {
        onMessageReceivedListener = listener
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startListeningForNearbySos() {
        if (!isBluetoothEnabled()) return

        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Bluetooth LE Scanner is not available")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Bluetooth BLE scanning started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopListening() {
        try {
            scanner?.stopScan(scanCallback)
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun broadcastSosOffline(senderName: String, messageText: String) {
        if (!isBluetoothEnabled()) return

        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "Bluetooth LE Advertiser is not available")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Combine sender and message: "Sender:Message" truncated if needed for BLE payload
        val payload = "$senderName:$messageText"
        val payloadBytes = payload.toByteArray(StandardCharsets.UTF_8)

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), payloadBytes)
            .setIncludeDeviceName(false)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            _isAdvertising.value = true
            Log.d(TAG, "Bluetooth BLE advertising SOS: $payload")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE advertising: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBroadcasting() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE advertising: ${e.message}")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "BLE Advertise success")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "BLE Advertise failure code: $errorCode")
            _isAdvertising.value = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val scanRecord = it.scanRecord ?: return@let
                val serviceData = scanRecord.getServiceData(ParcelUuid(SERVICE_UUID)) ?: return@let

                val fullDataStr = String(serviceData, StandardCharsets.UTF_8)
                val parts = fullDataStr.split(":", limit = 2)
                val sender = parts.getOrNull(0) ?: "Yaqindagi qurilma"
                val text = parts.getOrNull(1) ?: "SOS!"

                val message = SosMessage(
                    senderName = sender,
                    messageText = text,
                    channel = MessageChannel.BLUETOOTH,
                    isIncoming = true
                )

                onMessageReceivedListener?.invoke(message)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed code: $errorCode")
            _isScanning.value = false
        }
    }
}
