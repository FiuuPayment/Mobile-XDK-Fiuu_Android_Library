package com.fiuu.xdkandroid.platform

import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time

/**
 * iOS implementation of [ImageSaver] utilizing the native iOS Share Sheet ([UIActivityViewController]).
 *
 * Presenting the system share sheet allows the user to:
 * - Save directly to the device's photo album ("Save Image")
 * - Save to iCloud Drive / On My iPhone ("Save to Files")
 * - AirDrop, Copy, or Print the slip image
 *
 * Architectural & Privacy Benefits:
 * - **Zero Info.plist Entitlements:** Out-of-process system sheet eliminates the requirement for
 *   merchants to declare `NSPhotoLibraryAddUsageDescription` or `NSPhotoLibraryUsageDescription`.
 * - **Thread Safety:** Marshals all UIKit presentation operations to the Main Queue via [dispatch_async]
 *   and [dispatch_get_main_queue], preventing Main Thread Checker violations.
 * - **No Invasive App Redirection:** Keeps the customer directly within the checkout flow instead of
 *   jumping to external photo viewer apps.
 */
class IosImageSaver(
    private val viewControllerProvider: (() -> UIViewController?)? = null
) : ImageSaver {

    @OptIn(ExperimentalForeignApi::class)
    override fun saveImage(bytes: ByteArray, fileName: String, onResult: (Boolean, String?) -> Unit) {
        val data = bytes.toNSData()
        val image = UIImage.imageWithData(data)
        if (image == null) {
            FiuuLogger.log("IosImageSaver: Failed to decode captured image data (bytes size=${bytes.size})")
            onResult(false, "Failed to decode captured image data")
            return
        }

        // Write image to a temporary file URL so that out-of-process system extensions
        // (SaveToCameraRoll, Save to Files, AirDrop) receive a sandbox-extended file URL
        // rather than failing with "unable to make sandbox extension: [22: Invalid argument]".
        val tempDir = platform.Foundation.NSTemporaryDirectory()
        val cleanFileName = if (fileName.endsWith(".png", ignoreCase = true)) fileName else "$fileName.png"
        val tempPath = tempDir + cleanFileName
        val fileUrl = platform.Foundation.NSURL.fileURLWithPath(tempPath)
        val written = platform.Foundation.NSFileManager.defaultManager.createFileAtPath(tempPath, contents = data, attributes = null)
        FiuuLogger.log("IosImageSaver: Wrote ${bytes.size} bytes to temp file $tempPath (success=$written)")

        val sema = dispatch_semaphore_create(0)
        var presentationSuccess = false
        var presentationError: String? = null

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val rootVc = viewControllerProvider?.invoke()
                    ?: UIApplication.sharedApplication.keyWindow?.rootViewController
                    ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.rootViewController

                val presenter = findTopViewController(rootVc)
                if (presenter == null) {
                    val msg = "No active UIViewController found to present share sheet"
                    FiuuLogger.log("IosImageSaver: $msg")
                    presentationSuccess = false
                    presentationError = msg
                    dispatch_semaphore_signal(sema)
                    return@dispatch_async
                }

                // Use an activity item source so that:
                // 1. "Save Image" (SaveToCameraRoll) receives the in-memory UIImage and writes to Photos.
                // 2. "Save to Files", AirDrop, etc. receive the fileUrl with the proper filename (fiuu_<orderId>.png).
                val itemSource = SlipActivityItemSource(image, fileUrl)
                val activityVC = UIActivityViewController(
                    activityItems = listOf(itemSource),
                    applicationActivities = null
                )

                // Configure popover anchor for iPad to prevent UIPopoverPresentationControllerSourceMissingException
                activityVC.popoverPresentationController?.let { popover ->
                    popover.sourceView = presenter.view
                    popover.sourceRect = presenter.view.bounds
                    popover.permittedArrowDirections = 0u
                }

                activityVC.completionWithItemsHandler = { activityType, completed, _, activityError ->
                    if (activityError != null) {
                        FiuuLogger.log("IosImageSaver: Share sheet error: ${activityError.localizedDescription}")
                    } else if (completed) {
                        FiuuLogger.log("IosImageSaver: Slip saved/shared via activity: $activityType")
                    } else {
                        FiuuLogger.log("IosImageSaver: Share sheet dismissed by user")
                    }
                }

                presenter.presentViewController(activityVC, animated = true) {
                    presentationSuccess = true
                    dispatch_semaphore_signal(sema)
                }
            } catch (e: Throwable) {
                val msg = e.message ?: "Failed to present share sheet"
                FiuuLogger.log("IosImageSaver: Exception presenting share sheet: $msg")
                presentationSuccess = false
                presentationError = msg
                dispatch_semaphore_signal(sema)
            }
        }

        // Wait up to 3 seconds for presentation to start on the main queue
        val timeout = dispatch_time(DISPATCH_TIME_NOW, 3_000_000_000L)
        if (dispatch_semaphore_wait(sema, timeout) != 0L) {
            FiuuLogger.log("IosImageSaver: Timeout waiting for share sheet presentation")
            onResult(false, "Timeout opening share sheet")
            return
        }

        onResult(presentationSuccess, presentationError)
    }

    private fun findTopViewController(root: UIViewController?): UIViewController? {
        var current = root
        while (current?.presentedViewController != null && !current.presentedViewController!!.isBeingDismissed()) {
            current = current.presentedViewController
        }
        return current
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

private class SlipActivityItemSource(
    private val image: platform.UIKit.UIImage,
    private val fileUrl: platform.Foundation.NSURL
) : platform.darwin.NSObject(), platform.UIKit.UIActivityItemSourceProtocol {

    override fun activityViewControllerPlaceholderItem(activityViewController: platform.UIKit.UIActivityViewController): Any {
        return image
    }

    override fun activityViewController(
        activityViewController: platform.UIKit.UIActivityViewController,
        itemForActivityType: platform.UIKit.UIActivityType?
    ): Any? {
        return if (itemForActivityType == platform.UIKit.UIActivityTypeSaveToCameraRoll) {
            image
        } else {
            fileUrl
        }
    }
}
