package com.github.dedis.popstellar.ui.qrcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.QrScannerFragmentBinding
import com.github.dedis.popstellar.databinding.ReusableTextInputLayoutBinding
import com.github.dedis.popstellar.ui.PopViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.hideKeyboard
import com.github.dedis.popstellar.utility.GeneralUtils
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import timber.log.Timber

class QrScannerFragment : Fragment() {
  private lateinit var binding: QrScannerFragmentBinding
  private var barcodeScanner: BarcodeScanner? = null
  private lateinit var scanningViewModel: QRCodeScanningViewModel
  private lateinit var popViewModel: PopViewModel
  private lateinit var clipboardManager: GeneralUtils.ClipboardUtil
  private lateinit var inputLayouts: Array<ReusableTextInputLayoutBinding>

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = QrScannerFragmentBinding.inflate(inflater, container, false)

    val scanningAction = scanningAction
    popViewModel = scanningAction.obtainPopViewModel(requireActivity())
    scanningViewModel = scanningAction.obtainScannerViewModel(requireActivity(), popViewModel.laoId)
    if (scanningAction.displayCounter) {
      displayCounter()
    }
    clipboardManager = GeneralUtils.ClipboardUtil(requireActivity())

    binding.scannedTitle.setText(scanningAction.scanTitle)
    binding.addManualTitle.setText(scanningAction.manualAddTitle)
    initializeInputLayouts()
    configureActionBasedInputs()
    binding.scannerInstructionText.setText(scanningAction.instruction)

    setupNbScanned()
    setupManualAdd()
    setupAllowCameraButton()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    val scanningAction = scanningAction
    popViewModel.setPageTitle(scanningAction.pageTitle)
    applyPermissionToView()

