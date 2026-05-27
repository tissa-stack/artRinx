package com.example.artrinx.feature.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.artrinx.core.theme.LocalDimens

@Composable
fun ArtMosaicGrid(
    images: List<Int>,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    val leftImages: List<Int>
    val rightImages: List<Int>
    val leftAspectRatio: Float
    val rightAspectRatio: Float

    if (images.size % 2 == 0) {
        leftImages = images.filterIndexed { i, _ -> i % 2 == 0 }
        rightImages = images.filterIndexed { i, _ -> i % 2 == 1 }
        leftAspectRatio = 1.0f
        rightAspectRatio = 1.0f
    } else {
        // 5 images → 2 left (portrait 0.9), 3 right (landscape 1.35)
        // Balanced: 2/0.9 = 3/1.35 = 2.22
        leftImages = listOf(images[0], images[2])
        rightImages = listOf(images[1], images[3], images[4])
        leftAspectRatio = 0.9f
        rightAspectRatio = 1.35f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.gridCellGap),
    ) {
        MosaicColumn(
            images = leftImages,
            aspectRatio = leftAspectRatio,
            modifier = Modifier.weight(1f),
        )
        MosaicColumn(
            images = rightImages,
            aspectRatio = rightAspectRatio,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MosaicColumn(
    images: List<Int>,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.gridCellGap),
    ) {
        images.forEach { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(dimens.gridCornerRadius)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
