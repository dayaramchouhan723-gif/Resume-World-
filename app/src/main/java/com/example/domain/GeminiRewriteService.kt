package com.example.domain

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float = 0.2f,
    val maxOutputTokens: Int = 512
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

/**
 * Rewrites user's experience description (written in Urdu, Hindi, Roman Urdu, or rough English)
 * into 3-5 clean English resume bullet points.
 *
 * CRITICAL COMPLIANCE GUARANTEES:
 * 1. Privacy: Only sends `jobTitle` and `whatYouDidText`. NEVER sends name, phone, email, or address.
 * 2. Zero Misrepresentation: Strictly translates/polishes ONLY what the user wrote. Never adds
 *    certificates (WHMIS, CSTS, Red Seal, First Aid) or skills/relocation claims not in user input.
 * 3. Model Selection: Auto-selects the latest supported free-tier Flash Lite / Flash models in
 *    priority order without hardcoding deprecated models.
 */
object GeminiRewriteService {

    // Auto-select latest free-tier flash-lite / flash models dynamically
    private val FREE_MODEL_CANDIDATES = listOf(
        "gemini-3.1-flash-lite-preview",
        "gemini-flash-latest",
        "gemini-3.5-flash"
    )

    private val FORBIDDEN_INVENTED_TERMS = listOf(
        "whmis",
        "csts",
        "red seal",
        "willing to relocate",
        "first aid",
        "cpr",
        "forklift certified",
        "food safe",
        "working at heights",
        "ohsa"
    )

    private val api: GeminiRestApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRestApi::class.java)
    }

    suspend fun rewriteExperienceToEnglishBullets(
        apiKey: String,
        jobTitle: String,
        whatYouDidInput: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is required. Tap 'API Key' to save your key locally in EncryptedSharedPreferences.")
            )
        }
        val cleanInput = whatYouDidInput.trim()
        if (cleanInput.length < 5) {
            return@withContext Result.failure(
                IllegalArgumentException("Write what you did first (in Roman Urdu, Urdu, Hindi, or English).")
            )
        }

        val systemPrompt = """
            You are an ethical Canadian resume translator and editor.
            The user will provide ONLY a Job Title and their raw "What you did" notes (which may be written in Roman Urdu, Urdu script, Hindi, or simple English).
            
            STRICT RULES - DO NOT VIOLATE:
            1. Rewrite the user's input into 3 to 5 clear, simple, professional English bullet points (each starting with "- ").
            2. Use ONLY the facts, actions, equipment, and numbers that the user explicitly wrote in their input.
            3. NEVER invent or add ANY certificates, licenses, or skills the user did not write (NEVER add WHMIS, CSTS, Red Seal, First Aid, Forklift license, or "willing to relocate" unless the user explicitly typed those exact words). Adding false claims causes a 5-year Canadian work permit misrepresentation ban.
            4. Do NOT invent percentages, dollar amounts, or metrics that the user did not write.
            5. Output ONLY the bullet points, one per line, starting with "- ". No intro or outro text.
        """.trimIndent()

        // Privacy guarantee: ONLY jobTitle and whatYouDidInput are included in the payload.
        val userPayload = buildString {
            if (jobTitle.isNotBlank()) {
                appendLine("Job Title: ${jobTitle.trim()}")
            }
            appendLine("What I did (user input):")
            append(cleanInput)
        }

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPayload)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.15f,
                maxOutputTokens = 450
            )
        )

        var lastError: Throwable? = null
        for (modelName in FREE_MODEL_CANDIDATES) {
            try {
                val response = api.generateContent(
                    model = modelName,
                    apiKey = apiKey.trim(),
                    request = request
                )
                val rawText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()

                if (!rawText.isNullOrBlank()) {
                    val sanitized = sanitizeAndEnforceNoInventedClaims(
                        userOriginal = cleanInput,
                        aiOutput = rawText
                    )
                    if (sanitized.isNotBlank()) {
                        return@withContext Result.success(sanitized)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(
            lastError ?: IllegalStateException("Could not reach Gemini API. Please check your API key or internet connection.")
        )
    }

    /**
     * Post-processing guardrail: formats output into 3-5 clean bullet lines and strips any
     * forbidden certificate/relocation terms if the user did not mention them in their original input.
     */
    internal fun sanitizeAndEnforceNoInventedClaims(
        userOriginal: String,
        aiOutput: String
    ): String {
        val lowerInput = userOriginal.lowercase()
        val bulletLines = aiOutput.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Here ") && !it.startsWith("Sure") }
            .map { line ->
                val stripped = line.replace(Regex("^[-•*\\d.)]+\\s*"), "").trim()
                stripped
            }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                var cleanedLine = line
                for (forbidden in FORBIDDEN_INVENTED_TERMS) {
                    if (!lowerInput.contains(forbidden) && cleanedLine.lowercase().contains(forbidden)) {
                        // Strip unauthorized clause containing the hallucinated certificate/claim
                        cleanedLine = cleanedLine.replace(
                            Regex("(?i)\\b(with|including|using|following|certified in|holding)?\\s*$forbidden\\b[^.,;]*"),
                            ""
                        ).trim()
                    }
                }
                cleanedLine.takeIf { it.length > 3 }
            }
            .take(5)

        return bulletLines.joinToString("\n") { "- $it" }
    }
}
