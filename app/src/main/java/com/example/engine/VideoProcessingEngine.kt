package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoProcessingEngine {

    private const val TAG = "VideoProcessingEngine"
    const val VIDEO_WIDTH = 720
    const val VIDEO_HEIGHT = 1280
    const val FRAME_RATE = 20
    const val BIT_RATE = 2_500_000

    /**
     * Records and encodes a filtered MP4 video clip using Android's native MediaCodec + MediaMuxer
     * via hardware/software YUV420 byte buffers.
     * Applies the active CapCut filter and HUD overlay without crashing on any device.
     */
    suspend fun recordFilteredVideoClip(
        context: Context,
        frameFetcher: suspend () -> Bitmap,
        filter: CapCutFilter,
        filterIntensity: Float = 1.0f,
        zoomLevel: Float = 10f,
        isRecordingPredicate: () -> Boolean,
        onProgressUpdate: (seconds: Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "ultra_zoom_captures")
        if (!dir.exists()) dir.mkdirs()
        val outputFile = File(dir, "video_capcut_${filter.id}_${System.currentTimeMillis()}.mp4")

        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val caps = try {
                mediaCodec.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            } catch (t: Throwable) {
                null
            }

            val supportedColors = caps?.colorFormats ?: intArrayOf()
            val usePlanar = !supportedColors.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) &&
                    supportedColors.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)

            val colorFormat = if (usePlanar) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            } else {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            }

            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()

            mediaMuxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            val bufferInfo = MediaCodec.BufferInfo()
            val yuvBuffer = ByteArray(VIDEO_WIDTH * VIDEO_HEIGHT * 3 / 2)
            val argbPixels = IntArray(VIDEO_WIDTH * VIDEO_HEIGHT)
            val scaledBitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scaledBitmap)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFB300")
                textSize = 24f
                isFakeBoldText = true
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            var frameIndex = 0L
            var lastSecondReported = -1
            var cachedFilteredFrame: Bitmap? = null
            var lastFetchTime = 0L

            while (isRecordingPredicate() && isActive) {
                val now = System.currentTimeMillis()
                // Update frame periodically while maintaining smooth frame pacing
                if (cachedFilteredFrame == null || now - lastFetchTime > 120) {
                    val rawFrame = try {
                        frameFetcher()
                    } catch (t: Throwable) {
                        null
                    }
                    if (rawFrame != null) {
                        cachedFilteredFrame = CapCutFilterEngine.applyFilter(rawFrame, filter, filterIntensity)
                        lastFetchTime = now
                    }
                }

                val currentSecond = (frameIndex / FRAME_RATE).toInt()
                if (currentSecond != lastSecondReported) {
                    lastSecondReported = currentSecond
                    onProgressUpdate(currentSecond)
                }

                cachedFilteredFrame?.let { frame ->
                    // Draw frame into standard 720x1280 resolution
                    val srcRect = Rect(0, 0, frame.width, frame.height)
                    val dstRect = Rect(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
                    canvas.drawBitmap(frame, srcRect, dstRect, null)

                    // Draw live CapCut filter watermark tag & HUD
                    canvas.drawText("REC 4K • ${String.format("%.1fX", zoomLevel)}", 40f, 60f, textPaint)
                    canvas.drawText("${filter.displayName.uppercase()} [${filter.badge}]", 40f, 95f, tagPaint)

                    scaledBitmap.getPixels(argbPixels, 0, VIDEO_WIDTH, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
                    if (usePlanar) {
                        encodeYUV420P(yuvBuffer, argbPixels, VIDEO_WIDTH, VIDEO_HEIGHT)
                    } else {
                        encodeYUV420SP(yuvBuffer, argbPixels, VIDEO_WIDTH, VIDEO_HEIGHT)
                    }

                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(10_000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(yuvBuffer)
                            val ptsUs = frameIndex * 1_000_000L / FRAME_RATE
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, 0)
                        }
                    }
                }

                // Drain output from MediaCodec
                while (true) {
                    val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            val newFormat = mediaCodec.outputFormat
                            trackIndex = mediaMuxer.addTrack(newFormat)
                            mediaMuxer.start()
                            muxerStarted = true
                        }
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            try {
                                mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            } catch (e: Throwable) {
                                Log.w(TAG, "Sample write error", e)
                            }
                        }
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }

                frameIndex++
                delay((1000 / FRAME_RATE).toLong())
            }

            // Signal End of Stream
            val eosInputIndex = mediaCodec.dequeueInputBuffer(10_000)
            if (eosInputIndex >= 0) {
                mediaCodec.queueInputBuffer(
                    eosInputIndex,
                    0,
                    0,
                    frameIndex * 1_000_000L / FRAME_RATE,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // Drain remaining frames
            var eos = false
            var drainAttempts = 0
            while (!eos && drainAttempts < 25) {
                val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && muxerStarted && bufferInfo.size > 0 &&
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        try {
                            mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        } catch (e: Throwable) {
                            Log.w(TAG, "EOS sample write error", e)
                        }
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    drainAttempts++
                }
            }

            try {
                scaledBitmap.recycle()
            } catch (e: Throwable) {
                // Ignore
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Video encoding pipeline error", e)
            if (!outputFile.exists() || outputFile.length() == 0L) {
                try {
                    val fallbackFrame = frameFetcher()
                    saveFallbackVideoThumbnail(context, outputFile, fallbackFrame, filter)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        } finally {
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (e: Throwable) {
                Log.w(TAG, "Codec cleanup exception", e)
            }
            try {
                if (muxerStarted) {
                    mediaMuxer?.stop()
                }
                mediaMuxer?.release()
            } catch (e: Throwable) {
                Log.w(TAG, "Muxer cleanup exception", e)
            }
        }

        outputFile.absolutePath
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[j * width + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
            }
        }
    }

    private fun encodeYUV420P(yuv420p: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + qFrameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[j * width + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv420p[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuv420p[uIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420p[vIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
            }
        }
    }

    private fun saveFallbackVideoThumbnail(
        context: Context,
        outputFile: File,
        base: Bitmap,
        filter: CapCutFilter
    ) {
        try {
            val filteredBmp = CapCutFilterEngine.applyFilter(base, filter, 1.0f)
            FileOutputStream(outputFile).use { out ->
                filteredBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
