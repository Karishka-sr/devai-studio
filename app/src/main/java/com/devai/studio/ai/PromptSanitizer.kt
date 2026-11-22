package com.devai.studio.ai

/**
 * Removes model control tokens that some providers reject in plain text input.
 * Gemini returns INVALID_ARGUMENT when a prompt contains tokens such as <bos>.
 */
object PromptSanitizer {

    private val controlToken = Regex(
        """<\|(?:bos|eos|unk|pad|s|start_of_turn|end_of_turn|im_start|im_end|system|user|assistant|endoftext|begin_of_text|end_of_text|eot_id|start_header_id|end_header_id|end|tool)\|>|</?(?:bos|eos|unk|pad|s|start_of_turn|end_of_turn)>""",
        RegexOption.IGNORE_CASE,
    )

    fun clean(text: String): String =
        controlToken.replace(text, "")
            .filter { ch ->
                ch == '\n' || ch == '\t' || ch == '\r' ||
                    (ch.code >= 0x20 && ch != '\u007F' && ch != '\uFEFF')
            }
}
