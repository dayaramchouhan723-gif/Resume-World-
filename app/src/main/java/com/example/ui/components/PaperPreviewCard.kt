package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ResumeFormState
import com.example.ui.theme.LocalAtsColors
import com.example.ui.theme.PaperBg
import com.example.ui.theme.PaperRule
import com.example.ui.theme.PaperSubText
import com.example.ui.theme.PaperText

/**
 * Single-column Canadian resume paper preview matching `.paper` in the HTML specification:
 * White background (#FFF), crisp dark text (#111), Arial/Sans-Serif typography,
 * clean horizontal rules under section headings, and zero photo/DOB/marital-status fields.
 */
@Composable
fun PaperPreviewCard(
    formState: ResumeFormState,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    val contactItems = formState.contactParts()
    val activeJobs = formState.usedJobs()
    val eduLines = ResumeFormState.splitLines(formState.education)
    val certLines = ResumeFormState.splitLines(formState.certificates)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resume_paper_preview"),
        shape = RoundedCornerShape(6.dp),
        color = PaperBg,
        contentColor = PaperText,
        border = BorderStroke(1.dp, atsColors.line),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // H1: Candidate Name
            Text(
                text = formState.fullName.trim().ifEmpty { "Your Name" },
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                color = PaperText,
                modifier = Modifier.testTag("paper_name")
            )

            // .rt: Target Job Title
            if (formState.targetTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formState.targetTitle.trim(),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = PaperText
                )
            }

            // .rc: Contact Line
            if (contactItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contactItems.joinToString(" | "),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = PaperText
                )
            }

            // Summary Section
            if (formState.summary.isNotBlank()) {
                PaperSectionHeading(title = "Summary")
                Text(
                    text = formState.summary.trim(),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = PaperText
                )
            }

            // Work Experience Section
            if (activeJobs.isNotEmpty()) {
                PaperSectionHeading(title = "Work Experience")
                activeJobs.forEachIndexed { index, job ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (job.title.isNotBlank()) {
                        Text(
                            text = job.title.trim(),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = PaperText
                        )
                    }
                    val empAndDates = listOf(job.employer.trim(), job.dates.trim())
                        .filter { it.isNotEmpty() }
                        .joinToString(" | ")
                    if (empAndDates.isNotEmpty()) {
                        Text(
                            text = empAndDates,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = PaperSubText
                        )
                    }
                    val bullets = ResumeFormState.splitLines(job.bullets)
                    if (bullets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        bullets.forEach { rawLine ->
                            PaperBulletItem(text = ResumeFormState.stripBulletPrefix(rawLine))
                        }
                    }
                }
            }

            // Education Section
            if (eduLines.isNotEmpty()) {
                PaperSectionHeading(title = "Education")
                eduLines.forEach { eduLine ->
                    Text(
                        text = eduLine,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = PaperText,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    )
                }
            }

            // Skills Section
            if (formState.skills.isNotBlank()) {
                PaperSectionHeading(title = "Skills")
                Text(
                    text = formState.skills.trim(),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = PaperText
                )
            }

            // Certifications Section
            if (certLines.isNotEmpty()) {
                PaperSectionHeading(title = "Certifications")
                certLines.forEach { rawCert ->
                    PaperBulletItem(text = ResumeFormState.stripBulletPrefix(rawCert))
                }
            }
        }
    }
}

@Composable
private fun PaperSectionHeading(title: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = PaperText
    )
    Spacer(modifier = Modifier.height(2.dp))
    HorizontalDivider(thickness = 1.dp, color = PaperRule)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun PaperBulletItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 1.5.dp, bottom = 1.5.dp)
    ) {
        Text(
            text = "•",
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = PaperText
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = PaperText,
            modifier = Modifier.weight(1f)
        )
    }
}
