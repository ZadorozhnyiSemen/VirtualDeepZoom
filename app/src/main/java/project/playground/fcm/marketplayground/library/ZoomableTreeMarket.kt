package project.playground.fcm.marketplayground.library

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.util.Log.d
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.threesixty.coin.math.Mappable
import com.threesixty.coin.math.Rect
import com.threesixty.coin.math.SquarifiedLayout
import com.threesixty.coin.math.TreeModel
import project.playground.fcm.marketplayground.R
import java.util.Random
import kotlin.math.abs

class ZoomableTreeMarket @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet, defStyleAttr),
    GestureDetector.OnGestureListener {

    val layoutManager = SquarifiedLayout()
    val rndGenerator = Random()
    val marketClickList: MarketItemClickListener? = null

    var marketData: TreeModel? = null
        set(value) {
            field = value
            if (viewSizeX != 0f && viewSizeY != 0f) {
                calculateBounds()
                invalidate()
            }
        }

    private fun calculateBounds() {
        marketData?.let {
            layoutManager.layout(
                it.treeItems,
                Rect(
                    x.toDouble(),
                    y.toDouble(),
                    viewSizeX.toDouble(),
                    viewSizeY.toDouble()
                )
            )
        }
    }

    var zoomValue = MIN_ZOOM
        set(value) {
            field = when {
                value > MAX_ZOOM -> MAX_ZOOM
                value < MIN_ZOOM -> MIN_ZOOM
                else -> value
            }
        }
    var scaleFactor = DEFAULT_SCALE_FACTOR

    private val virtualField = RectF(0f, 0f, 0f, 0f)
    private var currentVirtualField = RectF(0f, 0f, 0f, 0f)
    private var horizontalRatio: Float = 1f
    private var verticalRatio: Float = 1f

    var inDebugMode = true
        set(value) {
            field = value
            invalidate()
        }
    private val gestureDetector = GestureDetectorCompat(context, this)
    private var viewScaledTouchSlop = 0f

    private var touchPointX = 0f
    private var touchPointY = 0f

    private var pinching = false

    // region Debug values
    private var pinchFirstX = 0f
    private var pinchFirstY = 0f
    private var pinchSecondX = 0f
    private var pinchSecondY = 0f
    private var viewSizeX = 0f
    private var viewSizeY = 0f
    private val viewRatio: Float
        get() = viewSizeX / viewSizeY
    private val zoomIntensity = .7f
    private var virtualPinchFirstX = 0f
    private var virtualPinchFirstY = 0f
    private var virtualPinchSecX = 0f
    private var virtualPinchSecY = 0f

    var shouldApplyScaleAndTransform = true
        set(value) {
            field = value
            invalidate()
        }

    private var scrollX = 0f
    private var scrollY = 0f
    // endregion Debug values

    private val circleRadius = context.resources.getDimensionPixelSize(R.dimen.circleRad).toFloat()
    private val virtualCircleRadius =
        context.resources.getDimensionPixelSize(R.dimen.virtualCircleRad).toFloat()
    private val circlePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val virtualCirclePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val debugTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = context.resources.getDimensionPixelSize(R.dimen.debugTextSize).toFloat()
        isAntiAlias = true
    }
    private val currentVFPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        isAntiAlias = true
    }
    private val blockPaint = Paint().apply {
        isAntiAlias = true
        color = Color.YELLOW
    }
    private val blockStroke = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    init {
        val viewConfig = ViewConfiguration.get(context)
        viewScaledTouchSlop = viewConfig.scaledTouchSlop.toFloat()
    }

    private var pointerCount = 0

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        d(ZoomableTreeMarket::class.java.simpleName, "Event is down")
        e?.let {
            currentDownEvent?.recycle()
            currentDownEvent = MotionEvent.obtain(e)

            primStartTouchEventX = it.x
            primStartTouchEventY = it.y
        }
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val pinch = isPinchGesture(e2)
        val scroll = isScrollGesture(e2)

        d(ZoomableTreeMarket::class.java.simpleName, "VIEW [PINCHING: $pinch] [SCROLLING: $scroll]")

        return true
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    private var primStartTouchEventX: Float = 0f
    private var primStartTouchEventY: Float = 0f
    private var secondaryStartTouchEventX: Float = 0f
    private var secondaryStartTouchEventY: Float = 0f
    private var touchDistance: Float = 0f
    private var intermediateTouchDistance: Float = 0f

    private var currentDownEvent: MotionEvent? = null
    private var downTimestamp = System.currentTimeMillis()


    private fun isPinchGesture(e2: MotionEvent?): Boolean {
        e2?.let {
            if (it.pointerCount == 2) {
                val distanceCurrent = distance(it, 0, 1)
                val diffPrimX = primStartTouchEventX - it.getX(0)
                val diffPrimY = primStartTouchEventY - it.getY(0)
                val diffSecondaryX = secondaryStartTouchEventX - it.getX(1)
                val diffSecondaryY = secondaryStartTouchEventY - it.getY(1)

                //val midPointX = (getInnerX(it.getX(0)) + getInnerX(it.getX(1))) / 2
                //val midPointY = (getInnerY(it.getY(0)) + getInnerY(it.getY(1))) / 2
                val midPointX = (it.getX(0) + it.getX(1)) / 2
                val midPointY = (it.getY(0) + it.getY(1)) / 2

                val resizeValueChange = distanceCurrent - intermediateTouchDistance
                d("Debug", "Resize value: $resizeValueChange")

                if (//Math.abs(distanceCurrent - touchDistance) > viewScaledTouchSlop &&
                    (diffPrimY * diffSecondaryY) <= 0 &&
                    (diffPrimX * diffSecondaryX) <= 0
                ) {
                    resizeVirtualField(midPointX to midPointY, distanceCurrent - touchDistance)
                        pinchFirstX = it.getX(0)
                        pinchFirstY = it.getY(0)
                        pinchSecondX = it.getX(1)
                        pinchSecondY = it.getY(1)
                        virtualPinchFirstX = getInnerX(it.getX(0))
                        virtualPinchFirstY = getInnerY(it.getY(0))
                        virtualPinchSecX = getInnerX(it.getX(1))
                        virtualPinchSecY = getInnerY(it.getY(1))
                        intermediateTouchDistance = distanceCurrent

                        invalidate()

                    return true
                }
            }
        }
        return false
    }

    private fun isScrollGesture(event: MotionEvent?): Boolean {
        event?.let {
            if (it.pointerCount == 2) {
                pinching = true
                val diffPrim = primStartTouchEventY - it.getY(0)
                val diffSec = secondaryStartTouchEventY - it.getY(1)

                if (diffPrim * diffSec > 0 &&
                    Math.abs(diffPrim) > viewScaledTouchSlop &&
                    Math.abs(diffSec) > viewScaledTouchSlop
                ) {

                        pinchFirstX = it.getX(0)
                        pinchFirstY = it.getY(0)
                        pinchSecondX = it.getX(1)
                        pinchSecondY = it.getY(1)
                        invalidate()

                    return true
                }
            } else if (it.pointerCount == 1 && !pinching) {
                val diffPrimX = primStartTouchEventX - it.getX(0)
                val diffPrimY = primStartTouchEventY - it.getY(0)
                if (Math.abs(diffPrimX) > viewScaledTouchSlop ||
                    Math.abs(diffPrimY) > viewScaledTouchSlop
                ) {

                        pinchFirstX = it.getX(0)
                        pinchFirstY = it.getY(0)

                        virtualPinchFirstX = getInnerX(it.getX(0))
                        virtualPinchFirstY = getInnerY(it.getY(0))

                        val diffX = getInnerX(touchPointX) - virtualPinchFirstX
                        val diffY = getInnerY(touchPointY) - virtualPinchFirstY

                        val newLeft = currentVirtualField.left + diffX
                        val newRight = currentVirtualField.right + diffX
                        val newTop = currentVirtualField.top + diffY
                        val newBottom = currentVirtualField.bottom + diffY

                        if (newLeft >= 0f && newRight <= width) {
                            currentVirtualField.left = currentVirtualField.left + diffX
                            currentVirtualField.right = currentVirtualField.right + diffX
                        }

                        if (newTop >= 0f && newBottom <= height) {
                            currentVirtualField.top = currentVirtualField.top + diffY
                            currentVirtualField.bottom = currentVirtualField.bottom + diffY
                        }

                        touchPointX = pinchFirstX
                        touchPointY = pinchFirstY

                        invalidate()

                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.actionMasked
        gestureDetector.onTouchEvent(event)

        action?.let {
            when (it) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    pointerCount++
                    d(
                        ZoomableTreeMarket::class.java.simpleName,
                        "PointerDown. Amount of touches: $pointerCount"
                    )

                    if (event.pointerCount > 1) {
                        secondaryStartTouchEventX = event.getX(1)
                        secondaryStartTouchEventY = event.getY(1)
                        touchDistance = distance(event, 0, 1)

                        currentDownEvent?.recycle()
                        currentDownEvent = MotionEvent.obtain(event)

                        if (System.currentTimeMillis() - downTimestamp > 50) {

                        }

                        downTimestamp = System.currentTimeMillis()

                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 1 && pinching) {
                        pinching = !pinching
                    }
                    pointerCount--
                    resetPinch(pointerCount)
                    d(
                        ZoomableTreeMarket::class.java.simpleName,
                        "PointerUp. Amount of touches: $pointerCount"
                    )
                    return true
                }
                MotionEvent.ACTION_DOWN -> {
                    touchPointX = event.x
                    touchPointY = event.y
                    pointerCount++
                    d(
                        ZoomableTreeMarket::class.java.simpleName,
                        "ActionDown. Amount of touches: $pointerCount, touch point x $touchPointX and y $touchPointY"
                    )
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (event.pointerCount == 1 && pinching) {
                        pinching = !pinching
                    }
                    pointerCount--
                    resetPinch(pointerCount)
                    d(
                        ZoomableTreeMarket::class.java.simpleName,
                        "ActionUp. Amount of touches: $pointerCount"
                    )
                    return true
                }
                else -> {
                }
            }
        }

        return false
    }

    fun resizeVirtualField(focusDot: Pair<Float, Float>, resizeValue: Float) {
        // Calculate new bounds for virtual field. Calculations based on frame
        // delta distance between fingers (pinch gesture)
        var newLeft =
            currentVirtualField.left + resizeValue * zoomIntensity * viewRatio * focusDot.first / currentVirtualField.right
        var newTop =
            currentVirtualField.top + resizeValue * zoomIntensity * focusDot.second / currentVirtualField.bottom
        var newRight =
            currentVirtualField.right - resizeValue * zoomIntensity * viewRatio * (1 - focusDot.first / currentVirtualField.right)
        var newBottom =
            currentVirtualField.bottom - resizeValue * zoomIntensity * (1 - focusDot.second / currentVirtualField.bottom)


        // Check out of bounds. If side out of bounds make opposite site grow
        // two times faster
        if (newLeft < virtualField.left) {
            newRight += abs(virtualField.left - newLeft)
        }
        if (newRight > virtualField.right) {
            newLeft -= abs(virtualField.right - newRight)
        }
        if (newTop < virtualField.top) {
            newBottom += abs(virtualField.top - newTop)
        }
        if (newBottom > virtualField.bottom) {
            newTop -= abs(virtualField.bottom - newBottom)
        }

        // Cut merge all sides with view size if out of bounds
        if (newLeft < virtualField.left) newLeft = virtualField.left
        if (newRight > virtualField.right) newRight = virtualField.right
        if (newTop < virtualField.top) newTop = virtualField.top
        if (newBottom > virtualField.bottom) newBottom = virtualField.bottom

        // Set new rectangle to virtual field and invalidate view
        currentVirtualField = RectF(newLeft, newTop, newRight, newBottom)
        horizontalRatio =
            (currentVirtualField.right - currentVirtualField.left) / virtualField.right
        verticalRatio = (currentVirtualField.bottom - currentVirtualField.top) / virtualField.bottom
        invalidate()
    }

    private fun resetPinch(pointerCount: Int) {
        if (pointerCount == 0) {
            pinchFirstX = 0f
            pinchFirstY = 0f
            pinchSecondX = 0f
            pinchSecondY = 0f
            touchDistance = 0f
            intermediateTouchDistance = 0f
            primStartTouchEventX = 0f
            primStartTouchEventY = 0f
            secondaryStartTouchEventX = 0f
            secondaryStartTouchEventY = 0f
            intermediateTouchDistance = 0f
            invalidate()
        }
    }

    private fun distance(event: MotionEvent, first: Int, second: Int): Float {
        if (event.pointerCount >= 2) {
            val x = (getInnerX(event.getX(first)) - getInnerX(event.getX(second))).toDouble()
            val y = (getInnerY(event.getY(first)) - getInnerY(event.getY(second))).toDouble()
            return Math.sqrt(x * x + y * y).toFloat()
        } else {
            return 0f
        }
    }

    private fun getInnerX(outerX: Float) =
        outerX * (currentVirtualField.right - currentVirtualField.left) / virtualField.right + currentVirtualField.left

    private fun getInnerY(outerY: Float) =
        outerY * (currentVirtualField.bottom - currentVirtualField.top) / virtualField.bottom + currentVirtualField.top

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        virtualField.right = width.toFloat()
        virtualField.bottom = height.toFloat()
        currentVirtualField.right = width.toFloat()
        currentVirtualField.bottom = height.toFloat()
        viewSizeX = width.toFloat()
        viewSizeY = height.toFloat()
        marketData?.let {
            calculateBounds()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val visibleBlocks = mutableListOf<Mappable>()
            marketData?.let { tree ->
                tree.treeItems.forEach { item ->

                    // Draw only overlapping
                    if (blockVisible(item)) {
                        visibleBlocks.add(item)
                    }
                }
            }

            visibleBlocks.forEach { visibleBlock ->
                val transformedLeft: Double
                val transformedTop: Double
                val scaledRight: Double
                val scaledBottom: Double

                if (shouldApplyScaleAndTransform) {
                    val innerSquareNewLeft = (visibleBlock.bounds.x - currentVirtualField.left) / horizontalRatio
                    val innerSquareNewTop = (visibleBlock.bounds.y - currentVirtualField.top) / verticalRatio
                    val innerSquareNewRight = innerSquareNewLeft + (visibleBlock.bounds.w / horizontalRatio)
                    val innerSquareNewBottom = innerSquareNewTop + (visibleBlock.bounds.h / verticalRatio)

                    transformedLeft = innerSquareNewLeft
                    transformedTop = innerSquareNewTop
                    scaledRight = innerSquareNewRight
                    scaledBottom = innerSquareNewBottom
                } else {
                    scaledRight = visibleBlock.bounds.w + visibleBlock.bounds.x
                    scaledBottom = visibleBlock.bounds.h + visibleBlock.bounds.y
                    transformedLeft = visibleBlock.bounds.x
                    transformedTop = visibleBlock.bounds.y
                }

                it.drawRect(
                    transformedLeft.toFloat(),
                    transformedTop.toFloat(),
                    scaledRight.toFloat(),
                    scaledBottom.toFloat(),
                    blockPaint
                )
                it.drawRect(
                    transformedLeft.toFloat(),
                    transformedTop.toFloat(),
                    scaledRight.toFloat(),
                    scaledBottom.toFloat(),
                    blockStroke
                )
            }

            if (inDebugMode) {
                it.drawCircle(pinchFirstX, pinchFirstY, circleRadius, circlePaint)
                it.drawCircle(pinchSecondX, pinchSecondY, circleRadius, circlePaint)

                it.drawCircle(
                    virtualPinchFirstX,
                    virtualPinchFirstY,
                    virtualCircleRadius,
                    virtualCirclePaint
                )
                it.drawCircle(
                    virtualPinchSecX,
                    virtualPinchSecY,
                    virtualCircleRadius,
                    virtualCirclePaint
                )

                it.drawText("View x: $viewSizeX", 50f, 50f, debugTextPaint)
                it.drawText("View y: $viewSizeY", 50f, 100f, debugTextPaint)
                it.drawText("View dimentions $virtualField", 50f, 150f, debugTextPaint)
                it.drawText("Zoomed boundaries $currentVirtualField", 50f, 200f, debugTextPaint)
                it.drawText("Touch distance: $touchDistance", 50f, 250f, debugTextPaint)
                it.drawText(
                    "Intermediate touch distance: $intermediateTouchDistance",
                    50f,
                    300f,
                    debugTextPaint
                )
                it.drawRect(currentVirtualField, currentVFPaint)
                it.drawText(
                    "Main touch: x[$touchPointX] y[$touchPointY]",
                    50f,
                    350f,
                    debugTextPaint
                )
            }
        }
    }

    private fun blockVisible(item: Mappable): Boolean {
        item.let {
            if (currentVirtualField.top > (it.bounds.y + it.bounds.h) ||
                currentVirtualField.bottom < it.bounds.y
            ) {
                return false
            }
            if (currentVirtualField.right < it.bounds.x ||
                currentVirtualField.left > (it.bounds.x + it.bounds.w)
            ) {
                return false
            }
            return true
        }
    }

    interface MarketItemClickListener {
        fun onItemClicked(itemName: String)
    }

    companion object {
        private const val MIN_ZOOM = 0f
        private const val MAX_ZOOM = 100f
        private const val DEFAULT_SCALE_FACTOR = 1.8f
    }
}