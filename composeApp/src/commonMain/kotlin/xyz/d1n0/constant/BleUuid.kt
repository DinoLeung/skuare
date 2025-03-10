package xyz.d1n0.constant

import com.juul.kable.Bluetooth
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object BleUuid {
	// 00001804-0000-1000-8000-00805f9b34fb
	val CASIO_SERVICE_UUID = Bluetooth.BaseUuid + 0x1804
	val ALL_FEATURES_CHARACTERISTIC_UUID = Uuid.parse("26eb000d-b012-49a8-b1f8-394fb2032b0f")
	val REQUEST_CHARACTERISTIC_UUID = Uuid.parse("26eb002c-b012-49a8-b1f8-394fb2032b0f")
	val IO_CHARACTERISTIC_UUID = Uuid.parse("26eb002d-b012-49a8-b1f8-394fb2032b0f")
}
