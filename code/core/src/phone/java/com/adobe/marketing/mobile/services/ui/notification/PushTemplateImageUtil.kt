/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.services.ui.notification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import com.adobe.marketing.mobile.services.caching.CacheService
import com.adobe.marketing.mobile.util.UrlUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility functions to assist in downloading and caching images for push template notifications.
 */

internal object PushTemplateImageUtil {
    private const val SELF_TAG = "PushTemplateImageUtil"
    private const val FULL_BITMAP_QUALITY = 100
    private const val DOWNLOAD_TIMEOUT_SECS = 10

    /**
     * Downloads an image using the provided url `String`. Prior to downloading, the image url
     * is used to retrieve a [CacheResult] containing a previously cached image. If no cache
     * result is returned, a call to [download] is made to download then cache the image.
     *
     * If a valid cache result is returned then no image is downloaded.
     *
     * @param cacheService the AEPSDK [CacheService] to use for caching downloaded image assets
     * @param urlList [String] containing an image asset url
     * @return [Int] number of images that were found in cache or successfully downloaded
     */
    internal fun downloadImage(
        cacheService: CacheService,
        urlList: List<String?>
    ): Int {
        val assetCacheLocation = getAssetCacheLocation()
        if (urlList.isEmpty() || assetCacheLocation.isNullOrEmpty()) {
            return 0
        }

        var downloadedImageCount = 0
        val latch = CountDownLatch(urlList.size)
        for (url in urlList) {
            if (url == null || !UrlUtils.isValidUrl(url)) {
                latch.countDown()
                continue
            }

            val cacheResult = cacheService[assetCacheLocation, url]
            if (cacheResult != null) {
                Log.trace(
                    PushTemplateConstants.LOG_TAG,
                    SELF_TAG,
                    "Found cached image for $url"
                )
                downloadedImageCount++
                latch.countDown()
                continue
            }

            download(url) { image ->
                image?.let {
                    Log.trace(
                        PushTemplateConstants.LOG_TAG,
                        SELF_TAG,
                        "Successfully download image from $url"
                    )
                    downloadedImageCount++

                    // scale down the bitmap to 300dp x 200dp as we don't want to use a full
                    // size image due to memory constraints
                    val pushImage = scaleBitmap(image)
                    // write bitmap to cache
                    try {
                        bitmapToInputStream(pushImage).use { bitmapInputStream ->
                            cacheBitmapInputStream(
                                cacheService,
                                bitmapInputStream,
                                url
                            )
                        }
                    } catch (exception: IOException) {
                        Log.trace(
                            PushTemplateConstants.LOG_TAG,
                            SELF_TAG,
                            "Exception occurred creating an input stream from a bitmap for {$url}: ${exception.localizedMessage}."
                        )
                    }
                }
                latch.countDown()
            }
        }
        latch.await(DOWNLOAD_TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
        return downloadedImageCount
    }

    /**
     * Downloads an image using the provided url `String`.
     *
     * @param url [String] containing the image url to download
     * @param completionCallback callback to be invoked with the downloaded [Bitmap]
     * when image specified by the given url is downloaded
     */
    internal fun download(
        url: String,
        completionCallback: (Bitmap?) -> Unit
    ) {
        val networkRequest = NetworkRequest(
            url,
            HttpMethod.GET,
            null,
            null,
            DOWNLOAD_TIMEOUT_SECS,
            DOWNLOAD_TIMEOUT_SECS
        )

        val networkCallback = NetworkCallback { connection: HttpConnecting? ->
            val image = handleDownloadResponse(url, connection)
            connection?.close()
            completionCallback.invoke(image)
        }

        ServiceProvider.getInstance()
            .networkService
            .connectAsync(networkRequest, networkCallback)
    }

    /**
     * Retrieves an image from the cache using the provided url `String`.
     *
     * @param cacheService the AEPSDK [CacheService] to use for caching downloaded image assets
     * @param url [String] containing the image url to retrieve from cache
     * @return [Bitmap] containing the image retrieved from cache, or `null` if no image is found
     */
    internal fun getImageFromCache(cacheService: CacheService, url: String?): Bitmap? {
        val assetCacheLocation = getAssetCacheLocation()
        if (assetCacheLocation.isNullOrEmpty() || url.isNullOrEmpty()) {
            return null
        }
        val cacheResult = cacheService[assetCacheLocation, url]
        if (cacheResult != null) {
            Log.trace(PushTemplateConstants.LOG_TAG, SELF_TAG, "Found cached image for $url")
            return BitmapFactory.decodeStream(cacheResult.data)
        }
        Log.trace(PushTemplateConstants.LOG_TAG, SELF_TAG, "Image not found in cache for $url")
        return null
    }

    private fun handleDownloadResponse(url: String?, connection: HttpConnecting?): Bitmap? {
        if (connection == null) {
            Log.warning(
                PushTemplateConstants.LOG_TAG,
                SELF_TAG,
                "Failed to download push notification image from url ($url), received a null connection."
            )
            return null
        }
        if ((connection.responseCode != HttpURLConnection.HTTP_OK)) {
            Log.debug(
                PushTemplateConstants.LOG_TAG,
                SELF_TAG,
                "Failed to download push notification image from url ($url). Response code was: ${connection.responseCode}."
            )
            return null
        }
        val bitmap = BitmapFactory.decodeStream(connection.inputStream)
        bitmap?.let {
            Log.trace(
                PushTemplateConstants.LOG_TAG,
                SELF_TAG,
                "Downloaded push notification image from url ($url)"
            )
        }
        return bitmap
    }

    /**
     * Converts a [Bitmap] into an [InputStream] to be used in caching images.
     *
     * @param bitmap [Bitmap] to be converted into an [InputStream]
     * @return an `InputStream` created from the provided bitmap
     */
    private fun bitmapToInputStream(bitmap: Bitmap): InputStream {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, FULL_BITMAP_QUALITY, byteArrayOutputStream)
        val bitmapData = byteArrayOutputStream.toByteArray()
        return ByteArrayInputStream(bitmapData)
    }

