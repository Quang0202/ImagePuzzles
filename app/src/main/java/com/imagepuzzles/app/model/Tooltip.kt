package com.imagepuzzles.app.model

import androidx.compose.ui.unit.dp
import com.github.skgmn.composetooltip.AnchorEdge
import com.github.skgmn.composetooltip.EdgePosition

enum class TooltipPosition(
    val anchorEdge: AnchorEdge,
    val edgePosition: EdgePosition
) {
    Top(AnchorEdge.Top, EdgePosition(0.5f)),
    TopStart(AnchorEdge.Top, EdgePosition(0.25f)),
    TopEnd(AnchorEdge.Top, EdgePosition(0.75f)),

    Bottom(AnchorEdge.Bottom, EdgePosition(0.5f)),
    BottomStart(AnchorEdge.Bottom, EdgePosition(0.25f)),
    BottomEnd(AnchorEdge.Bottom,EdgePosition(0.75f)),

    Start(AnchorEdge.Start, EdgePosition(0.5f)),
    StartTop(AnchorEdge.Start, EdgePosition(0.25f)),
    StartBottom(AnchorEdge.Start, EdgePosition(0.75f)),

    End(AnchorEdge.End, EdgePosition(0.5f)),
    EndTop(AnchorEdge.End, EdgePosition(0.25f)),
    EndBottom(AnchorEdge.End, EdgePosition(0.75f));
}
