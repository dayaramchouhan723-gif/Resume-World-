package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SharedPrefsHelper

@Composable
fun SanamProfileScreen(
    sharedPrefsHelper: SharedPrefsHelper,
    onProfileSaved: () -> Unit
) {
    val existingProfile = remember { sharedPrefsHelper.getUserProfile() }

    var name by remember { mutableStateOf(existingProfile.name.ifBlank { "Dayaram Chouhan" }) }
    var experience by remember { mutableStateOf(existingProfile.experience.ifBlank { "Farm Worker 3 years" }) }
    var skills by remember { mutableStateOf(existingProfile.skills.ifBlank { "Tractor Operation, Harvesting, Equipment Repair, WHMIS" }) }
    var education by remember { mutableStateOf(existingProfile.education.ifBlank { "Agricultural Trade Diploma" }) }
    var location by remember { mutableStateOf(existingProfile.location.ifBlank { "Punjab, India (Overseas Applicant)" }) }
    var isSavedSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("sanam_profile_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = SanamPink, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text("Candidate Profile Settings", color = SanamPink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Profile fill karne se ATS score 45 se 85+ ho jayega!", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SanamPanel),
            border = BorderStroke(1.dp, SanamCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Candidate Information", color = SanamCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = Color.Gray) },
                    placeholder = { Text("e.g. Dayaram Chouhan", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("Experience", color = Color.Gray) },
                    placeholder = { Text("e.g. Farm Worker 3 years, Tractor driving", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("profile_experience_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = { Text("Skills (Comma separated)", color = Color.Gray) },
                    placeholder = { Text("e.g. Tractor Operation, Harvesting, WHMIS, Forklift", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("profile_skills_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Education / Certifications", color = Color.Gray) },
                    placeholder = { Text("e.g. Agricultural Trade Certificate, High School", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("profile_education_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location", color = Color.Gray) },
                    placeholder = { Text("e.g. Punjab, India (Overseas Applicant)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("profile_location_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        sharedPrefsHelper.saveUserProfile(
                            name = name,
                            experience = experience,
                            skills = skills,
                            education = education,
                            location = location
                        )
                        isSavedSuccess = true
                        onProfileSaved()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanamPink),
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Candidate Profile (Boost ATS Score)")
                }

                if (isSavedSuccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SanamGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SanamGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = SanamGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Profile Saved! ATS Score automatically recalculating to 85+...", color = SanamGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
