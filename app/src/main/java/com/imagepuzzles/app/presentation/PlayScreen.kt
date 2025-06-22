package com.example.puzzleimagegame.ui.screens.playscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun PlayScreen(
    modifier: Modifier,
    level: Int
){
    // The grid is a 3x3 matrix of integers, where 0 represents the empty space.
    var grid by remember { mutableStateOf(generateGrid(level)) }
    var emptyPosition by remember { mutableStateOf(findEmptyPosition(grid)) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Drawing the grid inside a Canvas, and detecting swipe gestures.
        Canvas(
            modifier = Modifier
                .size(screenWidth-10.dp)
                .padding(end = 5.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // When drag ends, stop further detection until next swipe
                        },
                        onDragCancel = {
                            // When drag cancels, stop further detection until next swipe
                        },
                        onDrag = { change, dragAmount ->
                            // Determine the direction of the swipe based on the drag amount.
                            val direction = getDragDirection(dragAmount)
                            if (direction != null) {
                                // Identify which box was touched.
                                val touchedBox =
                                    findTouchedBox(change.position, grid.size, size.width / level.toFloat())
                                if (touchedBox != null) {
                                    // Move the box only if it is adjacent to the empty space.
                                    val (newGrid, newEmptyPosition) = grid.tryMove(
                                        direction,
                                        emptyPosition,
                                        touchedBox
                                    )
                                    grid = newGrid
                                    emptyPosition = newEmptyPosition
                                }
                            }
                        }
                    )
                }
        ) {
            // Draw the grid with the numbers.
            drawGrid(grid, level)
        }
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

// Function to determine the direction of the swipe.
fun getDragDirection(dragAmount: Offset): Direction? {
    return when {
        //abs will always give +ve number either we pass +ve or -ve
        abs(dragAmount.x) > abs(dragAmount.y) -> {  //we are swiping in x-axis
            if (dragAmount.x > 0) Direction.RIGHT else Direction.LEFT
        }

        abs(dragAmount.y) > abs(dragAmount.x) -> {  //we are swiping in y-axis
            if (dragAmount.y > 0) Direction.DOWN else Direction.UP
        }

        else -> null
    }
}

// Function to find the box that was touched based on the touch position.
fun findTouchedBox(position: Offset, gridSize: Int, cellSize: Float): Pair<Int, Int>? {
    val x = (position.x / cellSize).toInt()
    val y = (position.y / cellSize).toInt()
    return if (x in 0 until gridSize && y in 0 until gridSize) x to y else null
}

// Function to attempt moving the touched box if it is adjacent to the empty space.
fun List<List<Int>>.tryMove(
    direction: Direction,
    emptyPosition: Pair<Int, Int>,
    touchedBox: Pair<Int, Int>
): Pair<List<List<Int>>, Pair<Int, Int>> {
    val (emptyX, emptyY) = emptyPosition
    val (touchedX, touchedY) = touchedBox
    val newGrid = this.map { it.toMutableList() }

    return when (direction) {
        Direction.UP -> if (touchedX == emptyX && touchedY == emptyY + 1) {
            // Move the box down if the empty space is directly below it.
            newGrid[emptyY][emptyX] = newGrid[touchedY][touchedX]
            newGrid[touchedY][touchedX] = 0
            newGrid to (touchedX to touchedY)
        } else this to emptyPosition

        Direction.DOWN -> if (touchedX == emptyX && touchedY == emptyY - 1) {
            // Move the box up if the empty space is directly above it.
            newGrid[emptyY][emptyX] = newGrid[touchedY][touchedX]
            newGrid[touchedY][touchedX] = 0
            newGrid to (touchedX to touchedY)
        } else this to emptyPosition

        Direction.LEFT -> if (touchedY == emptyY && touchedX == emptyX + 1) {
            // Move the box left if the empty space is directly to the left of it.
            newGrid[emptyY][emptyX] = newGrid[touchedY][touchedX]
            newGrid[touchedY][touchedX] = 0
            newGrid to (touchedX to touchedY)
        } else this to emptyPosition

        Direction.RIGHT -> if (touchedY == emptyY && touchedX == emptyX - 1) {
            // Move the box right if the empty space is directly to the right of it.
            newGrid[emptyY][emptyX] = newGrid[touchedY][touchedX]
            newGrid[touchedY][touchedX] = 0
            newGrid to (touchedX to touchedY)
        } else this to emptyPosition
    }
}

// Function to generate a shuffled 3x3 grid with numbers 1-8 and one empty space.
fun generateGrid(): List<List<Int>> {
    return (0..8).shuffled().chunked(3)
}
fun generateGrid(level: Int): List<List<Int>> {
    // Tạo một danh sách số từ 0 đến level * level - 1
    val numbers = (0 until level * level).toList().shuffled()

    // Chia danh sách thành các danh sách con, mỗi danh sách có độ dài bằng level
    return numbers.chunked(level)
}


// Function to find the position of the empty space in the grid.
fun findEmptyPosition(grid: List<List<Int>>): Pair<Int, Int> {
    grid.forEachIndexed { y, row ->
        row.forEachIndexed { x, number ->
            if (number == 0) return x to y
        }
    }
    throw IllegalStateException("No empty space found in the grid")
}

// Function to draw the entire grid.
fun DrawScope.drawGrid(grid: List<List<Int>>, level: Int) {
    val cellSize = size.width / level.toFloat()
    grid.forEachIndexed { y, row ->
        row.forEachIndexed { x, number ->
            if (number != 0) {
                drawBoxWithNumber(
                    number = number,
                    x = x,
                    y = y,
                    cellSize = cellSize,
                    padding = 5.dp,
                    level = level
                )
            }
        }
    }
}

// Function to draw an individual box with a number inside it.
fun DrawScope.drawBoxWithNumber(number: Int, x: Int, y: Int, cellSize: Float, padding: Dp, level: Int) {
    val boxSize = cellSize - padding.toPx()
    val left = x * cellSize + padding.toPx()
    val top = y * cellSize + padding.toPx()

    drawRoundRect(
        color = Color.Green.copy(0.5f),
        topLeft = Offset(left, top),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    )
    drawContext.canvas.nativeCanvas.drawText(
        number.toString(),
        left + boxSize / 2,
        top + boxSize / 1.5f,
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = (40*3/level).sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.BLACK
            typeface = android.graphics.Typeface.create("", android.graphics.Typeface.BOLD)
        }
    )
}