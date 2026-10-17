package xyz.d1n0.ui.screen.pairing

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.juul.kable.Peripheral
import xyz.d1n0.Log
import xyz.d1n0.lib.connection.watchBluetoothDevice
import xyz.d1n0.lib.constant.BleUuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@Composable
actual fun rememberWatchPicker(onPicked: (Peripheral) -> Unit): (() -> Unit)? {
	val context = LocalContext.current
	val currentOnPicked by rememberUpdatedState(onPicked)

	val launcher = rememberLauncherForActivityResult(
		ActivityResultContracts.StartIntentSenderForResult()
	) { result ->
		if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
		result.data?.pickedDevice(context)
			?.let { currentOnPicked(Peripheral(it)) }
			?: Log.e { "Picked watch missing from the pairing result" }
	}

	return remember(context, launcher) {
		{
			context.associateWatch { intentSender ->
				launcher.launch(IntentSenderRequest.Builder(intentSender).build())
			}
		}
	}
}

/**
 * Asks the Companion Device Manager to find watches advertising the Casio service. The system
 * shows its dialog through [onPending]. Picking a watch creates an association, which is what
 * lets the app observe the watch's presence later.
 */
@OptIn(ExperimentalUuidApi::class)
private fun Context.associateWatch(onPending: (IntentSender) -> Unit) {
	val request = AssociationRequest.Builder()
		.addDeviceFilter(
			BluetoothLeDeviceFilter.Builder()
				.setScanFilter(
					ScanFilter.Builder()
						.setServiceUuid(ParcelUuid(BleUuid.CASIO_SERVICE_UUID.toJavaUuid()))
						.build()
				)
				.build()
		)
		.build()

	val callback = object : CompanionDeviceManager.Callback() {
		override fun onAssociationPending(intentSender: IntentSender) = onPending(intentSender)

		override fun onFailure(error: CharSequence?) {
			Log.e { "Watch pairing failed: $error" }
		}
	}

	getSystemService(CompanionDeviceManager::class.java).associate(request, mainExecutor, callback)
}

/**
 * Reads the picked watch from the pairing dialog result.
 */
private fun Intent.pickedDevice(context: Context): BluetoothDevice? {
	val association = getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
	val scannedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
		association?.associatedDevice?.bleDevice?.device
	} else {
		null
	}
	return scannedDevice ?: association?.deviceMacAddress?.let { context.watchBluetoothDevice(it.toString()) }
}
