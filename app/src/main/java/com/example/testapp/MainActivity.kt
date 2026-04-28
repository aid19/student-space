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
    var groupName by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(listOf<String>()) }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("📚 Study Groups", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group name") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            val data = hashMapOf(
                "name" to groupName,
                "members" to listOf(userId)
            )

            db.collection("groups")
                .add(data)
                .addOnSuccessListener {
                    groupName = ""
                }
        }) {
            Text("Create group")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            db.collection("groups")
                .get()
                .addOnSuccessListener { result ->
                    groups = result.map { it.getString("name") ?: "" }
                }
        }) {
            Text("Load groups")
        }

        Spacer(modifier = Modifier.height(16.dp))

        groups.forEach { group ->
            Row {
                Text(group)

                Button(onClick = {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    db.collection("groups")
                        .whereEqualTo("name", group)
                        .get()
                        .addOnSuccessListener { result ->
                            result. documents.forEach { doc ->
                                doc.reference.update(
                                    "members",
                                    com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                                )
                            }
                        }
                }) {
                    Text("Join")
                }
            }
        }
    }
}