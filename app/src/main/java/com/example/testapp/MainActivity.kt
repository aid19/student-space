package com.example.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testapp.ui.theme.TestAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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

@Composable
fun AppScreen() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        GroupsScreen()
    } else {
        AuthScreen(onLoginSuccess = { isLoggedIn = true })
    }
}

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

    Column(modifier = Modifier.padding(16.dp)) {

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    message = if (it.isSuccessful) {
                        "✅ Registered!"
                    } else {
                        "❌ ${it.exception?.message}"
                    }
                }
        }) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        message = "✅ Logged in!"
                        onLoginSuccess()
                    } else {
                        message = "❌ ${it.exception?.message}"
                    }
                }
        }) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)
    }
}

@Composable
fun GroupsScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var groupName by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(listOf<String>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                "📚 Study Groups",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 Input
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 Create button
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
                        "members" to listOf(userId)
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
                                snackbarHostState.showSnackbar("❌ Error creating group")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ Create group")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔥 Load button
            OutlinedButton(
                onClick = {
                    db.collection("groups")
                        .get()
                        .addOnSuccessListener { result ->
                            groups = result.map { it.getString("name") ?: "" }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Failed to load groups")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📥 Load groups")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 Groups list
            groups.forEach { group ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(group)

                        Button(
                            onClick = {
                                val userId = auth.currentUser?.uid

                                if (userId == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("❌ Not logged in")
                                    }
                                    return@Button
                                }

                                db.collection("groups")
                                    .whereEqualTo("name", group)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        result.documents.forEach { doc ->
                                            doc.reference.update(
                                                "members",
                                                com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                                            )
                                        }

                                        scope.launch {
                                            snackbarHostState.showSnackbar("✅ Joined $group!")
                                        }
                                    }
                                    .addOnFailureListener {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("❌ Failed to join")
                                        }
                                    }
                            }
                        ) {
                            Text("Join")
                        }
                    }
                }
            }
        }
    }
}