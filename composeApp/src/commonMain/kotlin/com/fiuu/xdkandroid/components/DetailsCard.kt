package com.fiuu.xdkandroid.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.model.PaymentUiState
import com.fiuu.xdkandroid.ui.theme.AppColors

@Composable
fun OrderDetailsCard(
    state: PaymentUiState,
    nameEditable: Boolean,
    emailEditable: Boolean,
    mobileEditable: Boolean,
    descriptionEditable: Boolean,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onDesc: (String) -> Unit
) {
    ExpandableCardPayment(
        label = "Order Details",
        initiallyExpanded = false,
        backgroundColor = AppColors.White,
        headerContent = {
            Column {
                CustomText(
                    label = "ID ${state.orderId}",
                )
            }
        },
        expandedContent = {
            TopLabelOutlinedTextField(
                value = state.name,
                onValueChange = onName,
                label = "Name",
                isError = state.formError != null,
                errorText = "Name cannot be empty",
                enabled = nameEditable,
                trailingIcon = AppIcons.Person,
            )
            TopLabelOutlinedTextField(
                value = state.email,
                onValueChange = onEmail,
                label = "Email",
                isError = state.formError != null,
                errorText = "Invalid email address",
                enabled = emailEditable,
                trailingIcon = AppIcons.Email,
            )
            TopLabelOutlinedTextField(
                value = state.mobile,
                onValueChange = onPhone,
                label = "Mobile",
                errorText = "Invalid phone number",
                enabled = mobileEditable,
                trailingIcon = AppIcons.Phone,
            )
            TopLabelOutlinedTextField(
                value = state.description,
                onValueChange = onDesc,
                label = "Description",
                enabled = descriptionEditable,
                trailingIcon = AppIcons.Description,
            )
        }

    )
}