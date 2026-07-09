package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DictionaryEntity
import com.example.keyboard.KeyboardDiagnostics
import com.example.ui.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        OptInTopAppBar()
                    }
                ) { innerPadding ->
                    MainSettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OptInTopAppBar() {
        TopAppBar(
            title = {
                Text(
                    text = "Multiling Keyboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun MainSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val layoutState by viewModel.layoutState.collectAsStateWithLifecycle()
    val dictionaries by viewModel.dictionariesState.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingState.collectAsStateWithLifecycle()
    val statusMsg by viewModel.downloadStatusMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    var testText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Show download notices gracefully using standard Toast to keep layout minimal
    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Navigation for minimal design
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Aktivasi", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Tema & Layout", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Kamus", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // Activation Guide
                    Text(
                        text = "Cara Mengaktifkan Papan Ketik",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "1. Ketuk tombol 'Aktifkan di Pengaturan' di bawah ini, lalu aktifkan 'Multiling Keyboard' dari daftar keyboard sistem.",
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aktifkan di Pengaturan")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "2. Ketuk tombol 'Pilih Metode Input' dan ubah keyboard aktif Anda saat ini menjadi 'Multiling Keyboard'.",
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showInputMethodPicker()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pilih Metode Input")
                            }
                        }
                    }

                     Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Uji Coba Papan Ketik Anda",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("Ketuk di sini untuk mencoba keyboard...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Ketuk kolom di atas, lalu coba ketik dengan keyboard Anda untuk menguji kustomisasi tema, tata letak, dan prediksi kata secara langsung.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Trace Diagnostik Layanan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val diagState by KeyboardDiagnostics.state.collectAsStateWithLifecycle()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛠️ Status Koneksi & Binding",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Layanan Dibuat:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (diagState.serviceCreated) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (diagState.serviceCreated) "AKTIF" else "NONAKTIF",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Binding Aktif:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (diagState.isInputConnectionActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (diagState.isInputConnectionActive) "TERIKAT" else "TERPUTUS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fase Lifecycle:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = diagState.currentLifecycleState,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            Text(
                                text = "Aplikasi Fokus: ${diagState.editorPackageName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tipe Kolom: ${diagState.editorInputType} | ID: ${diagState.editorFieldId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Event Logs Terkini:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(Color(0xFF121212), shape = RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                if (diagState.logs.isEmpty()) {
                                    Text(
                                        text = "Belum ada log terekam...",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    val recentLogs = diagState.logs.take(15)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        recentLogs.forEach { logItem ->
                                            Text(
                                                text = "[${logItem.timestamp}] [${logItem.level}] ${logItem.message}",
                                                color = when (logItem.level) {
                                                    "LIFECYCLE" -> Color(0xFF64B5F6)
                                                    "EDITOR_INFO" -> Color(0xFFFFB74D)
                                                    "INPUT_CONNECTION" -> Color(0xFF81C784)
                                                    else -> Color.LightGray
                                                },
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { KeyboardDiagnostics.clearLogs() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Bersihkan Log", fontSize = 11.sp)
                            }
                        }
                    }
                }

                1 -> {
                    // Layout Customization
                    Text(
                        text = "Tata Letak (Layout)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val layouts = listOf("QWERTY", "AZERTY", "QWERTZ", "DVORAK", "COLEMAK", "NUMERIC")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Layout: ${layoutState.layoutType}")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                layouts.forEach { lType ->
                                    DropdownMenuItem(
                                        text = { Text(lType) },
                                        onClick = {
                                            viewModel.updateLayout(layoutType = lType)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = layoutState.showNumberRow,
                            onCheckedChange = { viewModel.updateLayout(showNumberRow = it) }
                        )
                        Text("Tampilkan baris angka (0-9)")
                    }

                    // Key Spacing
                    Text("Jarak Antar Tombol: ${layoutState.keySpacingDp} dp", fontSize = 14.sp)
                    Slider(
                        value = layoutState.keySpacingDp.toFloat(),
                        onValueChange = { viewModel.updateLayout(keySpacingDp = it.toInt()) },
                        valueRange = 2f..10f,
                        steps = 8
                    )

                    HorizontalDivider()

                    // Theme Presets & Customizations
                    Text(
                        text = "Tema & Warna (Pilih Preset)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePresetCard(
                            title = "Gelap",
                            bg = 0xFF1C1C1E,
                            keyBg = 0xFF2C2C2E,
                            txt = 0xFFFFFFFF,
                            activeBg = 0xFF48484A,
                            isSelected = themeState.backgroundColor == 0xFF1C1C1E && themeState.keyTextColor == 0xFFFFFFFF,
                            onClick = {
                                viewModel.updateTheme(
                                    backgroundColor = 0xFF1C1C1E,
                                    keyBackgroundColor = 0xFF2C2C2E,
                                    keyTextColor = 0xFFFFFFFF,
                                    activeKeyBackgroundColor = 0xFF48484A
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemePresetCard(
                            title = "Terang",
                            bg = 0xFFF2F2F7,
                            keyBg = 0xFFFFFFFF,
                            txt = 0xFF000000,
                            activeBg = 0xFFD1D1D6,
                            isSelected = themeState.backgroundColor == 0xFFF2F2F7 && themeState.keyTextColor == 0xFF000000,
                            onClick = {
                                viewModel.updateTheme(
                                    backgroundColor = 0xFFF2F2F7,
                                    keyBackgroundColor = 0xFFFFFFFF,
                                    keyTextColor = 0xFF000000,
                                    activeKeyBackgroundColor = 0xFFD1D1D6
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePresetCard(
                            title = "Biru Laut",
                            bg = 0xFF1B2A47,
                            keyBg = 0xFF253E66,
                            txt = 0xFFFFFFFF,
                            activeBg = 0xFF3A5F99,
                            isSelected = themeState.backgroundColor == 0xFF1B2A47,
                            onClick = {
                                viewModel.updateTheme(
                                    backgroundColor = 0xFF1B2A47,
                                    keyBackgroundColor = 0xFF253E66,
                                    keyTextColor = 0xFFFFFFFF,
                                    activeKeyBackgroundColor = 0xFF3A5F99
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemePresetCard(
                            title = "Hutan",
                            bg = 0xFF1E281E,
                            keyBg = 0xFF2E3E2E,
                            txt = 0xFFFFFFFF,
                            activeBg = 0xFF425C42,
                            isSelected = themeState.backgroundColor == 0xFF1E281E,
                            onClick = {
                                viewModel.updateTheme(
                                    backgroundColor = 0xFF1E281E,
                                    keyBackgroundColor = 0xFF2E3E2E,
                                    keyTextColor = 0xFFFFFFFF,
                                    activeKeyBackgroundColor = 0xFF425C42
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sliders for corner radius and height
                    Text("Tinggi Tombol: ${themeState.keyHeightDp} dp", fontSize = 14.sp)
                    Slider(
                        value = themeState.keyHeightDp.toFloat(),
                        onValueChange = { viewModel.updateTheme(keyHeightDp = it.toInt()) },
                        valueRange = 40f..70f,
                        steps = 30
                    )

                    Text("Sudut Kelengkungan Tombol (Radius): ${themeState.borderRadius} dp", fontSize = 14.sp)
                    Slider(
                        value = themeState.borderRadius.toFloat(),
                        onValueChange = { viewModel.updateTheme(borderRadius = it.toInt()) },
                        valueRange = 0f..20f,
                        steps = 20
                    )
                }

                2 -> {
                    // Downloadable Dictionaries
                    Text(
                        text = "Manajemen Kamus Bahasa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Unduh kamus untuk mengaktifkan prediksi kata yang sangat akurat. Bahasa Inggris 'English' selalu aktif secara default.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    dictionaries.forEach { dict ->
                        val isDefault = dict.languageCode in listOf("en", "id", "es", "fr")
                        DictionaryRow(
                            dict = dict,
                            isDownloading = downloading[dict.languageCode] == true,
                            onDownload = { viewModel.downloadDictionary(dict) },
                            onDelete = { viewModel.deleteDictionary(dict) },
                            onRemove = if (!isDefault) { { viewModel.removeDictionary(dict) } } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Form to add custom dictionary
                    Text(
                        text = "Tambah Kamus Kustom",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Masukkan URL daftar kata (wordlist) text offline untuk menambah dukungan bahasa kustom.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var customName by remember { mutableStateOf("") }
                    var customCode by remember { mutableStateOf("") }
                    var customUrl by remember { mutableStateOf("") }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nama Bahasa (misal: Jerman)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customCode,
                        onValueChange = { customCode = it },
                        label = { Text("Kode Bahasa (misal: de)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("URL Unduhan (.txt newline separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.addDictionary(customCode, customName, customUrl)
                            // Clear inputs
                            customName = ""
                            customCode = ""
                            customUrl = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambah Kamus Baru")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggested Dictionaries list
                    Text(
                        text = "Rekomendasi Kamus Tambahan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ketuk untuk menambahkan salah satu bahasa populer berikut ke daftar manajemen kamus Anda.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val recommended = listOf(
                        Triple("Deutsch (Jerman)", "de", "https://raw.githubusercontent.com/damz/german-word-list/master/german-word-list.txt"),
                        Triple("Italiano (Italia)", "it", "https://raw.githubusercontent.com/napolux/parole-italiane/master/parole-italiane.txt"),
                        Triple("Português (Portugis)", "pt", "https://raw.githubusercontent.com/python-pro/wordlist/master/portuguese.txt"),
                        Triple("Русский (Rusia)", "ru", "https://raw.githubusercontent.com/danakt/russian-words/master/russian-words.txt")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    recommended.forEach { (name, code, url) ->
                        val isAlreadyAdded = dictionaries.any { it.languageCode == code }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Kode: $code", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (isAlreadyAdded) {
                                    Text(
                                        text = "Sudah Ada",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.addDictionary(code, name, url)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("Tambah", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePresetCard(
    title: String,
    bg: Long,
    keyBg: Long,
    txt: Long,
    activeBg: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(bg))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color(txt),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Preview keys row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Q", "W", "E").forEach { keyChar ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(Color(keyBg), shape = RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = keyChar,
                            color = Color(txt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(28.dp)
                        .background(Color(activeBg), shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Space",
                        color = Color(txt),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DictionaryRow(
    dict: DictionaryEntity,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dict.languageName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (dict.isDownloaded) {
                        "Aktif (${dict.wordCount} kata)"
                    } else {
                        "Belum diunduh"
                    },
                    fontSize = 13.sp,
                    color = if (dict.isDownloaded) Color(0xFF4CAF50) else Color.Gray
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dict.languageCode == "en") {
                        // English default is always on, no option to delete to guarantee basic predictor is always active
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Unduh Ulang", fontSize = 12.sp)
                        }
                    } else if (dict.isDownloaded) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onDownload
                        ) {
                            Text("Unduh", fontSize = 12.sp)
                        }
                    }

                    if (onRemove != null) {
                        Button(
                            onClick = onRemove,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Buang", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
