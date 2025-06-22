package com.imagepuzzles.app.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imagepuzzles.app.R
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun PuzzleGameScreen() {
    val context = LocalContext.current
    val bitmap = BitmapFactory.decodeResource(context.resources,R.drawable.splash)
    val puzzleSize = 3 // 3x3 grid
    val frameSizeDp = 600.dp // Kích thước khung game
    val frameSizePx = with(LocalDensity.current) { frameSizeDp.toPx() } // Chuyển dp sang px
    val scale = minOf(frameSizePx / bitmap.width, frameSizePx / bitmap.height) // Tỉ lệ thu phóng
    val scaledBitmap = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt(),
        (bitmap.height * scale).toInt(),
        true
    )
    val tileSizePx = scaledBitmap.width / puzzleSize
    val tileSizeDp = with(LocalDensity.current) { (tileSizePx / density).dp }
    val tiles = remember { mutableStateListOf<Bitmap?>() }
    val emptyTileIndex = remember { mutableStateOf(puzzleSize * puzzleSize - 1) }
    var moveCount by remember { mutableStateOf(0) }
    var isGameWon by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tiles.clear()
        for (i in 0 until puzzleSize) {
            for (j in 0 until puzzleSize) {
                if (i == puzzleSize - 1 && j == puzzleSize - 1) {
                    tiles.add(null)
                } else {
                    val tileBitmap = Bitmap.createBitmap(scaledBitmap, j * tileSizePx, i * tileSizePx, tileSizePx, tileSizePx)
                    tiles.add(tileBitmap)
                }
            }
        }
        // Shuffle tiles
        repeat(100) {
            val possibleMoves = getPossibleMoves(emptyTileIndex.value, puzzleSize)
            val nextMove = possibleMoves[Random().nextInt(possibleMoves.size)]
            tiles.swap(emptyTileIndex.value, nextMove)
            emptyTileIndex.value = nextMove
        }
    }

    // Check win condition
    LaunchedEffect(tiles) {
        if (!isGameWon && tiles.isNotEmpty()) {
            var correct = true
            for (i in 0 until puzzleSize * puzzleSize - 1) {
                val row = i / puzzleSize
                val col = i % puzzleSize
                val expectedBitmap = Bitmap.createBitmap(scaledBitmap, col * tileSizePx, row * tileSizePx, tileSizePx, tileSizePx)
                if (tiles[i] != null && !tiles[i]!!.sameAs(expectedBitmap)) {
                    correct = false
                    break
                }
            }
            if (correct && tiles[puzzleSize * puzzleSize - 1] == null) {
                isGameWon = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Moves: $moveCount",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isGameWon) {
            Text(
                text = "Congratulations! You won!",
                fontSize = 24.sp,
                color = Color.Green,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(frameSizeDp)
                .border(2.dp, Color.Black)
        ) {
            Column {
                for (i in 0 until puzzleSize) {
                    Row {
                        for (j in 0 until puzzleSize) {
                            val index = i * puzzleSize + j
                            val tile = tiles.getOrNull(index)
                            Box(
                                modifier = Modifier
                                    .size(tileSizeDp)
                                    .background(if (tile == null) Color.Gray else Color.White)
                                    .border(1.dp, Color.Black)
                                    .clickable(enabled = !isGameWon && canMoveTile(index, emptyTileIndex.value, puzzleSize)) {
                                        tiles.swap(index, emptyTileIndex.value)
                                        emptyTileIndex.value = index
                                        moveCount++
                                    }
                            ) {
                                if (tile != null) {
                                    Image(
                                        bitmap = tile.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getPossibleMoves(emptyIndex: Int, puzzleSize: Int): List<Int> {
    val moves = mutableListOf<Int>()
    val row = emptyIndex / puzzleSize
    val col = emptyIndex % puzzleSize

    if (row > 0) moves.add(emptyIndex - puzzleSize) // Up
    if (row < puzzleSize - 1) moves.add(emptyIndex + puzzleSize) // Down
    if (col > 0) moves.add(emptyIndex - 1) // Left
    if (col < puzzleSize - 1) moves.add(emptyIndex + 1) // Right

    return moves
}

fun canMoveTile(tileIndex: Int, emptyIndex: Int, puzzleSize: Int): Boolean {
    return tileIndex in getPossibleMoves(emptyIndex, puzzleSize)
}

fun MutableList<Bitmap?>.swap(index1: Int, index2: Int) {
    val temp = this[index1]
    this[index1] = this[index2]
    this[index2] = temp
}