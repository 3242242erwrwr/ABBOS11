package com.example.xabarsos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.xabarsos.R

@Composable
fun JamuHabarLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    cornerRadius: Dp = (size.value * 0.22f).dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_jamu_habar_logo_img),
        contentDescription = "JAMU HABAR Logo",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    )
}
