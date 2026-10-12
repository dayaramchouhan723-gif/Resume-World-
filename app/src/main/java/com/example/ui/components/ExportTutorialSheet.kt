package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalAtsColors

object ExportTutorialPrefs {
    private const val PREFS_NAME = "export_tutorial_prefs"
    private const val KEY_DONT_SHOW_AGAIN = "dont_show_export_tutorial"

    fun shouldShowOnFirstExport(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false)
    }

    fun setDontShowAgain(context: Context, dontShow: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DONT_SHOW_AGAIN, dontShow).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTutorialSheet(
    initialDontShowAgain: Boolean,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    onProceedAction: ((dontShowAgain: Boolean) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val atsColors = LocalAtsColors.current
    var dontShowChecked by remember { mutableStateOf(initialDontShowAgain) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(dontShowChecked) },
        sheetState = sheetState,
        containerColor = atsColors.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("export_tutorial_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.export_tutorial_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("export_tutorial_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.developer_name),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF2092)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TutorialStepCard(
                stepNumber = "Step 1",
                title = "Copy as Plain Text (For ATS)",
                description = "Paste in Naukri / LinkedIn Easy Apply text box, keeps single-column reading order",
                icon = Icons.Default.ContentCopy
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialStepCard(
                stepNumber = "Step 2",
                title = "Download PDF (Single-Column)",
                description = "For email / company portal, no tables, no graphics",
                icon = Icons.Default.PictureAsPdf
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialStepCard(
                stepNumber = "Step 3",
                title = "Download TXT",
                description = "For old ATS systems that only accept .txt",
                icon = Icons.Default.Description
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dontShowChecked = !dontShowChecked }
                    .padding(vertical = 4.dp)
                    .testTag("export_tutorial_dont_show_checkbox"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = dontShowChecked,
                    onCheckedChange = { dontShowChecked = it }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.export_dont_show_again),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (onProceedAction != null) {
                        onProceedAction(dontShowChecked)
                    } else {
                        onDismiss(dontShowChecked)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_tutorial_got_it_button")
            ) {
                Text(
                    text = if (onProceedAction != null) "Continue Export" else "Got it",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${stringResource(R.string.about_designed_by)} • ${stringResource(R.string.about_ats_optimized)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = atsColors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    val atsColors = LocalAtsColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, atsColors.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$stepNumber: $title",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = atsColors.muted
                )
            }
        }
    }
}
