package co.censo.vault.presentation.components.security_plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhonelinkErase
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.censo.shared.data.model.Guardian
import co.censo.shared.presentation.Colors
import co.censo.shared.presentation.Colors.DividerGray
import co.censo.shared.presentation.Colors.GreyText
import co.censo.shared.presentation.Colors.LabelText
import co.censo.shared.presentation.Colors.PrimaryBlue
import co.censo.vault.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApproverDialog(
    nickname: String,
    onDismiss: () -> Unit,
    paddingValues: PaddingValues,
    updateApproverName: (String) -> Unit,
    submit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(color = Colors.BackgroundAlphaBlack)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(color = Color.White, shape = RoundedCornerShape(8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            val contentHorizontalPadding = 16.dp

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(horizontal = contentHorizontalPadding),
                text = stringResource(R.string.enter_nickname),
                color = Color.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W700
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                modifier = Modifier.padding(horizontal = contentHorizontalPadding + 2.dp),
                text = stringResource(R.string.just_something_for_you_to_remember_this_approver_by),
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryBlue,
                    cursorColor = Colors.CursorBlue,
                    textColor = Color.Black,
                    errorBorderColor = Color.Red,
                ),
                placeholder = {
                    Text(stringResource(R.string.approver_placeholder), color = LabelText)
                },
                shape = RoundedCornerShape(0.dp),
                value = nickname,
                onValueChange = updateApproverName
            )

            Spacer(modifier = Modifier.height(28.dp))

            Divider(
                thickness = 1.dp,
                color = DividerGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.W400,
                        fontSize = 20.sp
                    )
                }

                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = submit
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.W700,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CancelEditPlanDialog(
    paddingValues: PaddingValues,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(color = Colors.BackgroundAlphaBlack)
            .verticalScroll(rememberScrollState()),
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 36.dp)
                .background(color = Color.White, shape = RoundedCornerShape(8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.padding(12.dp))

            Text(
                text = stringResource(R.string.are_you_sure),
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "Explain to user that they will either return to existing plan or restart",
                color = Color.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Divider(
                thickness = 1.dp,
                color = DividerGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(id = R.string.dismiss),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.W400,
                        fontSize = 20.sp
                    )
                }

                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCancel
                ) {
                    Text(
                        text = stringResource(R.string.exit),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.W700,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EditOrDeleteMenu(
    onDismiss: () -> Unit,
    edit: () -> Unit,
    delete: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .wrapContentSize(Alignment.TopEnd)
    ) {
        DropdownMenu(
            modifier = Modifier.background(color = Color.White),
            expanded = true,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
                text = {
                    Text(
                        stringResource(id = R.string.edit),
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                },
                onClick = edit
            )
            DropdownMenuItem(
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
                text = {
                    Text(
                        stringResource(id = R.string.delete),
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                },
                onClick = delete
            )
        }
    }
}

@Composable
fun ThresholdText(
    threshold: Int,
    total: Int,
    boxStyle: Boolean,
    paddingValues: PaddingValues = PaddingValues(),
    contentPaddingValues: PaddingValues = PaddingValues(),
) {

    val modifier = if (boxStyle) {
        Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .border(
                width = 1.dp,
                color = GreyText.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp)
            )
            .shadow(elevation = 5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color = Color.White)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = modifier.padding(contentPaddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val bigStyle = SpanStyle(
            fontSize = 24.sp,
            color = Color.Black,
            fontWeight = FontWeight.W700
        )

        val annotatedString = buildAnnotatedString {
            withStyle(bigStyle) {
                append("$threshold")
            }
            withStyle(
                style = SpanStyle(
                    color = Color.Black,
                    fontSize = 16.sp
                )
            ) {
                append(" ${stringResource(R.string.of)} ")
            }
            withStyle(bigStyle) {
                append("$total")
            }
        }

        if (boxStyle) {
            Text(
                text = stringResource(R.string.to_access_your_seed_phrases),
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
            )
        }

        Text(
            text = annotatedString,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.approvers_are_required_for_access),
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
        )
    }
}

@Composable
fun ProtectionPlanTitle(
    text: String,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 24.dp
) {
    Spacer(modifier = Modifier.height(topPadding))

    Text(
        text = text,
        color = Color.Black,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(bottomPadding))
}

@Composable
fun AddAnotherButton(
    onClick: () -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 48.dp),
        border = BorderStroke(1.dp, PrimaryBlue),
        shape = RoundedCornerShape(4.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                stringResource(R.string.add_another_approver),
                tint = PrimaryBlue
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.select_another),
                color = PrimaryBlue,
                fontSize = 17.sp,
                fontWeight = FontWeight.W400
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdSlider(
    sliderPosition: Float,
    totalPadding: PaddingValues = PaddingValues(),
    iconAndLabelHorizontalPadding: Dp,
    guardians: List<Guardian>,
    onValueChange: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.padding(totalPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = iconAndLabelHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for ((index, _) in guardians.withIndex()) {
                if (index < sliderPosition.toInt()) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        "",
                        tint = PrimaryBlue,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PhonelinkErase,
                        "",
                        tint = PrimaryBlue,
                    )
                }
            }
        }

        val sliderColors = SliderDefaults.colors(
            activeTrackColor = PrimaryBlue,
            inactiveTickColor = Colors.InactiveSliderGrey,
            thumbColor = Color.White,
            activeTickColor = PrimaryBlue,
            inactiveTrackColor = Colors.InactiveSliderGrey,
            disabledActiveTrackColor = Colors.InactiveSliderGrey,
            disabledInactiveTrackColor = Colors.InactiveSliderGrey
        )

        Slider(
            value = sliderPosition,
            onValueChange = onValueChange,
            steps = guardians.size - 2,
            valueRange = 1f..guardians.size.toFloat(),
            colors = sliderColors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(30.dp, 30.dp),
                    colors = sliderColors,
                    enabled = true
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = iconAndLabelHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for ((index, _) in guardians.withIndex()) {
                Text(
                    text = "${index + 1}",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.W700,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun AmountGuardiansProtectingText(amountGuardians: Int) {
    val normalGreyStyle = SpanStyle(
        color = LabelText,
        fontSize = 18.sp
    )

    val annotatedString = buildAnnotatedString {
        withStyle(normalGreyStyle) {
            append(stringResource(R.string.you_have_selected))
        }
        withStyle(
            style = SpanStyle(
                color = Color.Black,
                fontWeight = FontWeight.W700,
                fontSize = 18.sp
            )
        ) {
            append(" $amountGuardians ")
        }
        withStyle(normalGreyStyle) {
            append(stringResource(R.string.approvers_to_help_you_access_your_seed_phrases))
        }
    }

    Text(text = annotatedString, textAlign = TextAlign.Center)
}

@Composable
fun ApproverRow(
    approver: Guardian.SetupGuardian,
    horizontalPadding: Dp = 16.dp,
    editGuardianClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.approver),
                    color = LabelText,
                    fontSize = 14.sp,
                )
                Text(
                    text = approver.label,
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W700
                )
            }
            IconButton(onClick = editGuardianClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_approver),
                    tint = PrimaryBlue
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(
            modifier = Modifier.padding(start = horizontalPadding),
            thickness = 1.dp,
            color = DividerGray
        )
    }
}

@Composable
fun ProtectionPlanExplainerBox(
    modifier: Modifier = Modifier,
    annotatedString: AnnotatedString,
    paddingValues: PaddingValues = PaddingValues(),
    contentPaddingValues: PaddingValues = PaddingValues()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .border(
                width = 1.dp,
                color = GreyText.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp)
            )
            .shadow(elevation = 5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color = Color.White)
    ) {
        Text(
            modifier = Modifier
                .padding(contentPaddingValues)
                .align(Alignment.Center),
            text = annotatedString,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = GreyText
        )
    }
}