package co.censo.censo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import co.censo.censo.R
import co.censo.shared.presentation.components.ConfirmationDialog

@Composable
fun DeleteUserConfirmationUI(
    title: String,
    seedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append(stringResource(id = R.string.about_to_delete_user_first_half))
        append(" ")

        withStyle(SpanStyle(fontWeight = FontWeight.W500)) {
            append(stringResource(id = R.string.all))
        }

        append(" ")

        append(stringResource(id = R.string.about_to_delete_user_second_half))
        append(
            pluralStringResource(
                id = R.plurals.delete_my_data_confirmation_text,
                count = seedCount,
                seedCount
            )
        )
    }

    ConfirmationDialog(
        title = title,
        message = annotatedString,
        confirmationText = pluralStringResource(id = R.plurals.delete_my_data_confirmation_text, count = seedCount, seedCount),
        onCancel = onCancel,
        onDelete = onDelete,
    )

}

