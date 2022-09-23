/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils

/**
 * Encode h264 by MediaCodec
 *
 * @property width yuv width
 * @property height yuv height
 * @property gLESRender rendered by opengl flag
 * @author Created by jiangdg on 2022/2/10
 */
class H264EncodeProcessor(
    val width: Int,
    val height: Int,
    private val gLESRender: Boolean = true
) : AbstractProcessor(gLESRender, width, height) {

    private var mFrameRate: Int? = null
    private var mBitRate: Int? = null
    private var mReadyListener: OnEncodeReadyListener? = null

    override fun getThreadName(): String = TAG

    override fun handleStartEncode() {
        try {
            val mediaFormat = MediaFormat.createVideoFormat(MIME, width, height)

//            if (width < 2000 && height < 2000) {
                // 分辨率低于2000，采用可变比特率模式，用于拍摄变动态画面
                mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
//            } else {
//                // 分辨率高于2000，采用恒定比特率模式，用于拍摄相对静态画面（例如：拍摄不可移动物体）
//                mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
//            }

            // 描述视频格式的帧速率（以帧/秒为单位）的键。帧率 15-30FPS
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
//            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate ?: FRAME_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate ?: getEncodeBitrate(width, height))
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate ?: calcBitRate(width,height))
//            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_FRAME_INTERVAL)

            var frameInterval = 1
            // 关键帧
            if (width < 2000 && height < 2000) {
                frameInterval = 8
            }
            // 关键帧时间间隔，即编码一次关键帧的时间间隔
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval)

            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getSupportColorFormat())
            mMediaCodec = MediaCodec.createEncoderByType(MIME)
            mMediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            if (gLESRender) {
                mReadyListener?.onReady(mMediaCodec?.createInputSurface())
            }
            mMediaCodec?.start()
            mEncodeState.set(true)
            if (Utils.debugCamera) {
                Logger.i(TAG, "init h264 media codec success.")
            }
            doEncodeData()
        } catch (e: Exception) {
            Logger.e(TAG, "start h264 media codec failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun handleStopEncode() {
        try {
            mEncodeState.set(false)
            mMediaCodec?.stop()
            mMediaCodec?.release()
            if (Utils.debugCamera) {
                Logger.i(TAG, "release h264 media codec success.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Stop mediaCodec failed, err = ${e.localizedMessage}", e)
        } finally {
            mRawDataQueue.clear()
            mMediaCodec = null
        }
    }

    override fun getPTSUs(bufferSize: Int): Long = System.nanoTime() / 1000L

    /**
     * Set on encode ready listener
     *
     * @param listener input surface ready listener
     */
    fun setOnEncodeReadyListener(listener: OnEncodeReadyListener) {
        this.mReadyListener = listener
    }

    private fun getSupportColorFormat(): Int {
        if (gLESRender) {
//            return MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bitcompressed
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun getEncodeBitrate(width: Int, height: Int): Int {
        var bitRate = width * height * 20 * 3 * 0.07F
        if (width >= 1920 || height >= 1920) {
//            bitRate = 2080 * 1024 * 1F
//            bitRate = 3150 * 1024F
//            bitRate *= 0.75F
            //bitRate *=0.3F
            bitRate *=0.25F
        } else if (width >= 1280 || height >= 1280) {
//            bitRate *= 1.2F
            bitRate *= 0.33F
//            bitRate = 1130 * 1024 * 1F
        } else if (width >= 640 || height >= 640) {
//            bitRate *= 1.4F
            bitRate *= 0.4F
//            bitRate = 500 * 1024 * 1F   //0.4
//            bitRate = 256 * 1024 * 1F
        }

        return bitRate.toInt()
    }

    /**
     * BIT_RATE 没有什么太大的坑，注意下单位就好 bits/seconds. 下面的图展示了，官方要求设备对于不同分辨率下需要支持的最低分辨率。
     */
    private fun calcBitRate(mWidth: Int, mHeight: Int): Int {
        if (mWidth < 1000 && mHeight < 1000) {
            return 1920 * 1080 / 25 // 1000以下（控制1/4）
        }
        if (mWidth < 2000 && mHeight < 2000) {
            return 1920 * 1080 / 120 // 1000-2000之间（普通帧：1000-3000 关键帧：10000-40000 关：2/s）ok
        }
        if (mWidth < 3000 && mHeight < 3000) {
            return 1920 * 1080 / 200
        }
        return if (mWidth > 3000 || mHeight > 3000) {
            640 * 480 / 30
        } else 0
    }

    /**
     * Set encode rate
     *
     * @param bitRate encode bit rate, kpb/s
     * @param frameRate encode frame rate, fp/s
     */
    fun setEncodeRate(bitRate: Int?, frameRate: Int?) {
        this.mBitRate = bitRate
        this.mFrameRate = frameRate
    }

    /**
     * On encode ready listener
     */
    interface OnEncodeReadyListener {
        /**
         * On ready
         *
         * @param surface mediacodec input surface for getting raw data
         */
        fun onReady(surface: Surface?)
    }

    companion object {
        private const val TAG = "H264EncodeProcessor"
        private const val MIME = "video/hevc"
        private const val FRAME_RATE = 30
        private const val KEY_FRAME_INTERVAL = 1
    }
}
