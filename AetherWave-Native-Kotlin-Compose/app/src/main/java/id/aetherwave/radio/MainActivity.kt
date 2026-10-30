package id.aetherwave.radio

import android.content.Context
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

data class NativeStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val fallbackUrl: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val tags: String,
    val bitrate: Int,
    val frequency: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

data class CountryNode(
    val code: String,
    val name: String,
    val flag: String,
    val timezone: String,
    val mapX: Float, // 0f..1f
    val mapY: Float  // 0f..1f
)

val GLOBAL_COUNTRIES = listOf(
    CountryNode("ID", "Indonesia", "🇮🇩", "Asia/Jakarta", 0.78f, 0.58f),
    CountryNode("JP", "Jepang", "🇯🇵", "Asia/Tokyo", 0.86f, 0.34f),
    CountryNode("KR", "Korea Selatan", "🇰🇷", "Asia/Seoul", 0.82f, 0.33f),
    CountryNode("GB", "Inggris Raya", "🇬🇧", "Europe/London", 0.47f, 0.24f),
    CountryNode("US", "Amerika Serikat", "🇺🇸", "America/New_York", 0.23f, 0.32f),
    CountryNode("FR", "Prancis", "🇫🇷", "Europe/Paris", 0.49f, 0.28f),
    CountryNode("DE", "Jerman", "🇩🇪", "Europe/Berlin", 0.51f, 0.26f),
    CountryNode("BR", "Brasil", "🇧🇷", "America/Sao_Paulo", 0.33f, 0.68f),
    CountryNode("SA", "Arab Saudi", "🇸🇦", "Asia/Riyadh", 0.60f, 0.41f),
    CountryNode("AU", "Australia", "🇦🇺", "Australia/Sydney", 0.86f, 0.76f)
)