    // Handle back press navigation
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(this, scanningAction.onBackPressedCallback(parentFragmentManager, backArgs))
  }

  override fun onDestroy() {
    super.onDestroy()
    barcodeScanner?.close()
  }

  private val scanningAction: ScanningAction
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          requireArguments().getSerializable(SCANNING_KEY, ScanningAction::class.java)!!
        } else {
          // This is deprecated as of Android 13 but it'll be probably 2030 before it's our min SDK
          // noinspection deprecation
          requireArguments().getSerializable(SCANNING_KEY) as ScanningAction
        }

  private val backArgs: Array<String>
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          requireArguments().getSerializable(BACK_ARGS, Array<String>::class.java)!!
        } else {
          // This is deprecated as of Android 13 but it'll be probably 2030 before it's our min SDK
          // noinspection deprecation
          requireArguments().getSerializable(BACK_ARGS) as Array<String>
        }

  private fun setupNbScanned() {
    scanningViewModel.nbScanned.observe(viewLifecycleOwner) { nb: Int? ->
      binding.scannedNumber.text = nb.toString()
    }
  }

  private fun setupAllowCameraButton() {
    // Create request permission launcher which will ask for permission
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            requireActivity().activityResultRegistry) { _: Boolean? ->
              applyPermissionToView()
            } // This is the callback of the permission granter

    // The button launch the build launcher when is it clicked
    binding.allowCameraButton.setOnClickListener { _: View? ->
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  private fun applyPermissionToView() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED) {
      binding.cameraPermission.visibility = View.GONE
      binding.scannerInstructionText.visibility = View.VISIBLE
      binding.qrCodeSight.visibility = View.VISIBLE
      startCamera()
    } else {
      // the camera permission is not granted, make dedicated views visible
      binding.cameraPermission.visibility = View.VISIBLE
      binding.scannerInstructionText.visibility = View.GONE
      binding.qrCodeSight.visibility = View.GONE
    }
  }

  private fun startCamera() {
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    barcodeScanner = BarcodeScanning.getClient(options)

    val cameraController = LifecycleCameraController(requireContext())
    cameraController.bindToLifecycle(this)

    val executor = ContextCompat.getMainExecutor(requireContext())
    cameraController.setImageAnalysisAnalyzer(
        executor,
        MlKitAnalyzer(
            barcodeScanner?.let { listOf(it) } ?: emptyList(),
            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
            executor) { result: MlKitAnalyzer.Result ->
              val barcodes = barcodeScanner?.let { result.getValue(it) }
              if (!barcodes.isNullOrEmpty()) {
                Timber.tag(TAG).d("barcode raw value : %s", barcodes[0].rawValue)
                onResult(barcodes[0])
              }
            })
    binding.scannerCamera.controller = cameraController
  }

  private fun displayCounter() {
    binding.scannedTitle.visibility = View.VISIBLE
    binding.scannedNumber.visibility = View.VISIBLE
  }

  private fun onResult(barcode: Barcode) {
    onResult(barcode.rawValue)
  }

  private fun onResult(data: String?) {
    scanningViewModel.handleData(data)
  }

  /**
   * Initializes the input layouts
   * If you need more inputs for an action, you can add more TextInputLayouts in the qr_scanner_fragment.xml
   * Then you can add them to the inputLayouts array
   * TODO: need to find a better solution using a constant MAX_INPUT_FIELDS. findViewByID can't help to find inputLayout$i as it takes an int, not a string.
   * Maxime Teuber @kaz-ookid | May 2024
   */
  private fun initializeInputLayouts() {
    inputLayouts = arrayOf(
        binding.inputLayout1,
        binding.inputLayout2,
        binding.inputLayout3,
        binding.inputLayout4)
  }

  /**
   * Configures the input fields based on the scanning action
   * If the action requires less inputs than all of those available, the extra inputs will be hidden
   */
  private fun configureActionBasedInputs() {
    val action = scanningAction
    action.getInputFields().forEachIndexed { index, fieldConfig ->
      if (index < inputLayouts.size) {
        val layout = inputLayouts[index]
        val editText = layout.textInput
        val pasteButton = layout.pasteButton

        layout.container.visibility = View.VISIBLE
        editText.hint = fieldConfig.hint

        if (fieldConfig.needsPasteButton) {
          pasteButton.visibility = View.VISIBLE
          clipboardManager.setupPasteButton(pasteButton, editText)
        } else {
          pasteButton.visibility = View.GONE
        }
      }
    }

    for (i in action.getInputFields().size until inputLayouts.size) {
      inputLayouts[i].container.visibility = View.GONE
    }
  }

  private fun setupManualAdd() {
    binding.scannerEnterManually.setOnClickListener {
      binding.scannerBottomTexts.visibility = View.GONE
      binding.enterManuallyCard.visibility = View.VISIBLE
    }

    binding.addManualClose.setOnClickListener {
      hideKeyboard(requireContext(), binding.root)
      binding.scannerBottomTexts.visibility = View.VISIBLE
      binding.enterManuallyCard.visibility = View.GONE
    }

    binding.manualAddButton.setOnClickListener {
      val inputs = scanningAction.getInputFields().mapIndexed { index, config ->
        config.hint to inputLayouts[index].textInput.text.toString().trim()
      }.toMap()

      val json = scanningAction.formatJson(inputs)
      if (json.isNotBlank()) {
        onResult(json)
      } else {
        logAndShow(
          requireContext(),
          TAG,
          IllegalArgumentException("Required fields are empty"),
          R.string.qrcode_scanning_manual_entry_error
        )
      }
    }
  }

  companion object {
    val TAG: String = QrScannerFragment::class.java.simpleName
    const val SCANNING_KEY = "scanning_action_key"
    const val BACK_ARGS = "back_arguments"

    @JvmStatic
    fun newInstance(scanningAction: ScanningAction, vararg backArgs: String): QrScannerFragment {
      val fragment = QrScannerFragment()
      val bundle = Bundle(2)
      bundle.putSerializable(SCANNING_KEY, scanningAction)
      bundle.putSerializable(BACK_ARGS, backArgs)
      fragment.arguments = bundle

      return fragment
    }
  }
}