    /**
     * Writes the provided [InputStream] to the downloaded push template image [assetCacheLocation].
     *
     * @param cacheService [CacheService] the AEPSDK cache service
     * @param bitmapInputStream [InputStream] created from a download [Bitmap]
     * @param imageUrl [String] containing the image url to be used a cache key
     */
    private fun cacheBitmapInputStream(
        cacheService: CacheService,
        bitmapInputStream: InputStream,
        imageUrl: String
    ) {
        Log.trace(
            PushTemplateConstants.LOG_TAG,
            SELF_TAG,
            "Caching image downloaded from $imageUrl."
        )
        getAssetCacheLocation()?.let {
            // cache push notification images for 3 days
            val cacheEntry = CacheEntry(
                bitmapInputStream,
                CacheExpiry.after(
                    PushTemplateConstants.DefaultValues.PUSH_NOTIFICATION_IMAGE_CACHE_EXPIRY_IN_MILLISECONDS
                ),
                null
            )
            cacheService[it, imageUrl] = cacheEntry
        }
    }

    /**
     * Scales a downloaded [Bitmap] to a maximum width and height of 300dp x 200dp.
     * The scaling is done using a [Matrix] object to maintain the aspect ratio of the original
     * image.
     *
     * @param downloadedBitmap [Bitmap] to be scaled
     * @return [Bitmap] containing the scaled image
     */
    private fun scaleBitmap(downloadedBitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(0f, 0f, downloadedBitmap.width.toFloat(), downloadedBitmap.height.toFloat()),
            RectF(
                0f,
                0f,
                PushTemplateConstants.DefaultValues.CAROUSEL_MAX_BITMAP_WIDTH.toFloat(),
                PushTemplateConstants.DefaultValues.CAROUSEL_MAX_BITMAP_HEIGHT.toFloat()
            ),
            Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(
            downloadedBitmap,
            0,
            0,
            downloadedBitmap.width,
            downloadedBitmap.height,
            matrix,
            true
        )
    }

    /**
     * Retrieves the asset cache location to use for downloaded push template images.
     *
     * @return [String] containing the asset cache location to use for storing downloaded push template images.
     */
    internal fun getAssetCacheLocation(): String? {
        val deviceInfoService = ServiceProvider.getInstance().deviceInfoService
            ?: return null
        val applicationCacheDir = deviceInfoService.applicationCacheDir
        return if ((applicationCacheDir == null)) null else (
            (
                applicationCacheDir
                    .toString() + File.separator +
                    PushTemplateConstants.CACHE_BASE_DIR
                ) + File.separator +
                PushTemplateConstants.PUSH_IMAGE_CACHE
            )
    }
}
