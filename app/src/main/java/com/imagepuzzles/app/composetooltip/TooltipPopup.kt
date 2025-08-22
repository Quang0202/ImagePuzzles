package com.github.skgmn.composetooltip

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TRANSITION_INITIALIZE = 0
private const val TRANSITION_ENTER = 1
private const val TRANSITION_EXIT = 2
private const val TRANSITION_GONE = 3

/**
 * Show a tooltip as popup near to an anchor.
 * Anchor can be provided by putting the anchor and Tooltip altogether in one composable.
 *
 * Example:
 * ```kotlin
 * Box {
 *     AnchorComposable()
 *     Tooltip()
 * }
 * ```
 *
 * @param anchorEdge Can be either of [AnchorEdge.Start], [AnchorEdge.Top], [AnchorEdge.End],
 *   or [AnchorEdge.Bottom]
 * @param modifier Modifier for tooltip. Do not use layout-related modifiers except size
 *   constraints.
 * @param tooltipStyle Style for tooltip. Can be created by [rememberTooltipStyle]
 * @param tipPosition Tip position relative to balloon
 * @param anchorPosition Position on the anchor's edge where the tip points out.
 * @param margin Margin between tip and anchor
 * @param onDismissRequest Executes when the user clicks outside of the tooltip.
 * @param properties [PopupProperties] for further customization of this tooltip's behavior.
 * @param content Content inside balloon. Typically [Text].
 */
/**
 * Show a tooltip as popup near to an anchor with transition.
 * As [AnimatedVisibility] is experimental, this function is also experimental.
 * Anchor can be provided by putting the anchor and Tooltip altogether in one composable.
 *
 * Example:
 * ```kotlin
 * Box {
 *     AnchorComposable()
 *     Tooltip()
 * }
 * ```
 *
 * @param anchorEdge Can be either of [AnchorEdge.Start], [AnchorEdge.Top], [AnchorEdge.End],
 *   or [AnchorEdge.Bottom]
 * @param enterTransition [EnterTransition] to be applied when the [visible] becomes true.
 *   Types of [EnterTransition] are listed [here](https://developer.android.com/jetpack/compose/animation#entertransition).
 * @param exitTransition [ExitTransition] to be applied when the [visible] becomes false.
 *   Types of [ExitTransition] are listed [here](https://developer.android.com/jetpack/compose/animation#exittransition).
 * @param modifier Modifier for tooltip. Do not use layout-related modifiers except size
 *   constraints.
 * @param tooltipStyle Style for tooltip. Can be created by [rememberTooltipStyle]
 * @param tipPosition Tip position relative to balloon
 * @param anchorPosition Position on the anchor's edge where the tip points out.
 * @param margin Margin between tip and anchor
 * @param onDismissRequest Executes when the user clicks outside of the tooltip.
 * @param properties [PopupProperties] for further customization of this tooltip's behavior.
 * @param content Content inside balloon. Typically [Text].
 */
