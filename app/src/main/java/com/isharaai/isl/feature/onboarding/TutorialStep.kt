package com.isharaai.isl.feature.onboarding

/** A single step in the interactive tutorial. */
data class TutorialStep(
    val targetKey: String,
    val descEn: String,
    val descBn: String
)

val TUTORIAL_STEPS = listOf(
    TutorialStep("chat_input", "Type your message here to talk with Ishara", "এখানে টাইপ করে ইশারার সাথে কথা বলুন"),
    TutorialStep("attach_btn", "Attach photos from your gallery", "গ্যালারি থেকে ছবি যুক্ত করুন"),
    TutorialStep("camera_btn", "Take a photo to know about it's sign language", "যেকোনো জিনিসের ছবি তুলে তার সাইন জেনে নিন"),
    TutorialStep("mic_btn", "Tap to speak", "কথা বলতে চাপুন — ইংরেজি বা বাংলা বেছে নিন"),
    TutorialStep("send_btn", "Tap to send your message", "বার্তা পাঠাতে চাপুন"),
    TutorialStep("new_chat_btn", "Start a new conversation", "নতুন কথোপকথন শুরু করুন"),
    TutorialStep("settings_btn", "Open settings, check history and language options", "সেটিংস খুলুন, ইতিহাস দেখুন এবং ভাষা বেছে নিন")
)
