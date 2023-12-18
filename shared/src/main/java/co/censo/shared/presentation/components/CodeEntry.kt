package co.censo.shared.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.presentation.SharedColors

@Composable
fun CodeEntry(
    length: Int,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    primaryColor: Color,
    borderColor: Color,
    backgroundColor: Color,
    requestFocus: Boolean = false
) {
    val modifier = Modifier

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = requestFocus) {
        focusRequester.requestFocus()
    }

    val boxHeight = 80.dp
    val boxWidth = 34.dp
    val spaceBetweenBoxes = 11.dp

    val roundCorner = 15.dp

    BasicTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = value,
        singleLine = true,
        onValueChange = {
            if (it.length <= length) {
                onValueChange(it)
            }
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                Modifier
                    .size(
                        width = (boxWidth * length + spaceBetweenBoxes * (length - 1) + roundCorner * 2),
                        height = boxHeight
                    )
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(roundCorner)
                    )
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(roundCorner)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,

                ) {
                repeat(length) { index ->
                    val shouldHaveFocusedBorder =
                        index == value.length || !value.getOrNull(index)?.toString()
                            .isNullOrEmpty()

                    // spacer in front
                    if (index == 0) {
                        Spacer(
                            modifier = Modifier.width(roundCorner),
                        )
                    }

                    // separator between numbers
                    if (index != 0) {
                        Box(
                            modifier = Modifier.size(spaceBetweenBoxes, boxHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 22.dp)
                                    .width(1.dp)
                                    .background(color = borderColor)
                            )
                        }
                    }

                    // number
                    Box(
                        modifier = Modifier.size(boxWidth, boxHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        val isFilled = value.getOrNull(index) != null
                        Text(
                            modifier = Modifier.padding(2.dp),
                            text = value.getOrNull(index)?.toString() ?: "_",
                            textAlign = TextAlign.Center,
                            fontSize = 40.nonScaledSp,
                            fontWeight = if (isFilled) FontWeight.Bold else FontWeight.Normal,
                            color = if (enabled && (isFilled || shouldHaveFocusedBorder)) primaryColor else borderColor
                        )
                    }

                    // spacer in the end
                    if (index == length - 1) {
                        Spacer(
                            modifier = Modifier.width(roundCorner),
                        )
                    }
                }
            }
        })

}

@Preview()
@Composable
fun PreviewCodeEntry() {
    CodeEntry(
        length = 6,
        enabled = true,
        value = "43561",
        onValueChange = {},
        primaryColor = SharedColors.MainColorText,
        borderColor = SharedColors.BorderGrey,
        backgroundColor = SharedColors.WordBoxBackground
    )
}


val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp