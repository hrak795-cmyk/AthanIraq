package com.athaniqraq.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.athaniqraq.app.data.*
import com.athaniqraq.app.notifications.Notifications
import com.athaniqraq.app.ui.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

class MainActivity : ComponentActivity() {
    private val repo by lazy { PrayerRepository() }
    private val settings by lazy { SettingsRepository(this) }
    private var location by mutableStateOf<Pair<Double, Double>?>(null)
    private var prayers by mutableStateOf<List<PrayerTime>>(emptyList())
    private var error by mutableStateOf<String?>(null)
    private var maghribOffset by mutableIntStateOf(12)
    private var tasbihCount by mutableIntStateOf(0)
    private var target by mutableIntStateOf(33)
    private var fontSize by mutableIntStateOf(22)
    private var bookmarks by mutableStateOf<Set<String>>(emptySet())
    private var language by mutableStateOf("ar")
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { loadLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.createChannel(this)
        lifecycleScope.launch {
            settings.maghribOffset.collect { maghribOffset = it; refreshTimes() }
        }
        lifecycleScope.launch { settings.tasbih.collect { tasbihCount = it } }
        lifecycleScope.launch { settings.target.collect { target = it } }
        lifecycleScope.launch { settings.fontSize.collect { fontSize = it } }
        lifecycleScope.launch { settings.bookmarks.collect { bookmarks = it } }
        lifecycleScope.launch { settings.language.collect { language = it } }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) loadLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        setContent { var dark by remember { mutableStateOf(false) }; AthanTheme(dark) {
            MainUi(prayers, error, location, maghribOffset, tasbihCount, target, fontSize, bookmarks, language,
                onOffset = { lifecycleScope.launch { settings.setOffset(it) } },
                onTasbih = { lifecycleScope.launch { settings.setTasbih(it) } },
                onTarget = { lifecycleScope.launch { settings.setTarget(it) } },
                onFont = { lifecycleScope.launch { settings.setFontSize(it) } },
                onBookmark = { id -> lifecycleScope.launch { settings.toggleBookmark(id) } },
                onLanguage = { lifecycleScope.launch { settings.setLanguage(it) } },
                onDark = { dark = it }, onRefresh = { refreshTimes() })
        } }
    }
    private fun loadLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { error = getString(R.string.location_required); return }
        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) { location = loc.latitude to loc.longitude; refreshTimes() }
                else error = "تعذر تحديد الموقع. فعّل GPS وحاول مرة ثانية."
            }.addOnFailureListener { error = it.localizedMessage ?: "تعذر تحديد الموقع" }
    }
    private fun refreshTimes() {
        val loc = location ?: return
        lifecycleScope.launch {
            try { prayers = repo.getTimes(loc.first, loc.second, maghribOffset); error = null }
            catch (e: Exception) { error = "تعذر جلب أوقات الصلاة: ${e.localizedMessage ?: "تحقق من الإنترنت"}" }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainUi(
    prayers: List<PrayerTime>, error: String?, location: Pair<Double, Double>?,
    offset: Int, count: Int, target: Int, fontSize: Int, bookmarks: Set<String>, language: String,
    onOffset: (Int)->Unit, onTasbih: (Int)->Unit, onTarget: (Int)->Unit, onFont: (Int)->Unit,
    onBookmark: (String)->Unit, onLanguage: (String)->Unit, onDark: (Boolean)->Unit, onRefresh: ()->Unit
) {
    val labels = listOf("الصلاة", "القبلة", "الأدعية", "السبحة", "التقويم")
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("أذان العراق", fontSize = 22.sp) }, actions = {
            var show by remember { mutableStateOf(false) }
            IconButton(onClick = { show = true }) { Icon(Icons.Default.Settings, "الإعدادات") }
            DropdownMenu(expanded = show, onDismissRequest = { show = false }) {
                DropdownMenuItem(text = { Text("المغرب: ${offset} دقيقة") }, onClick = { onOffset((offset + 1).coerceAtMost(60)); show = false })
                DropdownMenuItem(text = { Text("تصغير خط الأدعية") }, onClick = { onFont(fontSize - 2); show = false })
                DropdownMenuItem(text = { Text("تكبير خط الأدعية") }, onClick = { onFont(fontSize + 2); show = false })
                DropdownMenuItem(text = { Text("العربية") }, onClick = { onLanguage("ar"); show = false })
                DropdownMenuItem(text = { Text("中文") }, onClick = { onLanguage("zh"); show = false })
                DropdownMenuItem(text = { Text("Русский") }, onClick = { onLanguage("ru"); show = false })
            }
        }) },
        bottomBar = { NavigationBar { labels.forEachIndexed { i, label ->
            NavigationBarItem(selected = tab == i, onClick = { tab = i },
                icon = { Icon(listOf(Icons.Default.AccessTime, Icons.Default.Explore, Icons.Default.MenuBook, Icons.Default.Favorite, Icons.Default.CalendarMonth)[i], null) },
                label = { Text(label) })
        } } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            when(tab) {
                0 -> PrayerScreen(prayers, error, onRefresh)
                1 -> QiblaScreen(location)
                2 -> DuaScreen(fontSize, bookmarks, onBookmark)
                3 -> TasbihScreen(count, target, onTasbih, onTarget)
                4 -> CalendarScreen()
            }
        }
    }
}

