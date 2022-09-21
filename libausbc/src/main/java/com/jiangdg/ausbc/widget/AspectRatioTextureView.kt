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
package com.jiangdg.ausbc.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.jiangdg.ausbc.utils.Logger
import kotlin.math.abs

/** Adaptive TextureView
 * Aspect ratio (width:height, such as 4:3, 16:9).
 *
 * @author Created by jiangdg on 2021/12/23
 */
class AspectRatioTextureView: TextureView, IAspectRatio {

    private var mAspectRatio = -1.0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    override fun setAspectRatio(width: Int, height: Int) {
        val orientation = context.resources.configuration.orientation
        // 处理竖屏和横屏情况
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            setAspectRatio(height.toDouble() / width)
//            return
//        }
        setAspectRatio(width.toDouble() / height)

        dc.common.Logger.w("texture view set ratio",width,height)

    }

    override fun getSurfaceWidth(): Int  = width

    override fun getSurfaceHeight(): Int  = height

    override fun getSurface(): Surface? {
        return try {
            Surface(surfaceTexture)
        } catch (e: Exception) {
            null
        }
    }

    override fun postUITask(task: () -> Unit) {
        post {
            task()
        }
    }

    private fun setAspectRatio(aspectRatio: Double) {
        if (aspectRatio < 0 || mAspectRatio == aspectRatio) {
            return
        }
        mAspectRatio = aspectRatio
        Logger.i(TAG, "AspectRatio = $mAspectRatio")
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        var initialHeight = MeasureSpec.getSize(heightMeasureSpec)
        val horizontalPadding = paddingLeft - paddingRight
        val verticalPadding = paddingTop - paddingBottom

        dc.common.Logger.w(javaClass.name,initialWidth,initialHeight,horizontalPadding,verticalPadding,paddingLeft,paddingRight,paddingTop,paddingBottom)

        initialWidth -= horizontalPadding
        initialHeight -= verticalPadding
        // 比较预览与TextureView(内容)纵横比
        // 如果有变化，重新设置TextureView尺寸
        val viewAspectRatio = initialWidth.toDouble() / initialHeight
        val diff = mAspectRatio / viewAspectRatio - 1
        var wMeasureSpec = widthMeasureSpec
        var hMeasureSpec = heightMeasureSpec

        dc.common.Logger.w(javaClass.name,initialWidth,initialHeight,viewAspectRatio,diff,wMeasureSpec,hMeasureSpec)
        if (mAspectRatio > 0 && abs(diff) > 0.01) {
            // diff > 0， 按宽缩放
            // diff < 0， 按高缩放
            if (diff > 0) {
                initialHeight = (initialWidth / mAspectRatio).toInt()
            } else {
                initialWidth = (initialHeight * mAspectRatio).toInt()
            }
            // 重新设置TextureView尺寸
            // 注意加回padding大小
            initialWidth += horizontalPadding
            initialHeight += verticalPadding
            wMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
            hMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(wMeasureSpec, hMeasureSpec)
    }


//    fun sizeNotify(width:Int,height: Int) {
//        val viewWidth = width.toFloat()
//        val viewHeight = height.toFloat()
//        var scaleX = 1.0f
//        var scaleY = 1.0f
//        var mPreviewWidth: Int = width
//        var mPreviewHeight: Int = height
//        if (viewWidth < viewHeight) {
//            mPreviewWidth = height
//            mPreviewHeight = width
//        }
//        if (mPreviewWidth > viewWidth && mPreviewHeight > viewHeight) {
//            scaleX = mPreviewWidth / viewWidth
//            scaleY = mPreviewHeight / viewHeight
//        } else if (mPreviewWidth < viewWidth && mPreviewHeight < viewHeight) {
//            scaleY = viewWidth / mPreviewWidth
//            scaleX = viewHeight / mPreviewHeight
//        } else if (viewWidth > mPreviewWidth) {
//            scaleY = viewWidth / mPreviewWidth / (viewHeight / mPreviewHeight)
//        } else if (viewHeight > mPreviewHeight) {
//            scaleX = viewHeight / mPreviewHeight / (viewWidth / mPreviewWidth)
//        }
//
//        // Calculate pivot points, in our case crop from center
//        val pivotPointX = (viewWidth / 2)
//        val pivotPointY = (viewHeight / 2)
//        val matrix = Matrix()
//        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY)
//        /*Log.e(TAG, "viewsize:" + viewWidth + " * " + viewHeight +
//                    ";prviewSize:" + mPreviewWidth + " * " + mPreviewHeight +
//                    ";scale:" + scaleX + " * " + scaleY +
//                    ";pivot:" + pivotPointX + " * " + pivotPointY);*/
//        setTransform(matrix)
//    }

//    fun calculateSurfaceHolderTransform(): Matrix? {
//        // 预览 View 的大小，比如 SurfaceView
//        val viewHeight: Int = configManager.getScreenResolution().y
//        val viewWidth: Int = configManager.getScreenResolution().x
//        // 相机选择的预览尺寸
//        val cameraHeight: Int = configManager.getCameraResolution().x
//        val cameraWidth: Int = configManager.getCameraResolution().y
//        // 计算出将相机的尺寸 => View 的尺寸需要的缩放倍数
//        val ratioPreview = cameraWidth.toFloat() / cameraHeight
//        val ratioView = viewWidth.toFloat() / viewHeight
//        val scaleX: Float
//        val scaleY: Float
//        if (ratioView < ratioPreview) {
//            scaleX = ratioPreview / ratioView
//            scaleY = 1f
//        } else {
//            scaleX = 1f
//            scaleY = ratioView / ratioPreview
//        }
//        // 计算出 View 的偏移量
//        val scaledWidth = viewWidth * scaleX
//        val scaledHeight = viewHeight * scaleY
//        val dx = (viewWidth - scaledWidth) / 2
//        val dy = (viewHeight - scaledHeight) / 2
//        val matrix = Matrix()
//        matrix.postScale(scaleX, scaleY)
//        matrix.postTranslate(dx, dy)
//        return matrix
//    }

    companion object {
        private const val TAG = "AspectRatioTextureView"
    }
}
