package com.android.app.planettab

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.android.app.R
import com.android.core.utils.dip
import com.android.core.utils.getScreenWidth
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 动画方向
 */
private const val ANIM_DIRECTION_NONE = 0
private const val ANIM_DIRECTION_LEFT = -1
private const val ANIM_DIRECTION_RIGHT = 1

/**
 * @author jiaochengyun@baidu.com
 * @since 3.0.0
 *
 * 星球Tab
 * 1. 星球环的绘制
 *    椭圆公式rotateProportion
 *    用户视角调整cameraDistance，涉及每个角度星球大小和每个星球所在角度的计算
 * 2. 星球单个切换过渡动画逻辑
 * 3. 星球滑动(随手仅x轴)过渡动画逻辑
 * 4. 点击星球多个切换过渡动画逻辑
 * 5. 抽取每个星球静置状态下固定角度集
 * 6. 单独处理选中(最前方)星球单独缩放规则(选中星球最大)
 * 7. 点击切换和触摸抬起切换 事件冲突问题处理
 * 8. 星球字体渐变色处理
 * 9. 选中星球文本隐藏
 * 10. 横竖滑动事件冲突
 * 11. 初始化根据选中星球调整数据源的真实顺序和显示角度
 * 12. 星球Tab整体宽高计算，星球放大缩小后 真实或近似(至少显示完整)的高度值计算
 * 13. afx接入，afx选中状态的位置计算
 * 14. 选中星球默认图和afx静置图动画切换过程中2倍速渐变(即：动画执行一半时默认图和文本透明度变为0)
 * 15. 只处理选中星球特殊缩放以及切换动画马上只有一个角度转换时afx动画静置图和默认图片才显示渐变效果
 *
 * 注：子View 尽量不要使用View.GONE等导致父View重新测量和布局的属性，由于动画是在layout布局中实现，会导致动画reset。
 *
 * ios 方案：
 * CATransform3D：https://www.kancloud.cn/manual/ios/97786
 * CATransformLayer：https://www.kancloud.cn/manual/ios/97792
 * 缺点：点击事件有问题(需动态计算)
 */
@SuppressLint("ClickableViewAccessibility")
class PlanetTabViewAnim : FrameLayout {
    private var centerX = 0
    private var centerY = 0
    private var viewWidth = 0
    private var viewHeight = 0

    /** 开始绘制的角度 */
    private val startAngle = 270f

    /**
     * 开始位置放大比例,注意比例不要太大。
     * 选中星球太大，容易和其它星球重合，可能看着没重合但是实际已经重合了。
     * 导致点击事件有问题(看着点击的是某星球，实际上点击的是选中星)
     */
    private val startScala = 1.8f

    /** 平面正圆绕x轴旋转的比例，控制远近效果，属于椭圆半径 x：y 的值 */
    private val rotateProportion = 120f / 360

    /** 角度缩放，可设置非选中星球的间距 */
    private val angleScalaRatio = 1f

    /** 相机(用户视角)距离 */
    private val cameraDistance = dip(100)

    /** 椭圆X轴半径 */
    private var ovalXRadius = 0f

    /** 椭圆Y轴半径 */
    private var ovalYRadius = 0f

    /** padding值 防止缩放超过边界 */
    private val padding = dip(45)

    /** 当前选中位置(以数据源中index为主) */
    private var currentSelectedIndex = 0

    /** 将来选中位置(以数据源中index为主) */
    private var futureSelectedIndex = 0

    /** 本地切换动画切换次数 */
    private var currentToggleNum = 1

    /** 随手滑动动画方向，是否向右滑动 */
    private var isSwipeRight = false

    /** 动画是否正在执行 */
    private var isAnimRunning = false

    /** 动画方向 */
    private var animDirection = ANIM_DIRECTION_NONE

    /** 动画滑动过程中触发切换的阈值方 */
    private var swipeToggleDistance = dip(100).toFloat()

    private var dataCount = 0

