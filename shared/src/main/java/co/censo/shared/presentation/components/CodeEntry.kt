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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeEntry(
    validCodeLength: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    primaryColor: Color,
    borderColor: Color,
    backgroundColor: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        CodeInputField(
            value = value,
            length = validCodeLength,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onValueChange,
            isLoading = isLoading,
            primaryColor = primaryColor,
            borderColor = borderColor,
            backgroundColor = backgroundColor
        )
    }
}

@Composable
fun CodeInputField(
    value: String,
    length: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    primaryColor: Color,
    borderColor: Color,
    backgroundColor: Color
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
            focusRequester.requestFocus()
    }

    val boxHeight = 88.dp
    val boxWidth = 40.dp
    val spaceBetweenBoxes = 14.dp

    val roundCorner = 20.dp

    BasicTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = value,
        singleLine = true,
        onValueChange = {
            if (it.length <= length) {
                onValueChange(it)
            }
        },
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                Modifier
                    .size(width = (boxWidth * length + spaceBetweenBoxes * (length - 1) + roundCorner * 2), height = boxHeight)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(roundCorner))
                    .background(color = backgroundColor, shape = RoundedCornerShape(roundCorner)),
                horizontalArrangement = Arrangement.SpaceBetween,

            ) {
                repeat(length) { index ->
                    val shouldHaveFocusedBorder =
                        index == value.length || !value.getOrNull(index)?.toString().isNullOrEmpty()

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
                                    .fillMaxHeight().padding(vertical = 22.dp)
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
                            color = if (isFilled || shouldHaveFocusedBorder) primaryColor else borderColor
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

val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp