package com.example.desafio20clientes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.desafio20clientes.ui.theme.ClienteAppTheme
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

// ================== ENTIDADE ROOM ==================
@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val email: String,
    val identificacao: String
)

// ================== DAO ROOM ==================
@Dao
interface ClienteDao {
    @androidx.room.Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliente(cliente: Cliente)

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Delete
    suspend fun deleteCliente(cliente: Cliente)

    @androidx.room.Query("DELETE FROM clientes")
    suspend fun deleteAllClientes()
}

// ================== DATABASE ROOM ==================
@Database(entities = [Cliente::class], version = 1, exportSchema = false)
abstract class ClienteDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao

    companion object {
        @Volatile
        private var INSTANCE: ClienteDatabase? = null

        fun getDatabase(context: android.content.Context): ClienteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClienteDatabase::class.java,
                    "cliente_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ================== MODELO API ==================
data class ClienteApi(
    val id: Int,
    val name: String,
    val email: String,
    val username: String
)

// ================== INTERFACE RETROFIT ==================
interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<ClienteApi>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): ClienteApi

    @POST("users")
    suspend fun createUser(@Body user: ClienteApi): ClienteApi

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: ClienteApi): ClienteApi

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int)
}

// ================== REPOSITÓRIO ==================
class ClienteRepository(
    private val clienteDao: ClienteDao,
    private val apiService: ApiService
) {
    val allClientes: Flow<List<Cliente>> = clienteDao.getAllClientes()

    suspend fun insert(cliente: Cliente) {
        clienteDao.insertCliente(cliente)
    }

    suspend fun update(cliente: Cliente) {
        clienteDao.updateCliente(cliente)
    }

    suspend fun delete(cliente: Cliente) {
        clienteDao.deleteCliente(cliente)
    }

    suspend fun syncWithApi() {
        try {
            val clientesFromApi = apiService.getUsers()
            clientesFromApi.forEach { clienteApi ->
                val cliente = Cliente(
                    id = clienteApi.id,
                    nome = clienteApi.name,
                    email = clienteApi.email,
                    identificacao = clienteApi.username
                )
                clienteDao.insertCliente(cliente)
            }
        } catch (e: Exception) {
            // Handle error - em produção, você deveria tratar melhor
            e.printStackTrace()
        }
    }

    suspend fun loadSampleData() {
        val sampleClientes = listOf(
            Cliente(nome = "João Silva", email = "joao@email.com", identificacao = "001"),
            Cliente(nome = "Maria Santos", email = "maria@email.com", identificacao = "002"),
            Cliente(nome = "Pedro Oliveira", email = "pedro@email.com", identificacao = "003"),
            Cliente(nome = "Ana Costa", email = "ana@email.com", identificacao = "004"),
            Cliente(nome = "Carlos Souza", email = "carlos@email.com", identificacao = "005"),
            Cliente(nome = "Lucia Ferreira", email = "lucia@email.com", identificacao = "006"),
            Cliente(nome = "Roberto Lima", email = "roberto@email.com", identificacao = "007"),
            Cliente(nome = "Patricia Alves", email = "patricia@email.com", identificacao = "008"),
            Cliente(nome = "Fernando Rocha", email = "fernando@email.com", identificacao = "009"),
            Cliente(nome = "Juliana Martins", email = "juliana@email.com", identificacao = "010"),
            Cliente(nome = "Ricardo Pereira", email = "ricardo@email.com", identificacao = "011"),
            Cliente(nome = "Sandra Gomes", email = "sandra@email.com", identificacao = "012"),
            Cliente(nome = "Paulo Ribeiro", email = "paulo@email.com", identificacao = "013"),
            Cliente(nome = "Camila Dias", email = "camila@email.com", identificacao = "014"),
            Cliente(nome = "Marcos Barbosa", email = "marcos@email.com", identificacao = "015"),
            Cliente(nome = "Beatriz Castro", email = "beatriz@email.com", identificacao = "016"),
            Cliente(nome = "Gustavo Mendes", email = "gustavo@email.com", identificacao = "017"),
            Cliente(nome = "Larissa Cardoso", email = "larissa@email.com", identificacao = "018"),
            Cliente(nome = "Diego Nascimento", email = "diego@email.com", identificacao = "019"),
            Cliente(nome = "Vanessa Araújo", email = "vanessa@email.com", identificacao = "020")
        )

        sampleClientes.forEach { cliente ->
            clienteDao.insertCliente(cliente)
        }
    }
}

// ================== VIEWMODEL ==================
class ClienteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ClienteDatabase.getDatabase(application)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)
    private val repository = ClienteRepository(database.clienteDao(), apiService)

    val allClientes = repository.allClientes

    fun insertCliente(cliente: Cliente) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(cliente)
        }
    }

    fun updateCliente(cliente: Cliente) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(cliente)
        }
    }

    fun deleteCliente(cliente: Cliente) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(cliente)
        }
    }

    fun syncWithApi() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncWithApi()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadSampleData()
        }
    }
}

// ================== TELA PRINCIPAL ==================
class MainActivity : ComponentActivity() {
    private val viewModel: ClienteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClienteAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClienteScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteScreen(viewModel: ClienteViewModel) {
    val clientes by viewModel.allClientes.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editingCliente by remember { mutableStateOf<Cliente?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro de Clientes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Botão para carregar dados de exemplo
                    TextButton(
                        onClick = { viewModel.loadSampleData() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Carregar 20 Clientes")
                    }

                    // Botão para sincronizar com API
                    TextButton(
                        onClick = { viewModel.syncWithApi() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Sync API")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cliente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contador de clientes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Total de Clientes: ${clientes.size}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Lista de clientes
            if (clientes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Nenhum cliente cadastrado",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSampleData() }) {
                            Text("Carregar Dados de Exemplo")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clientes) { cliente ->
                        ClienteCard(
                            cliente = cliente,
                            onEdit = {
                                editingCliente = cliente
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteCliente(cliente)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog para adicionar cliente
    if (showDialog) {
        AddClienteDialog(
            onDismiss = { showDialog = false },
            onConfirm = { nome, email, identificacao ->
                viewModel.insertCliente(
                    Cliente(
                        nome = nome,
                        email = email,
                        identificacao = identificacao
                    )
                )
                showDialog = false
            }
        )
    }

    // Dialog para editar cliente
    if (showEditDialog && editingCliente != null) {
        EditClienteDialog(
            cliente = editingCliente!!,
            onDismiss = {
                showEditDialog = false
                editingCliente = null
            },
            onConfirm = { nome, email, identificacao ->
                viewModel.updateCliente(
                    editingCliente!!.copy(
                        nome = nome,
                        email = email,
                        identificacao = identificacao
                    )
                )
                showEditDialog = false
                editingCliente = null
            }
        )
    }
}

@Composable
fun ClienteCard(
    cliente: Cliente,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cliente.nome,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = cliente.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ID: ${cliente.identificacao}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddClienteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identificacao by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Cliente") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = identificacao,
                    onValueChange = { identificacao = it },
                    label = { Text("Identificação") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nome.isNotBlank() && email.isNotBlank() && identificacao.isNotBlank()) {
                        onConfirm(nome, email, identificacao)
                    }
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditClienteDialog(
    cliente: Cliente,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf(cliente.nome) }
    var email by remember { mutableStateOf(cliente.email) }
    var identificacao by remember { mutableStateOf(cliente.identificacao) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Cliente") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = identificacao,
                    onValueChange = { identificacao = it },
                    label = { Text("Identificação") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nome.isNotBlank() && email.isNotBlank() && identificacao.isNotBlank()) {
                        onConfirm(nome, email, identificacao)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}