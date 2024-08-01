package xyz.kgy_production.webdavebookmanager.ui.screens

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.EditWebDavEntryViewModel

@Composable
fun EditWebDavEntryScreen(
    uuid: String?,
    onBack: () -> Unit,
    viewModel: EditWebDavEntryViewModel = hiltViewModel(),
) {
    val logger by Logger.delegate("EditWebDavEntryScr")
    val coroutineScope = rememberCoroutineScope()
    val model by viewModel.data.collectAsStateWithLifecycle()
    var errorMessage by remember { mutableStateOf("") }

    uuid?.let { it ->
        LaunchedEffect(key1 = it) {
            coroutineScope.launch {
                viewModel.setModelByUuid(it).getError()?.let(logger::e)
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
                        val err = if (uuid == null) {
                            viewModel.addWebDavEntry()
                        } else {
                            viewModel.editWebEntry()
                        }
                        if (err == null) {
                            onBack()
                        } else {
                            errorMessage = err
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
            Row {
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = "Open ebook with this app"
                ) // TODO i18n
                Row(modifier = Modifier.weight(1.0f)) {}
                Switch(checked = model.defaultOpenByThis, onCheckedChange = {
                    viewModel.updateModel(
                        model.copy(
                            defaultOpenByThis = it
                        )
                    )
                })
            }

            Text(text = stringResource(id = R.string.label_webdav_bypass_pattern))
            model.bypassPattern.forEachIndexed { inputIdx, inputByPassPattern ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(.9f),
                        value = inputByPassPattern.pattern,
                        onValueChange = {
                            viewModel.updateModel(model.copy(
                                bypassPattern = model.bypassPattern.mapIndexed { _idx, byPassPattern ->
                                    if (_idx == inputIdx) inputByPassPattern.copy(pattern = it)
                                    else byPassPattern
                                }
                            ))
                        }
                    )
                    Checkbox(
                        modifier = Modifier.weight(.1f),
                        checked = inputByPassPattern.isRegex,
                        onCheckedChange = {
                            viewModel.updateModel(model.copy(bypassPattern =
                            model.bypassPattern.mapIndexed { _idx, byPassPattern ->
                                if (_idx == inputIdx) inputByPassPattern.copy(isRegex = it)
                                else byPassPattern
                            }
                            ))
                        }
                    )
                }
            }
            TextButton(onClick = {
                viewModel.updateModel(
                    model.copy(
                        bypassPattern = model.bypassPattern + WebDavModel.ByPassPattern(
                            "",
                            false
                        )
                    )
                )
            }) {
                Text(text = stringResource(id = R.string.label_webdav_add_bypass_pattern))
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.Absolute.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = toReset, modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        ) {
            Text(text = stringResource(id = R.string.btn_reset))
        }
        Row(modifier = Modifier.weight(1.0f)) {}
        Button(
            onClick = toSubmit, modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        ) {
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
        uuid = null,
        onBack = {}
    )
}