val BUILTIN_STATIONS = listOf(
    NativeStation(
        id = "id-prambors",
        name = "Prambors FM 102.2 Jakarta",
        streamUrl = "https://s2.cloudmu.id/listen/prambors/radio.mp3",
        fallbackUrl = "https://ice1.somafm.com/indiepop-128-mp3",
        country = "Indonesia",
        countryCode = "ID",
        city = "DKI Jakarta",
        tags = "Top 40 · Pop Hits · Anak Muda",
        bitrate = 128,
        frequency = "102.2 FM",
        primaryColor = Color(0xFFFACC15),
        secondaryColor = Color(0xFFF97316)
    ),
    NativeStation(
        id = "id-jazz",
        name = "Cosmopolitan & Jazz Nusantara",
        streamUrl = "https://ice1.somafm.com/lush-128-mp3",
        fallbackUrl = "https://ice2.somafm.com/lush-128-mp3",
        country = "Indonesia",
        countryCode = "ID",
        city = "Jakarta Selatan",
        tags = "Urban Jazz · Chillout · Vocal",
        bitrate = 128,
        frequency = "87.6 FM",
        primaryColor = Color(0xFFF472B6),
        secondaryColor = Color(0xFFA855F7)
    ),
    NativeStation(
        id = "id-hemat",
        name = "Suara Nusantara Low-Data 64k",
        streamUrl = "https://ice1.somafm.com/groovesalad-64-aac",
        fallbackUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
        country = "Indonesia",
        countryCode = "ID",
        city = "Bandung",
        tags = "Acoustic · Ambient · Hemat Kuota",
        bitrate = 64,
        frequency = "90.0 FM",
        primaryColor = Color(0xFF34D399),
        secondaryColor = Color(0xFF14B8A6)
    ),
    NativeStation(
        id = "jp-shibuya",
        name = "J-Pop Sakura & Shibuya FM",
        streamUrl = "https://listen.moe/stream",
        fallbackUrl = "https://ice1.somafm.com/vaporwaves-128-mp3",
        country = "Jepang",
        countryCode = "JP",
        city = "Tokyo",
        tags = "J-Pop · City Pop · Shibuya · Anime",
        bitrate = 192,
        frequency = "81.3 FM",
        primaryColor = Color(0xFFF472B6),
        secondaryColor = Color(0xFF818CF8)
    ),
    NativeStation(
        id = "jp-lofi",
        name = "Tokyo Midnight Lo-Fi & Jazz",
        streamUrl = "https://ice1.somafm.com/vaporwaves-128-mp3",
        fallbackUrl = "https://ice2.somafm.com/vaporwaves-128-mp3",
        country = "Jepang",
        countryCode = "JP",
        city = "Osaka",
        tags = "Vaporwave · Future Funk · Lo-Fi",
        bitrate = 128,
        frequency = "89.7 FM",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFFA855F7)
    ),
    NativeStation(
        id = "kr-seoul",
        name = "Seoul K-Wave & Hongdae Live",
        streamUrl = "https://listen.moe/kpop/stream",
        fallbackUrl = "https://ice1.somafm.com/poptron-128-mp3",
        country = "Korea Selatan",
        countryCode = "KR",
        city = "Seoul",
        tags = "K-Pop · K-Indie · Dance · Charts",
        bitrate = 192,
        frequency = "95.9 FM",
        primaryColor = Color(0xFFA78BFA),
        secondaryColor = Color(0xFFEC4899)
    ),
    NativeStation(
        id = "gb-london",
        name = "London Soho Electronic HD",
        streamUrl = "https://ice1.somafm.com/groovesalad-256-mp3",
        fallbackUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
        country = "Inggris Raya",
        countryCode = "GB",
        city = "London",
        tags = "Downtempo · UK Chill · Audiophile",
        bitrate = 256,
        frequency = "98.8 FM",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF3B82F6)
    ),
    NativeStation(
        id = "us-paradise",
        name = "Radio Paradise California",
        streamUrl = "https://stream.radioparadise.com/mp3-192",
        fallbackUrl = "https://ice1.somafm.com/indiepop-128-mp3",
        country = "Amerika Serikat",
        countryCode = "US",
        city = "California",
        tags = "Eclectic · Rock · World · HD",
        bitrate = 192,
        frequency = "92.5 FM",
        primaryColor = Color(0xFFFB923C),
        secondaryColor = Color(0xFFF43F5E)
    ),
    NativeStation(
        id = "us-nyc-jazz",
        name = "Manhattan Velvet Lounge NYC",
        streamUrl = "https://ice1.somafm.com/secretagent-128-mp3",
        fallbackUrl = "https://ice2.somafm.com/secretagent-128-mp3",
        country = "Amerika Serikat",
        countryCode = "US",
        city = "New York",
        tags = "Vintage Jazz · Lounge · Cinematic",
        bitrate = 128,
        frequency = "88.3 FM",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFFB923C)
    ),
    NativeStation(
        id = "fr-fip",
        name = "FIP Radio France Paris",
        streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
        fallbackUrl = "https://ice1.somafm.com/lush-128-mp3",
        country = "Prancis",
        countryCode = "FR",
        city = "Paris",
        tags = "Chanson · Jazz · World · Eclectic",
        bitrate = 128,
        frequency = "105.1 FM",
        primaryColor = Color(0xFFA78BFA),
        secondaryColor = Color(0xFF38BDF8)
    ),
    NativeStation(
        id = "de-berlin",
        name = "Berlin Tresor Electronic",
        streamUrl = "https://ice1.somafm.com/thetrip-128-mp3",
        fallbackUrl = "https://ice2.somafm.com/thetrip-128-mp3",
        country = "Jerman",
        countryCode = "DE",
        city = "Berlin",
        tags = "Progressive · Techno · Melodic House",
        bitrate = 128,
        frequency = "97.2 FM",
        primaryColor = Color(0xFF2DD4BF),
        secondaryColor = Color(0xFF38BDF8)
    ),
    NativeStation(
        id = "br-rio",
        name = "Ipanema Sunset Bossa & Samba",
        streamUrl = "https://ice1.somafm.com/suburbsofgoa-128-mp3",
        fallbackUrl = "https://ice1.somafm.com/lush-128-mp3",
        country = "Brasil",
        countryCode = "BR",
        city = "Rio de Janeiro",
        tags = "Bossa Nova · World Beats · Tropical",
        bitrate = 128,
        frequency = "94.9 FM",
        primaryColor = Color(0xFF34D399),
        secondaryColor = Color(0xFFFACC15)
    ),
    NativeStation(
        id = "sa-quran",
        name = "Al-Quran Al-Kareem Radio Riyadh",
        streamUrl = "https://stream.radiojar.com/8s5u5tpdtwzuv",
        fallbackUrl = "https://ice1.somafm.com/dronezone-128-mp3",
        country = "Arab Saudi",
        countryCode = "SA",
        city = "Riyadh / Makkah",
        tags = "Tilawah Al-Quran 24 Jam · Timur Tengah",
        bitrate = 128,
        frequency = "100.0 FM",
        primaryColor = Color(0xFF34D399),
        secondaryColor = Color(0xFF2DD4BF)
    ),
    NativeStation(
        id = "au-sydney",
        name = "Bondi Beach Indie & Surf Radio",
        streamUrl = "https://ice1.somafm.com/indiepop-128-mp3",
        fallbackUrl = "https://ice2.somafm.com/indiepop-128-mp3",
        country = "Australia",
        countryCode = "AU",
        city = "Sydney",
        tags = "Indie Pop · Coastal Rock · Dream Pop",
        bitrate = 128,
        frequency = "105.7 FM",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF2DD4BF)
    )
)

