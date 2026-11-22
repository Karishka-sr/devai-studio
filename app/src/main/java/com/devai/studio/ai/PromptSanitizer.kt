package com.devai.studio.ai

/**
 * Removes model control tokens that LiteRT-LM and hosted providers reject in plain text.
 * The native LiteRT-LM engine fails with "Input contains bos control token" when a
 * conversation contains tokens such as <bos>, <|im_start|> or <start_of_turn>.
 */
object PromptSanitizer {

    private val angleToken = Regex(
        """<\s*/?\s*(?:bos|eos|unk|pad|s|start_of_turn|end_of_turn|im_start|im_end|system|user|assistant|endoftext|begin_of_text|end_of_text|eot_id|start_header_id|end_header_id|end|tool|observation|image|audio|video|ref|repo_name|file_separator|im_sep|start_of_query|end_of_query|object_ref_start|object_ref_end|box_start|box_end|quad_start|quad_end)\s*>""",
        RegexOption.IGNORE_CASE,
    )

    private val pipeToken = Regex("""<\|[^|>\r\n]{1,64}\|>""")

    fun clean(text: String): String =
        pipeToken.replace(angleToken.replace(text, ""), "")
            .filter { ch ->
                ch == '\n' || ch == '\t' || ch == '\r' ||
                    (ch.code >= 0x20 && ch != '\u007F' && ch != '\uFEFF')
            }
}
