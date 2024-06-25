package xyz.kgy_production.webdavebookmanager.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import arrow.core.Option
import arrow.core.none
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.matchParentWidth
import xyz.kgy_production.webdavebookmanager.viewmodel.EditWebDavEntryViewModel

@Composable
fun EditWebDavEntryScreen(
    uuid: Option<String>,
    onBack: () -> Unit,
    viewModel: EditWebDavEntryViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val model by viewModel.data.collectAsStateWithLifecycle()
    var errorMessage by remember { mutableStateOf("") }

    if (uuid.isSome()) {
        val _uuid = uuid.getOrNull()!!
        LaunchedEffect(key1 = _uuid) {
            coroutineScope.launch {
                viewModel.setModelByUuid(_uuid).onLeft {
                    Log.e("EditWebDavEntryScreen", it)
                }
            }
        }
    }
    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(id = R.string.screen_edit_webdav_entry_title),
                onBack = onBack
            )
        },
        bottomBar = {
            BottomBar(
                toReset = viewModel::resetModel,
                toSubmit = {
                    coroutineScope.launch {
                        val err = if (uuid.isNone()) {
                            viewModel.addWebDavEntry()
                        } else {
                            viewModel.editWebEntry()
                        }
                        if (err.isNone()) {
                            onBack()
                        } else {
                            errorMessage = err.getOrNull()!!
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (errorMessage.isNotEmpty())
                Text(text = errorMessage, color = Color.Red, fontWeight = FontWeight.Bold)
            InputField(
                label = stringResource(id = R.string.label_webdav_id),
                value = model.uuid.let { it.ifEmpty { "-" } },
                readOnly = true
            )
            InputField(
                label = stringResource(id = R.string.label_webdav_name),
                value = model.name,
            ) {
                viewModel.updateModel(model.copy(name = it))
            }
            InputField(
                label = stringResource(id = R.string.label_webdav_url),
                value = model.url,
                isRequired = true
            ) {
                viewModel.updateModel(model.copy(url = it))
            }
            InputField(
                label = stringResource(id = R.string.label_webdav_login_id),
                value = model.loginId,
                isRequired = true
            ) {
                viewModel.updateModel(model.copy(loginId = it))
            }
            InputField(
                label = stringResource(id = R.string.label_webdav_password),
                value = model.password,
                isRequired = true,
                isPassword = true
            ) {
                viewModel.updateModel(model.copy(password = it))
            }
        }
    }
}

@Composable
private fun BottomBar(
    toReset: () -> Unit,
    toSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = toReset, modifier = Modifier.padding(10.dp)) {
            Text(text = stringResource(id = R.string.btn_reset))
        }
        Button(onClick = toSubmit, modifier = Modifier.padding(10.dp)) {
            Text(text = stringResource(id = R.string.btn_submit))
        }
    }
}

@Composable
private fun RequiredField(
    field: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            stringResource(id = R.string.label_common_required),
            modifier = Modifier
                .widthIn(max = 20.dp)
                .heightIn(max = 20.dp)
        )
        field()
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    isRequired: Boolean = false,
    readOnly: Boolean = false,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }

    @Composable
    fun TF(modifier: Modifier = Modifier) {
        TextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label) },
            readOnly = readOnly,
            singleLine = true,
            label = { Text(label) },
            visualTransformation = if (!isPassword || visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            trailingIcon = {
                if (isPassword) {
                    val icon = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val desc =
                        stringResource(id = if (visible) R.string.label_common_hide_password else R.string.label_common_show_password)
                    IconButton(onClick = { visible = !visible }) {
                        Icon(icon, desc)
                    }
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .wrapContentHeight()
    ) {
        if (isRequired)
            RequiredField {
                TF()
            }
        else
            TF(Modifier.padding(start = 20.dp))
    }
}

@Preview
@Composable
private fun EditWebDavEntryScreenPreview() {
    EditWebDavEntryScreen(
        uuid = none(),
        onBack = {}
    )
}