    /** 数据源 */
    private var dataArray: List<PlanetItemData>? = null

    private var isClickSwitch = true

    /** 点击已选中的星球Tab 回调 */
    var clickSelectedCallback: ((PlanetItemData?) -> Unit)? = null

    /** 切换Tab 回调 */
    var switchTabCallback: ((PlanetItemData, direction: Int) -> Unit)? = null

    /** 星球轨迹的位置 */
    private val planetTrackRect = RectF()

    /** 此TabView屏幕中的位置 */
    private val planetTabViewGlobalRect = Rect()

    /** 通过计算算出的afx在此TabView中的位置 */
    private val calculateSelectedAfxRect = Rect()

    /** 选中星球屏幕中的位置 */
    private val selectedPlanetViewGlobalRect = Rect()

    /** item中仅星球的大小和顶部间距 */
    private val planetTopSpace = resources.getDimension(R.dimen.planet_top_space)
    private val planetOriginalWidth = resources.getDimension(R.dimen.planet_width)
    private val planetOriginalHeight = resources.getDimension(R.dimen.planet_height)

    /** 滑动后选中星球的AFX特效动画 */
    private val planetAfxView by lazy {
        PlanetAfxView(context).apply {
            clickCallback = {
                clickSelectedCallback?.invoke(dataArray?.getOrNull(currentSelectedIndex))
            }
        }
    }

    /** 最小最大动画时长 */
    private val minAnimDuration = 330L
    private val maxAnimDuration = 660L

    /** 动画集 */
    private val animSet by lazy {
        AnimatorSet().apply {
            interpolator = DecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    dataArray?.getOrNull(futureSelectedIndex)?.let {
                        switchTabCallback?.invoke(
                            it,
                            if (isClickSwitch) ANIM_DIRECTION_NONE else animDirection
                        )
                    }
                }

                override fun onAnimationEnd(animation: Animator?) {
                    changeItemIndex()
                    changeItemsAngle()

                    isAnimRunning = false
                    animDirection = ANIM_DIRECTION_NONE

                    currentSelectedIndex = futureSelectedIndex
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationRepeat(animation: Animator?) {
                }
            })
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setWillNotDraw(false)
        viewWidth = context.getScreenWidth()

        ovalXRadius = (viewWidth - padding * 2).toFloat() / 2
        swipeToggleDistance = ovalXRadius
        // 椭圆公式rotateProportion + dip(5) 为ui细节调整
        ovalYRadius = ovalXRadius * rotateProportion + dip(5)
        val originalOvalHeight = (ovalYRadius * 2).toInt()
        viewHeight = originalOvalHeight

        centerX = viewWidth / 2
        centerY = originalOvalHeight / 2

        planetTrackRect.set(
            padding.toFloat(), padding.toFloat(),
            (viewWidth - padding).toFloat(), ovalYRadius * 2 + padding
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var viewHeight = this.viewHeight
        if (childCount > 0) {
            val startChild = getChildAt(0)
            val originalPlanetHeight = startChild.measuredHeight
            val maxScala = calculateSelectScaleRule(270f)
            val minScala = calculateUnSelectScaleRule(90.0)

            val minPlanetRadius = (originalPlanetHeight * minScala / 2)
            val maxPlanetRadius = (originalPlanetHeight * maxScala / 2)
            val diffPlanetTopSpace = planetTopSpace * maxScala / 2
            centerY = (ovalYRadius + minPlanetRadius + paddingTop).toInt()
            // View的高度 = 原始椭圆轨道高度 + 顶部最小星球半径 + 底部最大星球半径 - 选中星球文本位移的距离
            viewHeight =
                (viewHeight + minPlanetRadius + maxPlanetRadius - diffPlanetTopSpace).toInt()

            planetTrackRect.set(
                padding.toFloat(),
                minPlanetRadius + paddingTop,
                (viewWidth - padding).toFloat(),
                viewHeight + paddingTop - maxPlanetRadius
                        + (planetTopSpace * calculateSelectScaleRule(startAngle) / 2)
            )
        }

        setMeasuredDimension(viewWidth, viewHeight + paddingTop + paddingBottom)
    }

