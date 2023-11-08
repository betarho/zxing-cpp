/*
* Copyright 2021 Axel Waggershauser
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.zxingcpp

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.ImageProxy
import java.lang.RuntimeException
import java.nio.ByteBuffer

public class BarcodeReader {
    private val supportedYUVFormats: List<Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
        } else {
            listOf(ImageFormat.YUV_420_888)
        }

    init {
        System.loadLibrary("zxing_android")
    }

    // Enumerates barcode formats known to this package.
    // Note that this has to be kept synchronized with native (C++/JNI) side.
    public enum class Format {
        NONE, AZTEC, CODABAR, CODE_39, CODE_93, CODE_128, DATA_BAR, DATA_BAR_EXPANDED,
        DATA_MATRIX, EAN_8, EAN_13, ITF, MAXICODE, PDF_417, QR_CODE, MICRO_QR_CODE, UPC_A, UPC_E
    }

    public enum class ContentType {
        TEXT, BINARY, MIXED, GS1, ISO15434, UNKNOWN_ECI
    }

    public enum class Binarizer {
        LOCAL_AVERAGE, GLOBAL_HISTOGRAM, FIXED_THRESHOLD, BOOL_CAST
    }

    public enum class EanAddOnSymbol {
        IGNORE, READ, REQUIRE
    }

    public enum class TextMode {
        PLAIN, ECI, HRI, HEX, ESCAPED
    }

    public data class Options(
        val formats: Set<Format> = setOf(),
        val tryHarder: Boolean = false,
        val tryRotate: Boolean = false,
        val tryInvert: Boolean = false,
        val tryDownscale: Boolean = false,
        val isPure: Boolean = false,
        val tryCode39ExtendedMode: Boolean = false,
        val validateCode39CheckSum: Boolean = false,
        val validateITFCheckSum: Boolean = false,
        val returnCodabarStartEnd: Boolean = false,
        val returnErrors: Boolean = false,
        val downscaleFactor: Int = 3,
        val eanAddOnSymbol: EanAddOnSymbol = EanAddOnSymbol.IGNORE,
        val binarizer: Binarizer = Binarizer.LOCAL_AVERAGE,
        val textMode: TextMode = TextMode.HRI,
        val minLineCount: Int = 2,
        val maxNumberOfSymbols: Int = 0xff,
        val downscaleThreshold: Int = 500
    )

    public data class Position(
        val topLeft: Point,
        val topRight: Point,
        val bottomLeft: Point,
        val bottomRight: Point,
        val orientation: Double
    )

    public data class Result(
        val format: Format = Format.NONE,
        val bytes: ByteArray? = null,
        val text: String? = null,
        val time: String? = null, // for development/debug purposes only
        val contentType: ContentType = ContentType.TEXT,
        val position: Position? = null,
        val orientation: Int = 0,
        val ecLevel: String? = null,
        val symbologyIdentifier: String? = null
    )

    public var options: Options = Options()

    public fun read(image: ImageProxy): List<Result>? {
        check(image.format in supportedYUVFormats) {
            "Invalid image format: ${image.format}. Must be one of: $supportedYUVFormats"
        }

        return readYBuffer(
            image.planes[0].buffer,
            image.planes[0].rowStride,
            image.cropRect.left,
            image.cropRect.top,
            image.cropRect.width(),
            image.cropRect.height(),
            image.imageInfo.rotationDegrees,
            options
        )
    }

    public fun read(bitmap: Bitmap, cropRect: Rect = Rect(), rotation: Int = 0): List<Result>? {
        return read(bitmap, options, cropRect, rotation)
    }

    public fun read(bitmap: Bitmap, options: Options, cropRect: Rect = Rect(), rotation: Int = 0): List<Result>? {
        return readBitmap(
            bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), rotation,
            options
        )
    }

    private external fun readYBuffer(
        yBuffer: ByteBuffer, rowStride: Int, left: Int, top: Int, width: Int, height: Int, rotation: Int,
        options: Options
    ): List<Result>?

    private external fun readBitmap(
        bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int, rotation: Int,
        options: Options
    ): List<Result>?
}
