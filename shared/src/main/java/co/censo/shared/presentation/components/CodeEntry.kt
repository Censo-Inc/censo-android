package co.censo.shared.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import co.censo.shared.presentation.SharedColors.DividerGray

@Composable
fun CodeEntry(
    validCodeLength: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    primaryColor: Color
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
            primaryColor = primaryColor
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
    primaryColor: Color
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
            focusRequester.requestFocus()
    }

    val boxHeight = 82.dp
    val boxWidth = 44.dp

    val spaceBetweenBoxes = 16.dp

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
                Modifier.size(width = (boxWidth + spaceBetweenBoxes) * length, height = boxHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(length) { index ->
                    val shouldHaveFocusedBorder =
                        index == value.length || !value.getOrNull(index)?.toString().isNullOrEmpty()

                    val borderModifier = Modifier.border(
                        2.dp,
                        color = if (shouldHaveFocusedBorder) primaryColor else DividerGray,
                        shape = RoundedCornerShape(4.dp)
                    )

                    // To achieve increased space between the 3rd and 4th boxes
                    val fourthBoxIndex = 3
                    if (index == fourthBoxIndex) {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Box(
                        modifier = borderModifier
                            .size(boxWidth, boxHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(2.dp),
                            text = value.getOrNull(index)?.toString() ?: "",
                            textAlign = TextAlign.Center,
                            fontSize = 48.nonScaledSp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }
        })
}

val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp