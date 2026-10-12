package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AtsAnalysisResult
import com.example.domain.AtsAnalyzer
import com.example.domain.AtsCheckItem
import com.example.domain.CategoryBar
import com.example.domain.CheckLevel
import com.example.domain.KeywordMatchResult
import com.example.domain.KeywordToken
import com.example.ui.theme.LocalAtsColors

@Composable
fun colorForLevel(level: CheckLevel): Color {
    val atsColors = LocalAtsColors.current
    return when (level) {
        CheckLevel.OK -> atsColors.ok
        CheckLevel.WARN -> atsColors.warn
        CheckLevel.BAD -> atsColors.bad
    }
}

/**
 * Honest Keyword Match Header + Ring Dial.
 * Prominently displays: "Keyword Match: 62% - Estimate only, not real ATS"
 */
@Composable
fun AtsScoreHeader(
    result: AtsAnalysisResult,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    val displayPct = result.keywordMatch?.pct ?: result.totalScore
    val level = AtsAnalyzer.levelForScore(displayPct)
    val dialColor = colorForLevel(level)
    val trackColor = atsColors.line

    val animatedProgress by animateFloatAsState(
        targetValue = (displayPct.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "dial_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
            .testTag("ats_score_header")
    ) {
        // Prominent Honest Match Banner required by Rule A
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = dialColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, dialColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("keyword_match_headline_banner")
        ) {
            Text(
                text = result.keywordMatchHeadline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .testTag("ats_score_dial"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(112.dp)) {
                    val strokeWidthPx = 10.dp.toPx()
                    val arcDiameter = size.minDimension - strokeWidthPx
                    val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
                    val arcSize = androidx.compose.ui.geometry.Size(arcDiameter, arcDiameter)

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx)
                    )
                    drawArc(
                        color = dialColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    )
                }
                Text(
                    text = "$displayPct%",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("ats_score_number")
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.bandLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("ats_score_band")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = atsColors.muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                val passedCount = result.formattingChecklist.count { it.passed }
                Text(
                    text = "$passedCount / ${result.formattingChecklist.size} checks passed • ${result.wordCount} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = atsColors.ok
                )
            }
        }
    }
}

/**
 * Category progress bars matching `.bar` and `.track` in the HTML specification.
 */
@Composable
fun AtsCategoryBarsSection(
    bars: List<CategoryBar>,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ats_category_bars"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bars.forEach { bar ->
            val level = AtsAnalyzer.levelForScore(bar.score)
            val fillColor = colorForLevel(level)
            val fraction = (bar.score.coerceIn(0, 100)) / 100f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(84.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(atsColors.line)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(fillColor)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${bar.score}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

/**
 * Priority action items / checklist items with ✅ and ❌ indicators.
 */
@Composable
fun AtsCheckListSection(
    items: List<AtsCheckItem>,
    useCheckmarkEmoji: Boolean = false,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = atsColors.line)
            }
            val badgeColor = colorForLevel(item.level)
            val symbol = if (useCheckmarkEmoji) {
                if (item.level == CheckLevel.OK) "✅" else "❌"
            } else {
                when (item.level) {
                    CheckLevel.OK -> "✓"
                    CheckLevel.WARN -> "!"
                    CheckLevel.BAD -> "✕"
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (useCheckmarkEmoji) {
                    Text(
                        text = symbol,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (useCheckmarkEmoji && item.category.isNotBlank()) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = atsColors.muted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Missing Hard Skills & Soft Skills (like Jobscan / SkillSyncer) + Matched Keywords.
 * Never auto-adds keywords—displays a clear Canadian anti-misrepresentation reminder.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissingKeywordsSection(
    keywordMatch: KeywordMatchResult,
    isCopied: Boolean,
    onCopyMissing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    val missingKeywords = keywordMatch.miss
    val matchedKeywords = keywordMatch.hit

    Column(modifier = modifier.fillMaxWidth()) {
        if (missingKeywords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Missing Keywords (${missingKeywords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = onCopyMissing,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, atsColors.line),
                    modifier = Modifier.testTag("copy_missing_keywords_button")
                ) {
                    Text(
                        text = if (isCopied) "Copied" else "Copy list",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = "Do NOT auto-add keywords. Only add a keyword manually to your resume if it is genuinely true for your experience and skills.",
                style = MaterialTheme.typography.bodySmall,
                color = atsColors.warn,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (keywordMatch.missingHardSkills.isNotEmpty()) {
                Text(
                    text = "Hard Skills, Tools & Role Terms (${keywordMatch.missingHardSkills.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missing_keywords_chips"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keywordMatch.missingHardSkills.take(20).forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = atsColors.card,
                            border = BorderStroke(1.dp, atsColors.bad.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "❌ ${kw.display}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (keywordMatch.missingSoftSkills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Soft Skills & Action Terms (${keywordMatch.missingSoftSkills.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missing_soft_keywords_chips"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keywordMatch.missingSoftSkills.take(15).forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = atsColors.card,
                            border = BorderStroke(1.dp, atsColors.warn.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⚠️ ${kw.display}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (matchedKeywords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Matched Keywords Found in Resume (${matchedKeywords.size})",
                style = MaterialTheme.typography.titleSmall,
                color = atsColors.ok,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                matchedKeywords.take(25).forEach { kw ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = atsColors.ok.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, atsColors.ok.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "✅ ${kw.display}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