    val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.parseColor("#FF0000")
        canvas.drawRect(planetTrackRect, paint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (childCount > 0) {
            for (index in 0 until childCount) {
                when (getChildAt(index)) {
                    is PlanetItemView -> layoutItemChildren(index)
                }
            }
        }
        if (isUseCalculateRect()) {
            calculateSelectedAfxRect.let {
                planetAfxView.layout(it.left, it.top, it.right, it.bottom)
            }
            planetTabViewGlobalRect.set(0, 0, 0, 0)
            selectedPlanetViewGlobalRect.set(0, 0, 0, 0)
        } else {
            planetAfxView.layout(
                selectedPlanetViewGlobalRect.left,
                selectedPlanetViewGlobalRect.top - planetTabViewGlobalRect.top,
                selectedPlanetViewGlobalRect.right,
                selectedPlanetViewGlobalRect.bottom - planetTabViewGlobalRect.top
            )
        }
    }

    fun initPlanetListData(data: List<PlanetItemData>) {
        resetTabViewToInitial()
        dataArray = data
        if (!dataArray.isNullOrEmpty()) {
            currentSelectedIndex = 0
            dataCount = data.size
            val middle = dataCount / 2f
            for (i in 0 until dataCount) {
                val item = data[i]
                if (item.currentIndex == 0) {
                    currentSelectedIndex = i
                }
                val child = PlanetItemView(context).apply {
                    setPlanetBean(item)
                    clickListener = {
                        val currentIndex = it?.currentIndex ?: 0
                        if (currentIndex == 0) {
                            val count = if (currentIndex > middle) {
                                // 左半边星球
                                isSwipeRight = true
                                dataCount - currentIndex
                            } else {
                                // 右半边星球
                                isSwipeRight = false
                                currentIndex
                            }
                            isClickSwitch = true
                            // 点击星球多个切换过渡动画逻辑
                            initToggleTransitionAnimation(count)
                        } else {
                            clickSelectedCallback?.invoke(it)
                        }
                    }
                }
                addView(
                    child,
                    LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            calculateAngleArray(startAngle, dataCount)
            switchTabCallback?.invoke(data[currentSelectedIndex], ANIM_DIRECTION_NONE)

            addView(
                planetAfxView,
                LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        requestLayout()
    }

    /** 重置view到初始 */
    private fun resetTabViewToInitial() {
        animSet.cancel()
        removeAllViews()
        animDirection = ANIM_DIRECTION_NONE
    }

    /** 计算静置状态每个星球静置时的角度 */
    private fun calculateAngleArray(start: Float, size: Int) {
        // 抽取每个星球静置状态下固定角度集
        PlanetItemData.calculateAngleArray(start, size) { index, average ->
            getCurrentItemAngle(index, average)
        }
        // 计算完成角度后，对各个星球实例进行角度变量初始化
        changeItemsAngle()
    }

    /** 获取当前星球角度 */
    private fun getCurrentItemAngle(index: Int, average: Float): Float {
        val angle = (startAngle + average * index) % 360
        // 角度缩放比。用户视角调整cameraDistance，涉及每个角度星球大小和每个星球所在角度的计算
        val scale = ovalYRadius * cos(angleToRadian(angle)) / cameraDistance
        val scaleAngle = average * scale.toFloat() * angleScalaRatio
        // 二、三象限角度减小；一、四象限角度增大
        return if (angle == 90f || angle == 270f) angle else angle + scaleAngle
    }

    private fun layoutItemChildren(index: Int) {
        val child = getChildAt(index)
        // 度数
        val angle = dataArray?.getOrNull(index)?.currentAngle() ?: startAngle
        // 弧度
        val radian = angleToRadian(angle)
        val rect = calculateItemLayoutRect(child, radian)

        /* 计算缩放比例 */
        val angleAbs = (angle + 360) % 360
        val isRange = PlanetItemData.isSelectAngleRange(angleAbs)
        // 是否选中态特殊处理
        val isSelectedRange =
            (currentSelectedIndex == index || futureSelectedIndex == index) && isRange
//        val scale = if (isSelectedRange) {
//            calculateSelectScaleRule(angleAbs)
//        } else {
//            calculateUnSelectScaleRule(radian)
//        }
        val scale = calculateUnSelectScaleRule(radian)
        if (child is PlanetItemView) {
            // 选中星球文本隐藏
            val threshold = (1 - sin(angleToRadian(angle)).toFloat()) / 2 * 0.6f + 0.4f
            child.alpha = threshold
            child.setItemViewAlpha(if (isSelectedRange) calculateSelectItemProportion(angleAbs) else 1f)
        }
        val diffTopSpace = if (isSelectedRange) {
            val prop = 1 - calculateSelectItemProportion(angleAbs)
            prop * planetTopSpace * scale / 2
        } else 0f

        child.layout(
            rect.left, (rect.top - diffTopSpace).toInt(), rect.right,
            (rect.bottom - diffTopSpace).toInt()
        )

        child.scaleX = scale
        child.scaleY = scale
    }

    private fun childSizeRect(child: View, angle: Float): Rect {
        val radian = angleToRadian(angle)
        val rect = calculateItemLayoutRect(child, radian)

        val scale = calculateUnSelectScaleRule(radian)
        val afxWidth = planetOriginalWidth * scale
        val afxHeight = planetOriginalHeight * scale
        val diffWidth = afxWidth - planetOriginalWidth
        val diffHeight = afxHeight - planetOriginalHeight
        val scaleTopScale = 0

        val sizeRect = Rect()
        sizeRect.set(
            (rect.left - diffWidth / 2).toInt(),
            (rect.top - diffHeight / 2 + scaleTopScale).toInt(),
            (rect.right + diffWidth / 2).toInt(),
            (rect.bottom + diffHeight / 2 + scaleTopScale).toInt()
        )
        return rect
    }

    /** 根据宽高和弧度计算layout的位置 */
    private fun calculateLayoutRect(width: Int, height: Int, radian: Double): Rect {
        val sin = sin(radian)
        val cos = cos(radian)
        val coordinateX = centerX + ovalXRadius * cos
        val coordinateY = centerY - ovalYRadius * sin

        val x1 = (coordinateX - width / 2).toInt()
        val y1 = (coordinateY - height / 2).toInt()
        val x2 = (coordinateX + width / 2).toInt()
        val y2 = (coordinateY + height / 2).toInt()

        return Rect(x1, y1, x2, y2)
    }

    /**
     * 布局选中状态的afx动画位置
     * 通过afx原始大小 和 选中状态的缩放比，以及选中状态星球layout_marginTop 值的缩放比计算最终位置
     */
    private fun calculateSelectedAfxLayoutRect() {
        val afxRect = calculateSelectedPlanetAfx()
        val scale = calculateSelectScaleRule(startAngle)
        val afxWidth = planetOriginalWidth * scale
        val afxHeight = planetOriginalHeight * scale
        val diffWidth = afxWidth - planetOriginalWidth
        val diffHeight = afxHeight - planetOriginalHeight
        val scaleTopScale = 0
//        val scaleTopScale = planetTopSpace * scale / 2
        calculateSelectedAfxRect.set(
            (afxRect.left - diffWidth / 2).toInt(),
            (afxRect.top - diffHeight / 2 + scaleTopScale).toInt(),
            (afxRect.right + diffWidth / 2).toInt(),
            (afxRect.bottom + diffHeight / 2 + scaleTopScale).toInt()
        )
    }

    /**
     * 是否使用计算出来的afx的位置，默认不使用(存在1px的偏移，float转int导致)
     * 误差值：若计算值和动态计算出来的值存在大于 2px 则使用计算，证明此时动态计算出来的可能有问题
     * 比如：页面上滑View位置发生变化，此View的父布局发生了layout导致此View重新布局就可能存在异常
     */
    private fun isUseCalculateRect(): Boolean {
        if (calculateSelectedAfxRect.right == 0) {
            calculateSelectedAfxLayoutRect()
        }
        if (planetTabViewGlobalRect.right == 0) {
            getGlobalVisibleRect(planetTabViewGlobalRect)
        }
        if (selectedPlanetViewGlobalRect.right == 0) {
            (getCurrentSelectedView() as? PlanetItemView)?.getSelectedIcon()
                ?.getGlobalVisibleRect(selectedPlanetViewGlobalRect)
        }
        val difference = 2
        val top = selectedPlanetViewGlobalRect.top - planetTabViewGlobalRect.top
        val bottom = selectedPlanetViewGlobalRect.bottom - planetTabViewGlobalRect.top
        val diffTop = calculateSelectedAfxRect.top - top
        val diffBottom = calculateSelectedAfxRect.bottom - bottom
        return abs(diffTop) > difference || abs(diffBottom) > difference
    }

    /**
     * 计算当前itemView的layout位置
     * @param child 当前子View
     * @param radian 弧度，弧度 = 角度 * PI / 180
     */
    private fun calculateItemLayoutRect(child: View, radian: Double): Rect {
        return calculateLayoutRect(child.measuredWidth, child.measuredHeight, radian)
    }

    /** 计算选中星球afx位置 */
    private fun calculateSelectedPlanetAfx(): Rect {
        val radian = angleToRadian(startAngle)
        return calculateLayoutRect(
            planetOriginalWidth.toInt(),
            planetOriginalHeight.toInt(),
            radian
        )
    }

    /** 计算未选中星球单独缩放规则 */
    private fun calculateUnSelectScaleRule(radian: Double): Float {
        // 物体离你的距离增加一倍，视觉上，物体缩小为原来的1/2。
        // 缩放比例 = (相机(用户视角)距离-绕x轴旋转距离) / 相机(用户视角)距离
        return ((cameraDistance - ovalYRadius * sin(radian)) / cameraDistance).toFloat()
    }

    /** 计算选中(最前方)星球单独缩放规则 */
    private fun calculateSelectScaleRule(angle: Float): Float {
        val firstNextAngle = PlanetItemData.firstNextAngle
        // 代表选中(最前方最近的)星球弧度
        val firstRadian = angleToRadian(startAngle)
        // 代表选中(最前方最近的)星球下一个位置的弧度
        val firstNextRadian = angleToRadian(firstNextAngle)
        // 选中星球的单独放大规则处理
        val firstScala =
            (((cameraDistance - ovalYRadius * sin(firstRadian)) / cameraDistance) * startScala).toFloat()
        // 选中星球下一个星球的正常规则处理
        val firstNextScala =
            (((cameraDistance - ovalYRadius * sin(firstNextRadian)) / cameraDistance)).toFloat()
        return (1 - calculateSelectItemProportion(angle)) * (firstScala - firstNextScala) + firstNextScala
    }

    /** 计算选中(最前方)星球当前角度比例 */
    private fun calculateSelectItemProportion(angle: Float): Float {
        val diff = angle - startAngle
        val firstNextAngle = PlanetItemData.firstNextAngle
        return abs(diff) / (abs(firstNextAngle - startAngle))
    }

    /** 手势处理 */
    private var downX = 0f
    private var downY = 0f
    private var firstPointerId = -1

    /** 系统所认为的最小滑动距离TouchSlop */
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: 0f
        val y = event?.y ?: 0f
        val actionIndex = event?.actionIndex ?: 0
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                firstPointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = downX - x
                val dy = downY - y
                if (abs(dx) > abs(dy) && abs(dx) > touchSlop) {
                    // 解决横竖滑动事件冲突
                    requestDisallowInterceptTouchEvent(true)
                    // 产生滑动，则拦截事件，防止触发子item的点击事件
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                val currentPointerId = event.getPointerId(actionIndex)
                if (currentPointerId == firstPointerId) {
                    val dx = downX - x
                    val dy = downY - y
                    if (abs(dx) > abs(dy) && abs(dx) > touchSlop) {
                        isSwipeRight = dx <= 0
                        isClickSwitch = false
                        // 执行切换动画，星球单个切换过渡动画逻辑
                        initToggleTransitionAnimation(1)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // 保证空白(非星球)区域可滑动切换星球
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val currentPos = FloatArray(2)

    /**
     * 执行切换动画
     * @param count 切换个数
     */
    private fun initToggleTransitionAnimation(count: Int) {
        if (isAnimRunning || dataCount <= 0) return
        currentToggleNum = count
        animDirection = if (isSwipeRight) ANIM_DIRECTION_RIGHT else ANIM_DIRECTION_LEFT
        futureSelectedIndex = if (isSwipeRight) {
            (currentSelectedIndex - count + dataCount) % dataCount
        } else {
            (currentSelectedIndex + count + dataCount) % dataCount
        }
        var build: AnimatorSet.Builder? = null
        dataArray?.forEachIndexed { index, item ->
            if (index > 0) return@forEachIndexed
            val path = Path()
            val fromAngle = 360 - item.currentAngle
            val toAngle = if (isSwipeRight) {
                // 右滑：current --> next (大 --> 小)
                val nextAngle = 360 - item.nextAngle(count)
                if (fromAngle < nextAngle) nextAngle - 360 else nextAngle
            } else {
                // 左滑：current --> previous (小 --> 大)
                val previousAngle = 360 - item.previousAngle(count)
                if (fromAngle > previousAngle) previousAngle + 360 else previousAngle
            }

//            Log.e("jcy", "initToggleTransitionAnimation: child=${getChildAt(index)} fromAngle=$fromAngle toAngle=$toAngle")
//            path.addArc(planetTrackRect, fromAngle, fromAngle - toAngle)
            path.addArc(planetTrackRect, 90f, 270f - PlanetItemData.firstPreviousAngle)
            val pathMeasure = PathMeasure(path, false)

            val valueAnimator = ValueAnimator.ofFloat(0f, pathMeasure.length)
//            valueAnimator.duration = if (count == 1) minAnimDuration else maxAnimDuration
            valueAnimator.duration = 10000
            valueAnimator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float

                pathMeasure.getPosTan(value, currentPos, null)
                val angle = getAngleFromCoordinate(currentPos)
//
//                Log.e("jcy", "addUpdateListener: child=${getChildAt(index)} x=${currentPos[0]} y=${currentPos[1]} centerX=$centerX centerY=$centerY angle=$angle")
//                /* 计算缩放比例 */
//                val angleAbs = (angle + 360) % 360
//                val isRange = PlanetItemData.isSelectAngleRange(angleAbs)
//                // 是否选中态特殊处理
//                val isSelectedRange =
//                    (currentSelectedIndex == index || futureSelectedIndex == index) && isRange
//                val scale = if (isSelectedRange) {
//                    calculateSelectScaleRule(angleAbs)
//                } else {
//                    calculateUnSelectScaleRule(angleToRadian(angle))
//                }
//                val diffTopSpace = if (isSelectedRange) {
//                    val prop = 1 - calculateSelectItemProportion(angleAbs)
//                    prop * planetTopSpace * scale / 2
//                } else 0f

                (getChildAt(item.originalIndex) as? PlanetItemView)?.let { itemView ->
                    // 选中星球文本隐藏
//                    val threshold = (1 - sin(angleToRadian(angle)).toFloat()) / 2 * 0.6f + 0.4f
//                    itemView.alpha = threshold
//                    itemView.setItemViewAlpha(
//                        if (isSelectedRange) calculateSelectItemProportion(
//                            angleAbs
//                        ) else 1f
//                    )
//                    itemView.scaleX = scale
//                    itemView.scaleY = scale
                    val rect = childSizeRect(itemView, angle.toFloat())
                    pathMeasure.getPosTan(value, currentPos, null)
                    // 注意 translationX 动画是以View的左上角坐标为准
                    itemView.translationX = currentPos[0] - rect.left - (rect.right - rect.left) / 2
                    itemView.translationY = currentPos[1] - rect.top - (rect.bottom - rect.top) / 2
                    Log.e(
                        "jcy",
                        "initToggleTransitionAnimation: child=${itemView} x=${currentPos[0]} y=${currentPos[1]} left=${itemView.left} top=${itemView.top}"
                    )
                }
            }
            valueAnimator.start()
        }

        // 开始播放动画
        animSet.duration = if (count == 1) minAnimDuration else maxAnimDuration
        isAnimRunning = true
        animSet.start()
    }

    /** 选中星球单一通过动画实现轨迹动画 */
    private fun initTransitionAnimation(count: Int) {
        getChildAt(0)?.let {
            val path = Path()
            path.addArc(planetTrackRect, 90f, 270f - PlanetItemData.firstPreviousAngle)
            val pathMeasure = PathMeasure(path, false)

            val valueAnimator = ValueAnimator.ofFloat(0f, pathMeasure.length)
            valueAnimator.duration = 10000
            valueAnimator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float

                pathMeasure.getPosTan(value, currentPos, null)
                it.translationX = currentPos[0] - it.left - (it.right - it.left) / 2
                it.translationY =
                    currentPos[1] - it.top - (it.bottom - it.top) / 2 - planetTopSpace / 4 * 3
            }
            valueAnimator.start()
        }
    }

    /** 获取当前星球index */
    private fun changeItemIndex() {
        if (animDirection == ANIM_DIRECTION_NONE) return
        dataArray?.forEach {
            when (animDirection) {
                ANIM_DIRECTION_RIGHT -> {
                    it.resetCurrentIndex(currentToggleNum)
                }
                ANIM_DIRECTION_LEFT -> {
                    it.resetCurrentIndex(-currentToggleNum)
                }
                else -> {}
            }
        }
    }

    /** 改变所有星球的角度 */
    private fun changeItemsAngle() {
        dataArray?.forEach {
            it.currentAngle = it.currentAngle()
        }
    }

    /** 获取选中星球View */
    private fun getCurrentSelectedView(): View? {
        return getChildAt(dataArray?.getOrNull(currentSelectedIndex)?.originalIndex ?: 0)
    }

    /** 角度转弧度 */
    private fun angleToRadian(angle: Float): Double {
        return angle.toDouble() * PI / 180
    }

    /** 根据坐标获取当前角度 */
    private fun getAngleFromCoordinate(floatArray: FloatArray): Double {
        val x = floatArray[0]
        val y = floatArray[1]


        // 坐标点 (椭圆轨道坐标的坐标点)
        val coordinatePointsX = x - centerX
        val coordinatePointsY = y - centerY

        // 斜边长
        val hypotenuseLength =
            sqrt(coordinatePointsX * coordinatePointsX + coordinatePointsY * coordinatePointsY).toDouble()
        val sin = abs(coordinatePointsY) / abs(hypotenuseLength)
        val angle = Math.toDegrees(asin(sin))
        return when {
            x >= centerX && y <= centerY -> {
                // 第一坐标系 包括0° 和 90°
                angle
            }
            x > centerX && y > centerY -> {
                // 第四坐标系
                360 - angle
            }
            x < centerX && y <= centerY -> {
                // 第二坐标系 包括 180°
                180 - angle
            }
            x <= centerX && y > centerY -> {
                // 第三坐标系 包括 270°
                180 + angle
            }
            else -> {
                angle
            }
        }
    }
}