package com.example.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testapp.ui.theme.TestAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)
        Log.d("FirebaseCheck", "Firebase works: ${FirebaseApp.getApps(this).isNotEmpty()}")

        setContent {
            TestAppTheme {
                AppScreen()
            }
        }
    }
}

// Realtime Database model for Lab 3
data class RoomStatus(
    var id: String = "",
    var name: String = "",
    var status: String = "",
    var currentUser: String = ""
)

@Composable
fun AppScreen() {
    var isLoggedIn by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    if (isLoggedIn) {
        GroupsScreen(onLogout = { isLoggedIn = false })
    } else {
        AuthScreen(onLoginSuccess = { isLoggedIn = true })
    }
}

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "StudentSPACE",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "❌ Please enter email and password"
                    return@Button
                }

                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        message = if (task.isSuccessful) {
                            "✅ Registered successfully. Now login."
                        } else {
                            "❌ ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "❌ Please enter email and password"
                    return@Button
                }

                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            message = "✅ Logged in!"
                            onLoginSuccess()
                        } else {
                            message = "❌ ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)
    }
}

@Composable
fun GroupsScreen(onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val realtimeDb = FirebaseDatabase.getInstance().getReference("rooms")

    var groupName by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var rooms by remember { mutableStateOf(listOf<RoomStatus>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Lab 3: Read rooms from Realtime Database live
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomList = mutableListOf<RoomStatus>()

                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.getValue(RoomStatus::class.java)

                    if (room != null) {
                        room.id = roomSnapshot.key ?: ""
                        roomList.add(room)
                    }
                }

                rooms = roomList

                for (room in roomList) {
                    Log.d("RealtimeDB", "Room: ${room.name}, Status: ${room.status}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Database error: ${error.message}")
            }
        }

        realtimeDb.addValueEventListener(listener)

        onDispose {
            realtimeDb.removeEventListener(listener)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "📚 Study Groups",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val userId = auth.currentUser?.uid

                    if (userId == null || groupName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("❌ Enter group name")
                        }
                        return@Button
                    }

                    val data = hashMapOf(
                        "name" to groupName,
                        "members" to listOf(userId),
                        "ownerId" to userId
                    )

                    db.collection("groups")
                        .add(data)
                        .addOnSuccessListener {
                            groupName = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("✅ Group created!")
                            }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Failed to create group")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ Create group")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val userId = auth.currentUser?.uid

                    if (userId == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("❌ User not logged in")
                        }
                        return@OutlinedButton
                    }

                    db.collection("groups")
                        .whereArrayContains("members", userId)
                        .get()
                        .addOnSuccessListener { result ->
                            groups = result.map {
                                Pair(it.id, it.getString("name") ?: "")
                            }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Failed to load groups")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📥 Load my groups")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚪 Logout")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "My Groups",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (groups.isEmpty()) {
                Text("No groups loaded yet.")
            }

            groups.forEach { (id, name) ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Button(
                                onClick = {
                                    val userId = auth.currentUser?.uid

                                    if (userId == null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("❌ User not logged in")
                                        }
                                        return@Button
                                    }

                                    db.collection("groups")
                                        .document(id)
                                        .update("members", FieldValue.arrayUnion(userId))

                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Joined $name")
                                    }
                                }
                            ) {
                                Text("Join")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    db.collection("groups")
                                        .document(id)
                                        .delete()
                                        .addOnSuccessListener {
                                            groups = groups.filterNot { it.first == id }
                                        }

                                    scope.launch {
                                        snackbarHostState.showSnackbar("🗑 Deleted $name")
                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "🏫 Live Room Status",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Lab 3 feature: Realtime Database live updates",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (rooms.isEmpty()) {
                Text("No rooms found in Realtime Database.")
            }

            rooms.forEach { room ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = room.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Status: ${room.status}")
                        Text("Current user: ${room.currentUser}")

                        Spacer(modifier = Modifier.height(12.dp))

                        Row {
                            Button(
                                onClick = {
                                    realtimeDb.child(room.id).child("status").setValue("available")
                                    realtimeDb.child(room.id).child("currentUser").setValue("None")
                                }
                            ) {
                                Text("Available")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val userEmail = auth.currentUser?.email ?: "Student"

                                    realtimeDb.child(room.id).child("status").setValue("occupied")
                                    realtimeDb.child(room.id).child("currentUser").setValue(userEmail)
                                }
                            ) {
                                Text("Occupied")
                            }
                        }
                    }
                }
            }
        }
    }
}