package com.emre.crisisresilience.data.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import android.os.DeadObjectException
import android.os.ParcelUuid
import android.util.Log
import com.emre.crisisresilience.data.model.MeshMessage
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"
    }

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val leScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val leAdvertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    // CrisisResilience özel servisi için benzersiz UUID
    private val CRISIS_SERVICE_UUID: UUID = UUID.fromString("C81D4E2E-BCE2-4E36-B15C-7609CACB4CB7")
    private val CRISIS_CHARACTERISTIC_UUID: UUID = UUID.fromString("D81D4E2E-BCE2-4E36-B15C-7609CACB4CB8")

    private val _discoveredDevices = MutableStateFlow<List<String>>(emptyList())
    val discoveredDevices: StateFlow<List<String>> = _discoveredDevices.asStateFlow()

    private val discoveredMacs = mutableSetOf<String>()

    private var gattServer: BluetoothGattServer? = null

    // Mesh mesajları artık MeshMessage nesneleri olarak tutuluyor
    private val _receivedMessages = MutableStateFlow<List<MeshMessage>>(emptyList())
    val receivedMessages: StateFlow<List<MeshMessage>> = _receivedMessages.asStateFlow()

    // Daha önce işlenmiş mesajları takip etmek için (deduplikasyon)
    private val processedMessageIds = mutableSetOf<String>()

    // Bu cihazın MAC adresini almak için (relay sırasında senderMac olarak kullanılır)
    val localMacAddress: String
        get() {
            val prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            var uuid = prefs.getString("device_uuid", "") ?: ""
            if (uuid.isEmpty()) {
                uuid = UUID.randomUUID().toString().take(8).uppercase() // 8 haneli benzersiz ID
                prefs.edit().putString("device_uuid", uuid).apply()
            }
            return uuid
        }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.address?.let { mac ->
                if (discoveredMacs.add(mac)) {
                    _discoveredDevices.value = discoveredMacs.toList()
                    Log.d(TAG, "Yeni BLE cihazı bulundu: $mac")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Tarama başarısız. Hata kodu: $errorCode")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "BLE Advertise başarıyla başlatıldı.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "BLE Advertise başlatılamadı. Hata kodu: $errorCode")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d(TAG, "GATT Server Bağlantı Durumu Değişti: $newState")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            if (characteristic?.uuid == CRISIS_CHARACTERISTIC_UUID) {
                value?.let {
                    val jsonStr = String(it, StandardCharsets.UTF_8)
                    val senderMac = device?.address ?: "Bilinmeyen MAC"
                    Log.d(TAG, "BLE üzerinden veri alındı (MAC: $senderMac): $jsonStr")

                    // JSON'dan MeshMessage'a dönüştür
                    val meshMessage = MeshMessage.fromJson(jsonStr)
                    if (meshMessage != null) {
                        // Deduplikasyon: Bu mesajı daha önce aldık mı?
                        if (processedMessageIds.add(meshMessage.messageId)) {
                            val currentList = _receivedMessages.value.toMutableList()
                            currentList.add(meshMessage)
                            _receivedMessages.value = currentList
                            Log.d(TAG, "Yeni MeshMessage alındı: id=${meshMessage.messageId}, hop=${meshMessage.hopCount}, payload=${meshMessage.payload}")
                        } else {
                            Log.d(TAG, "Duplike mesaj atlandı: id=${meshMessage.messageId}")
                        }
                    } else {
                        // Eski format (düz metin) uyumluluk — legacy fallback
                        val legacyMessage = MeshMessage(
                            originMac = senderMac,
                            senderMac = senderMac,
                            payload = jsonStr
                        )
                        if (processedMessageIds.add(legacyMessage.messageId)) {
                            val currentList = _receivedMessages.value.toMutableList()
                            currentList.add(legacyMessage)
                            _receivedMessages.value = currentList
                        }
                    }
                }
                if (responseNeeded) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "İzin eksik! GATT Yanıtı gönderilemedi.", e)
                    } catch (e: DeadObjectException) {
                        Log.e(TAG, "GATT Server öldü (DeadObjectException).", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "GATT Yanıtı gönderilirken hata.", e)
                    }
                }
            }
        }
    }

    private fun startGattServer() {
        if (bluetoothAdapter?.isEnabled != true) return
        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(CRISIS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic = BluetoothGattCharacteristic(
                CRISIS_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(characteristic)
            gattServer?.addService(service)
            Log.d(TAG, "GATT Server başlatıldı.")
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! GATT Server başlatılamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "GATT Server başlatılırken DeadObjectException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "GATT Server başlatılırken bilinmeyen hata.", e)
        }
    }

    fun startAdvertising() {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth kapalı, Advertise başlatılamıyor.")
            return
        }

        startGattServer()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(CRISIS_SERVICE_UUID))
            .build()

        try {
            leAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! BLE Advertise başlatılamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "Advertise başlatılırken DeadObjectException.", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Advertise başlatılırken IllegalStateException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Advertise başlatılırken hata.", e)
        }
    }

    fun stopAdvertising() {
        if (bluetoothAdapter?.isEnabled != true) return
        try {
            leAdvertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! BLE Advertise durdurulamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "Advertise durdurulurken DeadObjectException.", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Advertise durdurulurken IllegalStateException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Advertise durdurulurken bilinmeyen hata.", e)
        }

        try {
            gattServer?.close()
            gattServer = null
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! GATT Server kapatılamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "GATT Server kapatılırken DeadObjectException.", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "GATT Server kapatılırken IllegalStateException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "GATT Server kapatılırken hata.", e)
        }
    }

    fun startScanning() {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth kapalı, Tarama başlatılamıyor.")
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(CRISIS_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            leScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            Log.d(TAG, "BLE Tarama başlatıldı.")
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! BLE Tarama başlatılamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "Tarama başlatılırken DeadObjectException.", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Tarama başlatılırken IllegalStateException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Tarama başlatılırken hata.", e)
        }
    }

    fun stopScanning() {
        if (bluetoothAdapter?.isEnabled != true) return
        try {
            leScanner?.stopScan(scanCallback)
            Log.d(TAG, "BLE Tarama durduruldu.")
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! BLE Tarama durdurulamadı.", e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "Tarama durdurulurken DeadObjectException.", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Tarama durdurulurken IllegalStateException.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Tarama durdurulurken hata.", e)
        }
    }

    /**
     * Belirtilen MAC adresine GATT üzerinden MeshMessage gönderir.
     */
    fun sendMeshMessage(
        macAddress: String,
        meshMessage: MeshMessage,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth kapalı, GATT bağlantısı yapılamaz.")
            onFailure(IllegalStateException("Bluetooth kapalı"))
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(macAddress)
        if (device == null) {
            onFailure(IllegalArgumentException("Cihaz bulunamadı"))
            return
        }
        val jsonPayload = meshMessage.toJson()

        try {
            device.connectGatt(context, false, object : BluetoothGattCallback() {
                private var isCompleted = false

                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Cihaza bağlanıldı ($macAddress), MTU 512 talep ediliyor...")
                        try {
                            gatt?.requestMtu(512)
                        } catch (e: Exception) {
                            Log.e(TAG, "MTU talebi sırasında hata, servislere devam ediliyor.", e)
                            gatt?.discoverServices()
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "GATT bağlantısı koptu ($macAddress).")
                        safeGattClose(gatt)
                        if (!isCompleted) {
                            isCompleted = true
                            onFailure(Exception("Bağlantı koptu"))
                        }
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                    super.onMtuChanged(gatt, mtu, status)
                    Log.d(TAG, "MTU değişti: $mtu, durum: $status. Servisler keşfediliyor...")
                    try {
                        gatt?.discoverServices()
                    } catch (e: Exception) {
                        Log.e(TAG, "Servis keşfi sırasında hata.", e)
                        if (!isCompleted) {
                            isCompleted = true
                            onFailure(e)
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt?.getService(CRISIS_SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(CRISIS_CHARACTERISTIC_UUID)
                        if (characteristic != null) {
                            characteristic.value = jsonPayload.toByteArray(StandardCharsets.UTF_8)
                            try {
                                gatt.writeCharacteristic(characteristic)
                                Log.d(TAG, "GATT üzerinden MeshMessage yazıldı: ${meshMessage.payload} (hop=${meshMessage.hopCount})")
                            } catch (e: Exception) {
                                Log.e(TAG, "GATT Karakteristik yazılamadı.", e)
                                safeGattDisconnect(gatt)
                                if (!isCompleted) {
                                    isCompleted = true
                                    onFailure(e)
                                }
                            }
                        } else {
                            Log.e(TAG, "GATT Karakteristiği bulunamadı.")
                            safeGattDisconnect(gatt)
                            if (!isCompleted) {
                                isCompleted = true
                                onFailure(Exception("GATT Karakteristiği bulunamadı"))
                            }
                        }
                    } else {
                        Log.e(TAG, "GATT Servis keşfi başarısız: $status")
                        safeGattDisconnect(gatt)
                        if (!isCompleted) {
                            isCompleted = true
                            onFailure(Exception("Servis keşfi başarısız: $status"))
                        }
                    }
                }

                override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    safeGattDisconnect(gatt)
                    if (!isCompleted) {
                        isCompleted = true
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "MeshMessage başarıyla yazıldı, bağlantı kesiliyor.")
                            onSuccess()
                        } else {
                            Log.e(TAG, "Karakteristik yazma başarısız: $status")
                            onFailure(Exception("Karakteristik yazma hatası: $status"))
                        }
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "İzin eksik! GATT Bağlantısı kurulamadı.", e)
            onFailure(e)
        } catch (e: DeadObjectException) {
            Log.e(TAG, "GATT Bağlantısı sırasında DeadObjectException.", e)
            onFailure(e)
        } catch (e: Exception) {
            Log.e(TAG, "GATT Bağlantısı kurulamadı.", e)
            onFailure(e)
        }
    }

    /**
     * Alınan mesajı bilinen tüm BLE cihazlarına relay (sıçratma) yapar.
     * Kendi origin MAC'imize sahip mesajları ve zaten göndereni tekrar göndermeyiz.
     */
    fun relayMessageToAllPeers(meshMessage: MeshMessage) {
        if (!meshMessage.canRelay()) {
            Log.d(TAG, "Mesaj maksimum hop sayısına ulaştı, relay yapılmıyor: ${meshMessage.messageId}")
            return
        }

        val relayMessage = meshMessage.forRelay(localMacAddress)
        val peersToRelay = discoveredMacs.filter { mac ->
            mac != meshMessage.senderMac && mac != meshMessage.originMac
        }

        Log.d(TAG, "Mesaj relay ediliyor: id=${relayMessage.messageId}, hop=${relayMessage.hopCount}, hedef=${peersToRelay.size} cihaz")

        for (peerMac in peersToRelay) {
            sendMeshMessage(peerMac, relayMessage)
        }
    }

    /**
     * Eski API uyumluluğu ve doğrudan gönderim için.
     * İç olarak MeshMessage'a çevirir.
     */
    fun connectToDeviceAndSendMessage(
        macAddress: String,
        message: String,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        val meshMessage = MeshMessage(
            originMac = localMacAddress,
            senderMac = localMacAddress,
            payload = message
        )
        // Kendi mesajımızı da işlenmiş olarak işaretle (tekrar almamak için)
        processedMessageIds.add(meshMessage.messageId)
        sendMeshMessage(macAddress, meshMessage, onSuccess, onFailure)
    }

    private fun safeGattDisconnect(gatt: BluetoothGatt?) {
        try {
            gatt?.disconnect()
        } catch (e: DeadObjectException) {
            Log.e(TAG, "safeGattDisconnect: DeadObjectException", e)
        } catch (e: Exception) {
            Log.e(TAG, "safeGattDisconnect: Hata", e)
        }
    }

    private fun safeGattClose(gatt: BluetoothGatt?) {
        try {
            gatt?.close()
        } catch (e: DeadObjectException) {
            Log.e(TAG, "safeGattClose: DeadObjectException", e)
        } catch (e: Exception) {
            Log.e(TAG, "safeGattClose: Hata", e)
        }
    }
}
