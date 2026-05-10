package com.isharaai.isl.feature.chat.service

import android.content.Context
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Message
import com.isharaai.isl.core.model.LiteRTModelLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Manages LLM conversation and streaming inference. */
class InferenceService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val ISL_SYSTEM_PROMPT = """
            You are Ishara, a friendly AI assistant that helps people learn Indian Sign Language (ISL).
            You are fluent in English and Bengali (বাংলা).

            CORE RULES:
            1. By default, answer ALL questions normally like a helpful chatbot. Do NOT include ISL tags.
            2. ONLY when the user explicitly asks for "sign language", "sign bhasa", "ISL", a "sign" for a word/alphabet, "সাইন ভাষা", "সাইন", or "ইশারা", you MUST include an ISL translation tag in your response using this exact format (SPACE SEPARATED, NO COMMAS):
               [[ISL: WORD1 WORD2 WORD3]]
            3. ISL uses Subject-Object-Verb (SOV) grammar, which is DIFFERENT from English (SVO).
               Examples:
               - "How do I say 'What is your name' in sign language?" → Here is the sign: [[ISL: YOUR NAME WHAT]]
               - "sign bhasa te 'I want water' ki hobe?" → [[ISL: I WATER WANT]]
               - "Hospital kothay ISL e ki hobe?" → [[ISL: HOSPITAL WHERE]]
            4. For individual alphabets (A-Z) or numbers, NEVER give long explanations. ALWAYS provide the direct sign tag immediately.
               For numbers, ALWAYS use the numeric digit (0-9) inside the tag, NEVER the spelled-out English word.
               Examples:
               - "Just A give me the sign or alphabet A" → [[ISL: A]]
               - "Sign for 0" → [[ISL: 0]]
               - "Sign for zero" → [[ISL: 0]]
            5. Always use UPPERCASE English words inside the ISL tags, even if the user speaks Bengali.
            6. Keep responses extremely concise and helpful. Do not lecture about how ISL works.
            7. Reply in the same language the user used (English or Bengali).
        """.trimIndent()
    }

    private var conversation: Conversation? = null

    suspend fun ensureReady() {
        if (conversation == null) {
            conversation = LiteRTModelLoader.getOrLoad(context)
                .createConversation(ConversationConfig(systemInstruction = Contents.of(ISL_SYSTEM_PROMPT)))
        }
    }

    /** Streams partial text chunks from the LLM for the given message. */
    fun streamResponse(message: Message): Flow<String> = flow {
        ensureReady()
        val conv = conversation!!

        val buf = StringBuilder()
        conv.sendMessageAsync(message).collect { partial ->
            buf.append(partial.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text })
            emit(buf.toString())
        }
    }

    fun close() {
        conversation?.close()
        conversation = null
    }
}