@ExperimentalAnimationApi
@Composable
fun Tooltip(
    anchorEdge: AnchorEdge,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    tooltipStyle: TooltipStyle = rememberTooltipStyle(),
    tipPosition: EdgePosition = remember { EdgePosition() },
    anchorPosition: EdgePosition = remember { EdgePosition() },
    margin: Dp = 8.dp,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = remember { PopupProperties() },
    content: @Composable RowScope.() -> Unit,
) = with(anchorEdge) {
    var transitionState by remember { mutableStateOf(TRANSITION_GONE) }

    var widthScreen = Resources.getSystem().displayMetrics.widthPixels
    var heightScreen = Resources.getSystem().displayMetrics.heightPixels

    LaunchedEffect(visible) {
        if (visible) {
            when (transitionState) {
                TRANSITION_EXIT -> transitionState = TRANSITION_ENTER
                TRANSITION_GONE -> {
                    transitionState = TRANSITION_INITIALIZE
                    delay(1)
                    transitionState = TRANSITION_ENTER
                }
            }
        } else {
            when (transitionState) {
                TRANSITION_INITIALIZE -> transitionState = TRANSITION_GONE
                TRANSITION_ENTER -> transitionState = TRANSITION_EXIT
            }
        }
    }
    if (transitionState != TRANSITION_GONE) {
        Popup(
            popupPositionProvider = TooltipPopupPositionProvider(
                LocalDensity.current,
                anchorEdge,
                tooltipStyle,
                tipPosition,
                anchorPosition,
                widthScreen,
                heightScreen,
                margin
            ),
            onDismissRequest = onDismissRequest,
            properties = properties
        ) {
            if (transitionState == TRANSITION_INITIALIZE) {
                TooltipImpl(
                    tooltipStyle = tooltipStyle,
                    tipPosition = tipPosition,
                    anchorEdge = anchorEdge,
                    modifier = modifier.alpha(0f),
                    content = content,
                )
            }
            AnimatedVisibility(
                visible = transitionState == TRANSITION_ENTER,
                enter = enterTransition,
                exit = exitTransition
            ) {
                remember {
                    object : RememberObserver {
                        override fun onAbandoned() {
                            transitionState = TRANSITION_GONE
                        }

                        override fun onForgotten() {
                            transitionState = TRANSITION_GONE
                        }

                        override fun onRemembered() {
                        }
                    }
                }
                TooltipImpl(
                    modifier = modifier,
                    tooltipStyle = tooltipStyle,
                    tipPosition = tipPosition,
                    anchorEdge = anchorEdge,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun AnchorEdge.TooltipImpl(
    tooltipStyle: TooltipStyle,
    tipPosition: EdgePosition,
    anchorEdge: AnchorEdge,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)
) {
    TooltipContainer(
        modifier = modifier,
        cornerRadius = tooltipStyle.cornerRadius,
        tipPosition = tipPosition,
        tip = { Tip(anchorEdge, tooltipStyle) },
        content = {
            TooltipContentContainer(
                anchorEdge = anchorEdge,
                tooltipStyle = tooltipStyle,
                content = content
            )
        }
    )
}

@Composable
internal fun Tip(anchorEdge: AnchorEdge, tooltipStyle: TooltipStyle) = with(anchorEdge) {
    Box(modifier = Modifier
        .size(
            width = anchorEdge.selectWidth(
                tooltipStyle.tipWidth,
                tooltipStyle.tipHeight
            ),
            height = anchorEdge.selectHeight(
                tooltipStyle.tipWidth,
                tooltipStyle.tipHeight
            )
        )
        .background(
            color = tooltipStyle.color,
            shape = GenericShape { size, layoutDirection ->
                this.drawTip(size, layoutDirection)
            }
        )
    )
}

@Composable
internal fun TooltipContentContainer(
    anchorEdge: AnchorEdge,
    tooltipStyle: TooltipStyle,
    content: @Composable (RowScope.() -> Unit)
) = with(anchorEdge) {
    Row(
        modifier = Modifier.Companion
            .minSize(tooltipStyle)
            .background(
                color = tooltipStyle.color,
                shape = RoundedCornerShape(tooltipStyle.cornerRadius)
            )
            .padding(tooltipStyle.contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColorFor(tooltipStyle.color)
        ) {
            content()
        }
    }
}

class TooltipPopupPositionProvider(
    private val density: Density,
    private var anchorEdge: AnchorEdge,
    private val tooltipStyle: TooltipStyle,
    private var tipPosition: EdgePosition,
    private val anchorPosition: EdgePosition,
    private val widthScreen: Int,
    private val heightScreen: Int,
    private val margin: Dp
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = with(density) {
        // Điểm neo (anchor point) trên icon gốc
        val anchorPointX = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left + anchorBounds.width * anchorPosition.percent + anchorPosition.offset.toPx()
        } else {
            anchorBounds.right - anchorBounds.width * anchorPosition.percent - anchorPosition.offset.toPx()
        }
        val anchorPointY = anchorBounds.top + anchorBounds.height * anchorPosition.percent + anchorPosition.offset.toPx()

        // Kích thước mũi nhọn
        val tipWidth = max(tooltipStyle.tipWidth, tooltipStyle.tipHeight).toPx()
        val tipHeight = max(tooltipStyle.tipWidth, tooltipStyle.tipHeight).toPx()

        // Giới hạn màn hình
        val minX = 0f
        val maxX = widthScreen.toFloat() - popupContentSize.width
        val minY = 0f
        val maxY = heightScreen.toFloat() - popupContentSize.height

        // Tính toán vị trí mặc định
        var newX = 0f
        var newY = 0f
        var newTipPercent = tipPosition.percent
        var newAnchorEdge = anchorEdge

        when (anchorEdge) {
            is AnchorEdge.Top -> {
                // Thử Top
                newY = anchorBounds.top - margin.toPx() - popupContentSize.height
                if (newY < minY) {
                    // Thử Bottom
                    newY = anchorBounds.bottom + margin.toPx()
                    if (newY > maxY) {
                        // Cả Top và Bottom không đủ, thử Start hoặc End
                        newAnchorEdge = selectHorizontalEdge(
                            anchorBounds,
                            popupContentSize,
                            layoutDirection,
                            margin,
                            widthScreen
                        )
                        if (newAnchorEdge is AnchorEdge.Start) {
                            newX = if (layoutDirection == LayoutDirection.Ltr) {
                                anchorBounds.left - margin.toPx() - popupContentSize.width
                            } else {
                                anchorBounds.right + margin.toPx()
                            }
                            newY = anchorPointY - tipHeight / 2 - (popupContentSize.height - tipHeight) * tipPosition.percent
                            newY = newY.coerceIn(minY, maxY)
                            if (newX < minX) {
                                newX = minX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            } else if (newX > maxX) {
                                newX = maxX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            }
                        } else if (newAnchorEdge is AnchorEdge.End) {
                            newX = if (layoutDirection == LayoutDirection.Ltr) {
                                anchorBounds.right + margin.toPx()
                            } else {
                                anchorBounds.left - margin.toPx() - popupContentSize.width
                            }
                            newY = anchorPointY - tipHeight / 2 - (popupContentSize.height - tipHeight) * tipPosition.percent
                            newY = newY.coerceIn(minY, maxY)
                            if (newX < minX) {
                                newX = minX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            } else if (newX > maxX) {
                                newX = maxX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            }
                        }
                    } else {
                        // Bottom khả dụng
                        newAnchorEdge = AnchorEdge.Bottom
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    }
                } else {
                    // Top khả dụng
                    val tipY = anchorPointY - newY - tipHeight / 2
                    newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                }
                // Tính toán X và điều chỉnh nếu tràn ra ngoài
                newX = (anchorEdge as AnchorEdge.HorizontalAnchorEdge).calculatePopupPositionX(
                    this, layoutDirection, anchorBounds, anchorPosition, tooltipStyle, tipPosition, popupContentSize
                )
                if (newX < minX) {
                    newX = minX
                    // Cập nhật tipPosition để mũi nhọn chỉ vào anchorPointX
                    val tipX = anchorPointX - newX - tipWidth / 2
                    newTipPercent = if (layoutDirection == LayoutDirection.Ltr) {
                        (tipX / (popupContentSize.width - tipWidth)).coerceIn(0f, 1f)
                    } else {
                        (1f - (tipX / (popupContentSize.width - tipWidth))).coerceIn(0f, 1f)
                    }
                } else if (newX > maxX) {
                    newX = maxX
                    val tipX = anchorPointX - newX - tipWidth / 2
                    newTipPercent = if (layoutDirection == LayoutDirection.Ltr) {
                        (tipX / (popupContentSize.width - tipWidth)).coerceIn(0f, 1f)
                    } else {
                        (1f - (tipX / (popupContentSize.width - tipWidth))).coerceIn(0f, 1f)
                    }
                }
            }
            is AnchorEdge.Bottom -> {
                // Thử Bottom
                newY = anchorBounds.bottom + margin.toPx()
                if (newY > maxY) {
                    // Thử Top
                    newY = anchorBounds.top - margin.toPx() - popupContentSize.height
                    if (newY < minY) {
                        // Cả Bottom và Top không đủ, thử Start hoặc End
                        newAnchorEdge = selectHorizontalEdge(
                            anchorBounds,
                            popupContentSize,
                            layoutDirection,
                            margin,
                            widthScreen
                        )
                        if (newAnchorEdge is AnchorEdge.Start) {
                            newX = if (layoutDirection == LayoutDirection.Ltr) {
                                anchorBounds.left - margin.toPx() - popupContentSize.width
                            } else {
                                anchorBounds.right + margin.toPx()
                            }
                            newY = anchorPointY - tipHeight / 2 - (popupContentSize.height - tipHeight) * tipPosition.percent
                            newY = newY.coerceIn(minY, maxY)
                            if (newX < minX) {
                                newX = minX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            } else if (newX > maxX) {
                                newX = maxX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            }
                        } else if (newAnchorEdge is AnchorEdge.End) {
                            newX = if (layoutDirection == LayoutDirection.Ltr) {
                                anchorBounds.right + margin.toPx()
                            } else {
                                anchorBounds.left - margin.toPx() - popupContentSize.width
                            }
                            newY = anchorPointY - tipHeight / 2 - (popupContentSize.height - tipHeight) * tipPosition.percent
                            newY = newY.coerceIn(minY, maxY)
                            if (newX < minX) {
                                newX = minX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            } else if (newX > maxX) {
                                newX = maxX
                                val tipY = anchorPointY - newY - tipHeight / 2
                                newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                            }
                        }
                    } else {
                        // Top khả dụng
                        newAnchorEdge = AnchorEdge.Top
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    }
                } else {
                    // Bottom khả dụng
                    val tipY = anchorPointY - newY - tipHeight / 2
                    newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                }
                // Tính toán X và điều chỉnh nếu tràn ra ngoài
                newX = (anchorEdge as AnchorEdge.HorizontalAnchorEdge).calculatePopupPositionX(
                    this, layoutDirection, anchorBounds, anchorPosition, tooltipStyle, tipPosition, popupContentSize
                )
                if (newX < minX) {
                    newX = minX
                    // Cập nhật tipPosition để mũi nhọn chỉ vào anchorPointX
                    val tipX = anchorPointX - newX - tipWidth / 2
                    newTipPercent = if (layoutDirection == LayoutDirection.Ltr) {
                        (tipX / (popupContentSize.width - tipWidth)).coerceIn(0f, 1f)
                    } else {
                        (1f - (tipX / (popupContentSize.width - tipWidth))).coerceIn(0f, 1f)
                    }
                } else if (newX > maxX) {
                    newX = maxX
                    val tipX = anchorPointX - newX - tipWidth / 2
                    newTipPercent = if (layoutDirection == LayoutDirection.Ltr) {
                        (tipX / (popupContentSize.width - tipWidth)).coerceIn(0f, 1f)
                    } else {
                        (1f - (tipX / (popupContentSize.width - tipWidth))).coerceIn(0f, 1f)
                    }
                }
            }
            is AnchorEdge.Start, is AnchorEdge.End -> {
                // Xử lý Start và End như trước
                val intOffset = anchorEdge.calculatePopupPosition(
                    this, tooltipStyle, tipPosition, anchorPosition, margin, anchorBounds, layoutDirection, popupContentSize
                )
                newX = intOffset.x.toFloat()
                newY = intOffset.y.toFloat()
                if (newAnchorEdge is AnchorEdge.Start) {
                    if (layoutDirection == LayoutDirection.Ltr && newX < minX) {
                        newX = minX
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    } else if (layoutDirection == LayoutDirection.Rtl && newX > maxX) {
                        newX = maxX
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    }
                } else if (newAnchorEdge is AnchorEdge.End) {
                    if (layoutDirection == LayoutDirection.Ltr && newX > maxX) {
                        newX = maxX
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    } else if (layoutDirection == LayoutDirection.Rtl && newX < minX) {
                        newX = minX
                        val tipY = anchorPointY - newY - tipHeight / 2
                        newTipPercent = (tipY / (popupContentSize.height - tipHeight)).coerceIn(0f, 1f)
                    }
                }
                newY = newY.coerceIn(minY, maxY)
            }
        }

        // Cập nhật anchorEdge và tipPosition
        anchorEdge = newAnchorEdge
        tipPosition = EdgePosition(percent = newTipPercent, offset = tipPosition.offset)

        return IntOffset(newX.roundToInt(), newY.roundToInt())
    }

    private fun selectHorizontalEdge(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        layoutDirection: LayoutDirection,
        margin: Dp,
        widthScreen: Int
    ): AnchorEdge = with(density) {
        val spaceLeft = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left - margin.toPx()
        } else {
            widthScreen - anchorBounds.right - margin.toPx()
        }
        val spaceRight = if (layoutDirection == LayoutDirection.Ltr) {
            widthScreen - anchorBounds.right - margin.toPx()
        } else {
            anchorBounds.left - margin.toPx()
        }

        return if (spaceLeft >= popupContentSize.width && spaceLeft >= spaceRight) {
            AnchorEdge.Start
        } else if (spaceRight >= popupContentSize.width) {
            AnchorEdge.End
        } else {
            // Chọn hướng có nhiều không gian hơn
            if (spaceLeft > spaceRight) AnchorEdge.Start else AnchorEdge.End
        }
    }
}