suspend fun fetchRemoteStationsByCountry(countryCode: String): List<NativeStation> = withContext(Dispatchers.IO) {
    val localCurated = BUILTIN_STATIONS.filter { it.countryCode.equals(countryCode, ignoreCase = true) }
    val countryNode = GLOBAL_COUNTRIES.find { it.code.equals(countryCode, ignoreCase = true) }
    val paletteColors = listOf(
        Color(0xFF38BDF8) to Color(0xFF818CF8),
        Color(0xFFF472B6) to Color(0xFFA855F7),
        Color(0xFF34D399) to Color(0xFF14B8A6),
        Color(0xFFFACC15) to Color(0xFFF97316)
    )

    try {
        val url = URL("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/${countryCode.uppercase()}?limit=15&order=votes&reverse=true&hidebroken=true")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 3500
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AetherWaveNativeAndroid/2.0")
        }
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArr = JSONArray(body)
            val result = mutableListOf<NativeStation>()
            result.addAll(localCurated)
            val seenNames = localCurated.map { it.name.lowercase() }.toMutableSet()

            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val resolved = obj.optString("url_resolved", "")
                val rawName = obj.optString("name", "").trim()
                if (resolved.startsWith("https://") && rawName.length > 2 && !seenNames.contains(rawName.lowercase())) {
                    seenNames.add(rawName.lowercase())
                    val pair = paletteColors[i % paletteColors.size]
                    val tagsRaw = obj.optString("tags", "Live Broadcast")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.length in 2..18 }
                        .take(3)
                        .joinToString(" · ")
                        .ifEmpty { "Live Radio · Global" }

                    result.add(
                        NativeStation(
                            id = obj.optString("stationuuid", "remote-$i"),
                            name = rawName,
                            streamUrl = resolved,
                            fallbackUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                            country = countryNode?.name ?: obj.optString("country", countryCode),
                            countryCode = countryCode.uppercase(),
                            city = obj.optString("state", countryNode?.name ?: ""),
                            tags = tagsRaw,
                            bitrate = obj.optInt("bitrate", 128).coerceAtLeast(64),
                            frequency = "${88.1 + ((i * 2.1) % 19.0).toInt()}.5 FM",
                            primaryColor = pair.first,
                            secondaryColor = pair.second
                        )
                    )
                }
            }
            return@withContext result
        }
    } catch (_: Exception) {
        // Fallback to verified built-in stations
    }
    return@withContext localCurated.ifEmpty { BUILTIN_STATIONS }
}

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    private var audioEqualizer: Equalizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exoPlayer = player

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF090D16),
                    surface = Color(0xFF0F172A),
                    primary = Color(0xFF38BDF8),
                    onBackground = Color(0xFFF8FAFC),
                    onSurface = Color(0xFFF8FAFC)
                )
            ) {
                AetherWaveNativeApp(
                    exoPlayer = player,
                    onAttachEqualizer = { sessionId ->
                        try {
                            audioEqualizer?.release()
                            if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
                                audioEqualizer = Equalizer(0, sessionId).apply { enabled = true }
                            }
                        } catch (_: Exception) {
                        }
                    },
                    onUpdateEqBand = { bandIdx, gainDb ->
                        try {
                            val eq = audioEqualizer
                            if (eq != null && bandIdx < eq.numberOfBands) {
                                val minMb = eq.bandLevelRange[0]
                                val maxMb = eq.bandLevelRange[1]
                                val targetMb = (gainDb * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                                eq.setBandLevel(bandIdx.toShort(), targetMb)
                            }
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            audioEqualizer?.release()
        } catch (_: Exception) {
        }
        exoPlayer?.release()
        super.onDestroy()
    }
}

@Composable
fun AetherWaveNativeApp(
    exoPlayer: ExoPlayer,
    onAttachEqualizer: (Int) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aetherwave_native_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableIntStateOf(0) } // 0: Peta & Jelajah, 1: Negara, 2: Studio EQ, 3: Favorit
    var selectedCountryCode by remember { mutableStateOf("ID") }
    var stationList by remember { mutableStateOf(BUILTIN_STATIONS.filter { it.countryCode == "ID" }) }
    var isLoadingCountry by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var activeStation by remember { mutableStateOf(BUILTIN_STATIONS.first()) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var fallbackTried by remember { mutableStateOf(false) }

    var dataSaverMode by remember { mutableStateOf(false) }
    var sleepTimerSeconds by remember { mutableStateOf<Int?>(null) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0: Spektrum, 1: Gelombang, 2: Orbital
    var eqGains by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var selectedPresetName by remember { mutableStateOf("Natural Flat") }

    var favoriteIds by remember {
        val saved = prefs.getStringSet("favorites", setOf("id-prambors", "jp-shibuya", "fr-fip")) ?: emptySet()
        mutableStateOf(saved.toSet())
    }

    // Dynamic Glassmorphism Smooth Color Transitions
    val animatedPrimary by animateColorAsState(
        targetValue = activeStation.primaryColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "dynamicPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = activeStation.secondaryColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "dynamicSecondary"
    )

    // ExoPlayer Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    onAttachEqualizer(exoPlayer.audioSessionId)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) isBuffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!fallbackTried) {
                    fallbackTried = true
                    exoPlayer.setMediaItem(MediaItem.fromUri(activeStation.fallbackUrl))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    isBuffering = false
                    isPlaying = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    fun playStation(station: NativeStation) {
        fallbackTried = false
        activeStation = station
        isBuffering = true
        val targetUrl = if (dataSaverMode && station.fallbackUrl.contains("64")) {
            station.fallbackUrl
        } else {
            station.streamUrl
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(targetUrl))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun toggleFavorite(id: String) {
        val updated = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
        favoriteIds = updated
        prefs.edit().putStringSet("favorites", updated).apply()
    }

    // Load stations when country changes
    LaunchedEffect(selectedCountryCode) {
        isLoadingCountry = true
        stationList = fetchRemoteStationsByCountry(selectedCountryCode)
        isLoadingCountry = false
    }

    // Sleep Timer Countdown Coroutine
    LaunchedEffect(sleepTimerSeconds) {
        val current = sleepTimerSeconds ?: return@LaunchedEffect
        if (current <= 0) {
            exoPlayer.pause()
            isPlaying = false
            sleepTimerSeconds = null
        } else {
            delay(1000L)
            sleepTimerSeconds = current - 1
        }
    }

    // Local Clock in Selected Country Timezone
    var countryClock by remember { mutableStateOf("") }
    LaunchedEffect(selectedCountryCode) {
        val node = GLOBAL_COUNTRIES.find { it.code == selectedCountryCode } ?: GLOBAL_COUNTRIES.first()
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone(node.timezone)
            countryClock = sdf.format(Date())
            delay(1000L)
        }
    }

    val filteredStations = remember(stationList, searchQuery, dataSaverMode) {
        stationList.filter { st ->
            val matchesSearch = searchQuery.isBlank() ||
                st.name.contains(searchQuery, ignoreCase = true) ||
                st.tags.contains(searchQuery, ignoreCase = true) ||
                st.city.contains(searchQuery, ignoreCase = true)
            matchesSearch
        }
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF090D16).copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f)
                )
            ) {
                val items = listOf(
                    Triple(0, "Peta Dunia", Icons.Default.Public),
                    Triple(1, "Negara", Icons.Default.Explore),
                    Triple(2, "Studio EQ", Icons.Default.Tune),
                    Triple(3, "Favorit (${favoriteIds.size})", Icons.Default.Favorite)
                )
                items.forEach { (idx, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == idx,
                        onClick = { currentTab = idx },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (currentTab == idx) animatedPrimary else Color(0xFF94A3B8)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == idx) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == idx) Color.White else Color(0xFF94A3B8)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = animatedPrimary.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .padding(innerPadding)
        ) {
            // Dynamic Ambient Glass Aurora Background Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedPrimary.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.14f),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                    radius = size.width * 0.75f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedSecondary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.55f),
                        radius = size.width * 0.70f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.55f),
                    radius = size.width * 0.70f
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top App Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AetherWave",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Native Jetpack Compose · Media3 ExoPlayer",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Button(
                            onClick = {
                                val randomPick = BUILTIN_STATIONS.random()
                                selectedCountryCode = randomPick.countryCode
                                playStation(randomPick)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = animatedPrimary,
                                contentColor = Color(0xFF090D16)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Acak",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Acak Dunia",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ACTIVE DYNAMIC GLASS PLAYER DECK
                item {
                    GlassCard(
                        primaryColor = animatedPrimary,
                        secondaryColor = animatedSecondary,
                        highlightBorder = true
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Station Emblem
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    animatedPrimary.copy(alpha = 0.55f),
                                                    animatedSecondary.copy(alpha = 0.35f)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.28f),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = activeStation.countryCode,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = activeStation.frequency,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${activeStation.country} · ${activeStation.frequency} · ${if (dataSaverMode) 64 else activeStation.bitrate} kbps",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeStation.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeStation.tags,
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { toggleFavorite(activeStation.id) }
                            ) {
                                val isFav = favoriteIds.contains(activeStation.id)
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorit",
                                    tint = if (isFav) Color(0xFFF43F5E) else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 60FPS NATIVE COMPOSE AUDIO VISUALIZER CANVAS
                        NativeSpectrumVisualizer(
                            isPlaying = isPlaying,
                            mode = visualizerMode,
                            primaryColor = animatedPrimary,
                            secondaryColor = animatedSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { visualizerMode = (visualizerMode + 1) % 3 },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Ganti Mode Visualizer",
                                    tint = animatedPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val pool = filteredStations.ifEmpty { BUILTIN_STATIONS }
                                    val idx = pool.indexOfFirst { it.id == activeStation.id }
                                    val prev = if (idx <= 0) pool.last() else pool[idx - 1]
                                    playStation(prev)
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Sebelumnya",
                                    tint = Color.White
                                )
                            }

                            // Play / Pause Primary Button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(animatedPrimary, animatedSecondary)
                                        )
                                    )
                                    .clickable {
                                        if (isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            if (exoPlayer.currentMediaItem == null) {
                                                playStation(activeStation)
                                            } else {
                                                exoPlayer.play()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        color = Color(0xFF090D16),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Putar / Jeda",
                                        tint = Color(0xFF090D16),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val pool = filteredStations.ifEmpty { BUILTIN_STATIONS }
                                    val idx = pool.indexOfFirst { it.id == activeStation.id }
                                    val next = if (idx < 0 || idx >= pool.lastIndex) pool.first() else pool[idx + 1]
                                    playStation(next)
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Berikutnya",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { currentTab = 2 },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Equalizer",
                                    tint = if (sleepTimerSeconds != null) Color(0xFF34D399) else Color.White
                                )
                            }
                        }
                    }
                }

                // TAB 0: INTERACTIVE WORLD MAP CANVAS & COUNTRY EXPLORER
                if (currentTab == 0) {
                    item {
                        val activeCountryNode = GLOBAL_COUNTRIES.find { it.code == selectedCountryCode } ?: GLOBAL_COUNTRIES.first()
                        GlassCard(primaryColor = animatedPrimary, secondaryColor = animatedSecondary) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Peta Frekuensi Dunia Interaktif",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${activeCountryNode.flag} ${activeCountryNode.name} · Jam Lokal $countryClock",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = "${stationList.size} Stasiun",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive World Map Canvas (Tap any country pin!)
                            InteractiveWorldMapCanvas(
                                selectedCode = selectedCountryCode,
                                accentColor = animatedPrimary,
                                onSelectCountry = { code -> selectedCountryCode = code }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(GLOBAL_COUNTRIES) { node ->
                                    val selected = node.code == selectedCountryCode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) animatedPrimary.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) animatedPrimary else Color.White.copy(alpha = 0.10f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCountryCode = node.code }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${node.flag} ${node.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: FULL COUNTRY DIRECTORY GRID
                if (currentTab == 1) {
                    item {
                        GlassCard(primaryColor = animatedPrimary, secondaryColor = animatedSecondary) {
                            Text(
                                text = "Direktori Negara Dunia",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GLOBAL_COUNTRIES.forEach { node ->
                                    val selected = node.code == selectedCountryCode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (selected) animatedPrimary.copy(alpha = 0.20f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) animatedPrimary else Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedCountryCode = node.code
                                                currentTab = 0
                                            }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = node.flag, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = node.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Zona Waktu: ${node.timezone}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                        Text(
                                            text = node.code,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = animatedPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: HARDWARE 5-BAND EQUALIZER, SLEEP TIMER & DATA SAVER STUDIO
                if (currentTab == 2) {
                    item {
                        GlassCard(primaryColor = animatedPrimary, secondaryColor = animatedSecondary) {
                            Text(
                                text = "Hardware Equalizer 5-Band & Studio Audio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Terhubung langsung ke DSP Android AudioFX · Preset: $selectedPresetName",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // EQ Presets
                            val presets = listOf(
                                "Natural Flat" to listOf(0f, 0f, 0f, 0f, 0f),
                                "Deep Bass" to listOf(7f, 5f, 0f, 1f, 2f),
                                "Vocal Clarity" to listOf(-2f, 1f, 5f, 4f, 1f),
                                "Warm Jazz" to listOf(4f, 2f, -1f, 3f, 5f),
                                "Neon Club" to listOf(6f, 3f, -2f, 3f, 6f)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(presets) { (name, values) ->
                                    val active = selectedPresetName == name
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (active) animatedPrimary.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                1.dp,
                                                if (active) animatedPrimary else Color.White.copy(alpha = 0.10f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedPresetName = name
                                                eqGains = values
                                                values.forEachIndexed { idx, g -> onUpdateEqBand(idx, g) }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(text = name, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                            bandLabels.forEachIndexed { idx, label ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.width(52.dp)
                                    )
                                    Slider(
                                        value = eqGains[idx],
                                        onValueChange = { newVal ->
                                            selectedPresetName = "Custom"
                                            val mutable = eqGains.toMutableList()
                                            mutable[idx] = newVal
                                            eqGains = mutable
                                            onUpdateEqBand(idx, newVal)
                                        },
                                        valueRange = -12f..12f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = animatedPrimary,
                                            activeTrackColor = animatedPrimary
                                        )
                                    )
                                    Text(
                                        text = "${eqGains[idx].toInt()} dB",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        modifier = Modifier.width(44.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )

                            // Sleep Timer Controls
                            Text(
                                text = "Sleep Timer (Matikan Otomatis): ${sleepTimerSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "Nonaktif"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    OutlinedButton(
                                        onClick = { sleepTimerSeconds = mins * 60 },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text(text = "${mins}m", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { sleepTimerSeconds = null },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text(text = "Off", fontSize = 11.sp, color = Color(0xFFF43F5E))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Data Saver Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mode Hemat Data Seluler (≤ 64 kbps)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Hemat kuota hingga 70% (~28 MB/jam)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Switch(
                                    checked = dataSaverMode,
                                    onCheckedChange = { dataSaverMode = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF090D16),
                                        checkedTrackColor = Color(0xFF34D399)
                                    )
                                )
                            }
                        }
                    }
                }

                // STATION LIST (All tabs show relevant stations below)
                item {
                    val displayedList = if (currentTab == 3) {
                        (BUILTIN_STATIONS + stationList)
                            .distinctBy { it.id }
                            .filter { favoriteIds.contains(it.id) }
                    } else {
                        filteredStations
                    }

                    GlassCard(primaryColor = animatedPrimary, secondaryColor = animatedSecondary) {
                        Text(
                            text = if (currentTab == 3) "Stasiun Favorit Tersimpan (${displayedList.size})"
                            else "Daftar Stasiun Radio Aktif (${displayedList.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Cari nama stasiun, kota, atau genre...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingCountry) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = animatedPrimary)
                            }
                        } else if (displayedList.isEmpty()) {
                            Text(
                                text = "Belum ada stasiun yang cocok.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                displayedList.forEach { st ->
                                    val isCurrent = st.id == activeStation.id
                                    val isFav = favoriteIds.contains(st.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isCurrent) Color.White.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrent) animatedPrimary.copy(alpha = 0.6f)
                                                else Color.White.copy(alpha = 0.07f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { playStation(st) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(st.primaryColor.copy(alpha = 0.28f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = st.countryCode,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = st.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) animatedPrimary else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${st.country} · ${st.city} · ${st.tags} · ${st.bitrate} kbps",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(onClick = { toggleFavorite(st.id) }) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorit",
                                                tint = if (isFav) Color(0xFFF43F5E) else Color(0xFF94A3B8)
                                            )
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
}

@Composable
fun GlassCard(
    primaryColor: Color,
    secondaryColor: Color,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = if (highlightBorder) {
                        listOf(
                            primaryColor.copy(alpha = 0.18f),
                            secondaryColor.copy(alpha = 0.10f),
                            Color(0xFF0F172A).copy(alpha = 0.85f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (highlightBorder) {
                        listOf(primaryColor.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f))
                    }
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun NativeSpectrumVisualizer(
    isPlaying: Boolean,
    mode: Int,
    primaryColor: Color,
    secondaryColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrumPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 1100 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnim"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D16).copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val bins = 32
        if (mode == 0) {
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (bins - 1)) / bins
            for (i in 0 until bins) {
                val amp = if (isPlaying) {
                    (0.38f + sin(phase * 2f + i * 0.45f) * 0.28f + cos(phase * 3f - i * 0.6f) * 0.22f)
                        .coerceIn(0.10f, 0.96f)
                } else {
                    0.10f + sin(phase + i * 0.3f) * 0.03f
                }
                val barHeight = amp * size.height
                val x = i * (barWidth + gap)
                val y = (size.height - barHeight) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(primaryColor, secondaryColor)),
                    topLeft = Offset(x, y),
                    size = Size(barWidth.coerceAtLeast(2f), barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        } else if (mode == 1) {
            val path = Path()
            val stepX = size.width / (bins - 1)
            for (i in 0 until bins) {
                val amp = if (isPlaying) {
                    sin(phase * 2f + i * 0.35f) * (size.height * 0.36f)
                } else {
                    sin(phase + i * 0.25f) * (size.height * 0.06f)
                }
                val x = i * stepX
                val y = size.height / 2f + amp
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.height * 0.24f
            drawCircle(
                brush = Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.45f), Color.Transparent)),
                center = Offset(cx, cy),
                radius = baseR * 1.8f
            )
            for (i in 0 until 24) {
                val angle = (i.toFloat() / 24f) * 6.283185f + phase * 0.5f
                val extra = if (isPlaying) (0.2f + sin(phase * 2f + i) * 0.18f) * size.height * 0.4f else 4f
                val x1 = cx + cos(angle) * baseR
                val y1 = cy + sin(angle) * baseR
                val x2 = cx + cos(angle) * (baseR + extra)
                val y2 = cy + sin(angle) * (baseR + extra)
                drawLine(
                    color = if (i % 2 == 0) primaryColor else secondaryColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun InteractiveWorldMapCanvas(
    selectedCode: String,
    accentColor: Color,
    onSelectCountry: (String) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF060911))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val normX = tapOffset.x / size.width
                    val normY = tapOffset.y / size.height
                    val nearest = GLOBAL_COUNTRIES.minByOrNull { c ->
                        val dx = c.mapX - normX
                        val dy = c.mapY - normY
                        dx * dx + dy * dy
                    }
                    if (nearest != null) {
                        onSelectCountry(nearest.code)
                    }
                }
            }
    ) {
        // Subtle Latitude/Longitude Grid
        val cols = 8
        val rows = 4
        for (c in 1 until cols) {
            val x = (size.width / cols) * c
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        for (r in 1 until rows) {
            val y = (size.height / rows) * r
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Country Pins
        GLOBAL_COUNTRIES.forEach { node ->
            val px = node.mapX * size.width
            val py = node.mapY * size.height
            val isSelected = node.code == selectedCode
            if (isSelected) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.28f),
                    radius = 16.dp.toPx(),
                    center = Offset(px, py)
                )
            }
            drawCircle(
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.75f),
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}
