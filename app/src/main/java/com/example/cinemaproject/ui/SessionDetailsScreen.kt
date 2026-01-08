package com.example.cinemaproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinemaproject.data.TokenStorage
import com.example.cinemaproject.network.ApiProvider
import com.example.cinemaproject.network.HallPlan
import com.example.cinemaproject.network.Ticket
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

class SessionDetailsViewModel(
    private val tokenStorage: TokenStorage,
    private val api: Api
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SessionDetailsUiState>(SessionDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private val _selectedSeats = MutableStateFlow<Set<String>>(emptySet())
    val selectedSeats = _selectedSeats.asStateFlow()
    
    fun loadSessionData(sessionId: String, hallId: String) {
        viewModelScope.launch {
            _uiState.value = SessionDetailsUiState.Loading
            try {
                val token = tokenStorage.getAccessToken()
                val authHeader = token?.let { "Bearer $it" } ?: ""
                
                val hallPlan = api.getHallPlan(authorization = authHeader, hallId = hallId)
                val tickets = api.getSessionTickets(
                    authorization = authHeader,
                    sessionId = sessionId,
                    status = null
                )
                
                _uiState.value = SessionDetailsUiState.Success(
                    hallPlan = hallPlan,
                    tickets = tickets
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error("Не удалось загрузить данные")
            }
        }
    }
    
    fun toggleSeatSelection(seatId: String, isAvailable: Boolean) {
        if (!isAvailable) return
        
        val current = _selectedSeats.value
        _selectedSeats.value = if (current.contains(seatId)) {
            current - seatId
        } else {
            current + seatId
        }
    }
    
    fun clearSelection() {
        _selectedSeats.value = emptySet()
    }
    
    fun removeSelectedSeat(seatId: String) {
        _selectedSeats.value = _selectedSeats.value - seatId
    }
    
    suspend fun processOrder(): Boolean {
        // Здесь логика оформления заказа
        delay(3000)
        return true
    }
}

sealed class SessionDetailsUiState {
    object Loading : SessionDetailsUiState()
    data class Success(val hallPlan: HallPlan, val tickets: List<Ticket>) : SessionDetailsUiState()
    data class Error(val message: String) : SessionDetailsUiState()
}

@Composable
fun SessionDetailsScreen(
    tokenStorage: TokenStorage,
    sessionId: String,
    hallId: String,
    onBack: () -> Unit = {},
) {
    val viewModel: SessionDetailsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = ApiProvider.getApi(LocalContext.current)
                return SessionDetailsViewModel(tokenStorage, api) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSeats by viewModel.selectedSeats.collectAsStateWithLifecycle()
    
    LaunchedEffect(sessionId, hallId) {
        viewModel.loadSessionData(sessionId, hallId)
    }
    
    when (val state = uiState) {
        is SessionDetailsUiState.Loading -> LoadingScreen()
        is SessionDetailsUiState.Error -> ErrorScreen(message = state.message, onBack = onBack)
        is SessionDetailsUiState.Success -> SuccessScreen(
            hallPlan = state.hallPlan,
            tickets = state.tickets,
            selectedSeats = selectedSeats,
            onToggleSeat = { seatId, isAvailable ->
                viewModel.toggleSeatSelection(seatId, isAvailable)
            },
            onRemoveSeat = viewModel::removeSelectedSeat,
            onProcessOrder = { 
                viewModelScope.launch {
                    val success = viewModel.processOrder()
                    if (success) {
                        // Обработка успешного оформления
                    }
                }
            },
            onBack = onBack
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
private fun SuccessScreen(
    hallPlan: HallPlan,
    tickets: List<Ticket>,
    selectedSeats: Set<String>,
    onToggleSeat: (String, Boolean) -> Unit,
    onRemoveSeat: (String) -> Unit,
    onProcessOrder: () -> Unit,
    onBack: () -> Unit,
) {
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isProcessingOrder by remember { mutableStateOf(false) }
    
    if (showSuccessDialog) {
        SuccessDialog(onDismiss = onBack)
        return
    }
    
    if (isProcessingOrder) {
        ProcessingOrderScreen()
        return
    }
    
    val seatIdToStatus = remember(tickets) {
        tickets.associateBy { it.seatId }.mapValues { it.value.status }
    }
    
    val seatIdToSeat = remember(hallPlan) {
        hallPlan.seats.associateBy { it.id }
    }
    
    val categoryIdToPrice = remember(hallPlan) {
        hallPlan.categories.associate { it.id to it.priceCents }
    }
    
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader()
        
        SeatsGrid(
            hallPlan = hallPlan,
            seatIdToStatus = seatIdToStatus,
            selectedSeats = selectedSeats,
            onSeatClicked = onToggleSeat
        )
        
        Legend()
        
        if (selectedSeats.isNotEmpty()) {
            SelectedSeatsList(
                selectedSeats = selectedSeats,
                seatIdToSeat = seatIdToSeat,
                categoryIdToPrice = categoryIdToPrice,
                onRemoveSeat = onRemoveSeat
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OrderButton(
            isEnabled = selectedSeats.isNotEmpty(),
            onClick = {
                isProcessingOrder = true
                onProcessOrder()
                LaunchedEffect(Unit) {
                    delay(3000) // Имитация обработки
                    isProcessingOrder = false
                    showSuccessDialog = true
                }
            }
        )
    }
}

@Composable
private fun ScreenHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color(0xFF3A2C5C)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ЭКРАН",
            color = Color(0xFFBCA7FF),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SeatsGrid(
    hallPlan: HallPlan,
    seatIdToStatus: Map<String, String>,
    selectedSeats: Set<String>,
    onSeatClicked: (String, Boolean) -> Unit
) {
    val seatsByRow = remember(hallPlan) {
        hallPlan.seats
            .groupBy { it.row }
            .mapValues { it.value.sortedBy { seat -> seat.number } }
            .toSortedMap()
    }
    
    LazyColumn(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(seatsByRow.entries.toList()) { (rowNum, rowSeats) ->
            SeatRow(
                rowNumber = rowNum,
                seats = rowSeats,
                seatIdToStatus = seatIdToStatus,
                selectedSeats = selectedSeats,
                onSeatClicked = onSeatClicked
            )
        }
    }
}

@Composable
private fun SeatRow(
    rowNumber: String,
    seats: List<Seat>,
    seatIdToStatus: Map<String, String>,
    selectedSeats: Set<String>,
    onSeatClicked: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowNumberText(text = rowNumber)
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            seats.forEachIndexed { index, seat ->
                SeatItem(
                    seat = seat,
                    index = index,
                    totalSeats = seats.size,
                    seatIdToStatus = seatIdToStatus,
                    isSelected = selectedSeats.contains(seat.id),
                    onClick = onSeatClicked
                )
            }
        }
        
        RowNumberText(text = rowNumber, startPadding = true)
    }
}

@Composable
private fun RowNumberText(
    text: String,
    startPadding: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(
                start = if (startPadding) 8.dp else 0.dp,
                end = if (!startPadding) 8.dp else 0.dp
            )
            .size(width = 24.dp, height = 24.dp),
        color = Color(0xFFBCA7FF),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SeatItem(
    seat: Seat,
    index: Int,
    totalSeats: Int,
    seatIdToStatus: Map<String, String>,
    isSelected: Boolean,
    onClick: (String, Boolean) -> Unit
) {
    val status = seatIdToStatus[seat.id] ?: "AVAILABLE"
    val isAvailable = status !in listOf("SOLD", "RESERVED", "CANCELLED")
    
    val seatColor = when {
        !isAvailable -> Color(0xFF6B5C8E)
        isSelected -> Color(0xFF2ECC71)
        else -> Color(0xFF8B6BE8)
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index == totalSeats / 2) {
            Spacer(modifier = Modifier.size(20.dp))
        }
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    seatColor,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    Color(0xFF2C2344),
                    shape = RoundedCornerShape(6.dp)
                )
                .let { modifier ->
                    if (isAvailable) {
                        modifier.clickable { onClick(seat.id, isAvailable) }
                    } else {
                        modifier
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = seat.number.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = Color(0xFF8B6BE8), text = "Свободно")
        Spacer(modifier = Modifier.size(8.dp))
        LegendItem(color = Color(0xFF6B5C8E), text = "Занято")
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color)
        )
        Text(text = text, color = Color(0xFFBCA7FF))
    }
}

@Composable
private fun SelectedSeatsList(
    selectedSeats: Set<String>,
    seatIdToSeat: Map<String, Seat>,
    categoryIdToPrice: Map<String, Int>,
    onRemoveSeat: (String) -> Unit
) {
    Spacer(modifier = Modifier.size(8.dp))
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(selectedSeats.toList()) { seatId ->
            val seat = seatIdToSeat[seatId] ?: return@items
            val priceRub = ((categoryIdToPrice[seat.categoryId] ?: 0) / 100)
            
            SelectedSeatCard(
                seat = seat,
                priceRub = priceRub,
                onRemove = { onRemoveSeat(seatId) }
            )
        }
    }
}

@Composable
private fun SelectedSeatCard(
    seat: Seat,
    priceRub: Int,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1F58))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(min = 220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        Color(0xFF2ECC71),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Ряд ${seat.row}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Место ${seat.number}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$priceRub ₽",
                    color = Color(0xFFBCA7FF),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Text(
                text = "✕",
                color = Color(0xFFBCA7FF),
                modifier = Modifier.clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun OrderButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        enabled = isEnabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Оформить заказ")
    }
}

@Composable
private fun ProcessingOrderScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Обработка заказа...",
            color = Color(0xFFBCA7FF),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SuccessDialog(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Заказ успешно оплачен - билеты будут отправлены вам на почту",
            color = Color.Black,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Button(onClick = onDismiss) {
            Text("ОК")
        }
    }
}