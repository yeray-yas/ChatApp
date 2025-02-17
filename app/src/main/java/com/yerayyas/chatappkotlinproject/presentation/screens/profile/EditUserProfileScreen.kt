package com.yerayyas.chatappkotlinproject.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserProfileScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    // Estados del ViewModel
    val names by userProfileViewModel.names.collectAsState()
    val lastNames by userProfileViewModel.lastNames.collectAsState()
    val profession by userProfileViewModel.profession.collectAsState()
    val address by userProfileViewModel.address.collectAsState()
    val age by userProfileViewModel.age.collectAsState()
    val phone by userProfileViewModel.phone.collectAsState()

    // Estados locales MUTABLES para edición
    var editedNames by rememberSaveable { mutableStateOf(names) }
    var editedLastNames by rememberSaveable { mutableStateOf(lastNames) }
    var editedProfession by rememberSaveable { mutableStateOf(profession) }
    var editedAddress by rememberSaveable { mutableStateOf(address) }
    var editedAge by rememberSaveable { mutableStateOf(age) }
    var editedPhone by rememberSaveable { mutableStateOf(phone) }

    // Sincroniza los estados locales cuando el ViewModel se actualiza
    LaunchedEffect(names, lastNames, profession, address, age, phone) {
        editedNames = names
        editedLastNames = lastNames
        editedProfession = profession
        editedAddress = address
        editedAge = age
        editedPhone = phone
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Edit Personal Information") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = editedNames,
                onValueChange = { editedNames = it },
                label = { Text(text = "First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editedLastNames,
                onValueChange = { editedLastNames = it },
                label = { Text(text = "Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editedProfession,
                onValueChange = { editedProfession = it },
                label = { Text(text = "Profession") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editedAddress,
                onValueChange = { editedAddress = it },
                label = { Text(text = "Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editedAge,
                onValueChange = { editedAge = it },
                label = { Text(text = "Age") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editedPhone,
                onValueChange = { editedPhone = it },
                label = { Text(text = "Phone") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Actualizamos la información en Firebase (o en el ViewModel)
                    userProfileViewModel.updatePersonalInformation(
                        names = editedNames,
                        lastNames = editedLastNames,
                        profession = editedProfession,
                        address = editedAddress,
                        age = editedAge,
                        phone = editedPhone
                    )
                    // Una vez guardado, volvemos a la pantalla anterior
                    navController.popBackStack()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Save")
            }
        }
    }
}