@Composable
private fun PrayerScreen(prayers: List<PrayerTime>, error: String?, onRefresh: () -> Unit) {
    val now = LocalTime.now()
    val next = prayers.firstOrNull { runCatching { LocalTime.parse(it.time) }.getOrNull()?.isAfter(now) == true }
    Card(colors = CardDefaults.cardColors(containerColor = DeepGreen), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("أوقات الصلاة حسب منهج أهل البيت", color = Gold, fontSize = 18.sp)
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", java.util.Locale("ar"))), color = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.height(10.dp))
            Text("الصلاة القادمة: ${next?.name ?: "الفجر"} ${next?.time ?: ""}", color = androidx.compose.ui.graphics.Color.White, fontSize = 21.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
    if (prayers.isEmpty()) Button(onClick = onRefresh) { Text("تحميل أوقات الصلاة") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(prayers) { p ->
            val isNext = p == next
            Card(colors = CardDefaults.cardColors(containerColor = if (isNext) Gold.copy(alpha = .22f) else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.name, fontSize = 19.sp); Text(p.time, fontSize = 19.sp)
                }
            }
        }
    }
    Text("تنبيه: API طريقة 7 أساس للحساب؛ راجع التوقيت المحلي والمرجع الشرعي المعتمد.", fontSize = 12.sp)
}

@Composable
private fun QiblaScreen(location: Pair<Double, Double>?) {
    var azimuth by remember { mutableFloatStateOf(0f) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(location) {
        val manager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val sensor = manager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                val rotation = FloatArray(9); val orientation = FloatArray(3)
                manager.getRotationMatrixFromVector(rotation, event.values)
                manager.getOrientation(rotation, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        if (sensor != null) manager.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    val bearing = location?.let { qiblaBearing(it.first, it.second) }
    val distance = location?.let { distanceKm(it.first, it.second, 21.4225, 39.8262) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("اتجاه القبلة", fontSize = 24.sp)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(240.dp).border(3.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
            Text("N", Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
            Icon(Icons.Default.Navigation, null, modifier = Modifier.size(120.dp).rotate(((bearing ?: 0f) - azimuth)), tint = DeepGreen)
        }
        Spacer(Modifier.height(20.dp))
        Text(if (bearing != null) "اتجاه مكة: ${"%.1f".format(bearing)}°" else "فعّل الموقع لحساب اتجاه القبلة", fontSize = 20.sp)
        Text(distance?.let { "المسافة إلى مكة: ${it.roundToInt()} كم" } ?: "")
        Text("ابتعد عن المعادن وعاير البوصلة إذا كان الاتجاه غير ثابت.", fontSize = 13.sp)
    }
}
private fun qiblaBearing(lat: Double, lon: Double): Float {
    val a = Math.toRadians(lat); val b = Math.toRadians(lon)
    val c = Math.toRadians(21.4225); val d = Math.toRadians(39.8262)
    val y = sin(d-b); val x = cos(a)*tan(c)-sin(a)*cos(d-b)
    return ((Math.toDegrees(atan2(y,x))+360)%360).toFloat()
}
private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r=6371.0; val dLat=Math.toRadians(lat2-lat1); val dLon=Math.toRadians(lon2-lon1)
    val a=sin(dLat/2).pow(2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2).pow(2)
    return 2*r*asin(sqrt(a))
}

@Composable
private fun DuaScreen(fontSize: Int, bookmarks: Set<String>, onBookmark: (String)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val duas = remember {
        runCatching {
            val json = org.json.JSONArray(context.assets.open("duas.json").bufferedReader().use { it.readText() })
            (0 until json.length()).map { i -> json.getJSONObject(i).let { Dua(it.getString("id"),it.getString("title"),it.getString("text"),it.optString("note")) } }
        }.getOrElse { emptyList() }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(duas) { dua ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dua.title, Modifier.weight(1f), fontSize = 20.sp, color = DeepGreen)
                        IconButton(onClick = { onBookmark(dua.id) }) {
                            Icon(if (dua.id in bookmarks) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "حفظ")
                        }
                    }
                    Text(dua.text, fontSize = fontSize.sp, lineHeight = (fontSize+12).sp, textAlign = TextAlign.Start)
                    if (dua.note.isNotBlank()) Text(dua.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TasbihScreen(count: Int, target: Int, onCount: (Int)->Unit, onTarget: (Int)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("السبحة الإلكترونية", fontSize = 24.sp)
        Spacer(Modifier.height(22.dp))
        Text("$count", fontSize = 58.sp, color = DeepGreen)
        Text("الهدف: $target")
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            (context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator).let {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    it.vibrate(android.os.VibrationEffect.createOneShot(35, 80))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(35)
                }
            }
            onCount(count + 1)
        }, modifier = Modifier.size(190.dp), shape = CircleShape) { Text("سَبِّح", fontSize = 28.sp) }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(33,99,100).forEach { n -> OutlinedButton(onClick = { onTarget(n) }) { Text("$n") } }
        }
        OutlinedButton(onClick = { onCount(0) }) { Text("تصفير العداد") }
        if (count >= target) Text("تقبّل الله طاعتكم 🌙", color = DeepGreen)
    }
}

@Composable
private fun CalendarScreen() {
    var hijri by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("التقويم", fontSize = 24.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("هجري")
            Switch(checked = hijri, onCheckedChange = { hijri = it })
            Text("ميلادي")
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(if (hijri) HijrahDate.now().toString() else today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ar"))), fontSize = 24.sp)
                Text(if (hijri) "التاريخ الهجري وفق تقويم الجهاز؛ قد يختلف يومًا حسب الرؤية." else "التاريخ الميلادي")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("مناسبات إسلامية", fontSize = 20.sp)
        listOf("1 محرم — رأس السنة الهجرية", "10 محرم — عاشوراء", "17 ربيع الأول — مولد النبي (ص)", "13 رجب — مولد الإمام علي (ع)", "15 شعبان — ولادة الإمام المهدي (ع)", "1 رمضان — بداية شهر رمضان (تقريبًا)", "1 شوال — عيد الفطر (تقريبًا)", "10 ذو الحجة — عيد الأضحى (تقريبًا)").forEach {
            ListItem(headlineContent = { Text(it) })
        }
    }
}
