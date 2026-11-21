package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.R
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiMessage
import com.devai.studio.data.ChatHistoryStore
import com.devai.studio.databinding.FragmentChatBinding
import com.devai.studio.util.AiSupport
import com.devai.studio.util.AndroidUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<AiMessage>()
    private var job: Job? = null
    private var streaming = false

    @Volatile
    private var cancelled = false

    private val systemPrompt = "Ты — DevAI, полезный ИИ-ассистент разработчика. " +
        "Отвечай по делу, давай рабочие примеры кода."

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChatAdapter()
        binding.chatList.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.chatList.adapter = adapter

        messages.clear()
        messages.addAll(ChatHistoryStore.load(requireContext()))
        adapter.submit(messages)
        scrollToBottom()

        binding.btnSend.setOnClickListener { onSendOrStop() }
        binding.inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSendOrStop()
                true
            } else {
                false
            }
        }
        binding.inputText.doAfterTextChanged { updateSendEnabled() }
        updateSendEnabled()
        updateModelLabel()
    }

    override fun onResume() {
        super.onResume()
        updateModelLabel()
    }

    override fun onDestroyView() {
        job?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun updateModelLabel() {
        val b = _binding ?: return
        val p = AiSupport.provider(requireContext())
        b.modelLabel.text =
            if (p == null) getString(R.string.chat_no_provider)
            else p.name + " · " + AiSupport.model(requireContext(), p)
    }

    private fun updateSendEnabled() {
        val b = _binding ?: return
        b.btnSend.isEnabled = streaming || b.inputText.text.toString().isNotBlank()
    }

    private fun setStreamingUi(isStreaming: Boolean) {
        streaming = isStreaming
        val b = _binding ?: return
        b.btnSend.setImageResource(if (isStreaming) R.drawable.ic_stop else R.drawable.ic_send)
        b.btnSend.contentDescription =
            getString(if (isStreaming) R.string.chat_stop else R.string.chat_send)
        updateSendEnabled()
    }

    private fun onSendOrStop() {
        if (streaming) stop() else send()
    }

    private fun stop() {
        cancelled = true
        job?.cancel()
        setStreamingUi(false)
    }

    private fun send() {
        val text = binding.inputText.text.toString().trim()
        if (text.isEmpty()) return
        val ctx = requireContext()

        val cfg = try {
            AiSupport.config(ctx)
        } catch (e: IllegalStateException) {
            AndroidUtils.toast(ctx, e.message ?: getString(R.string.chat_no_key))
            return
        }

        binding.inputText.setText("")
        messages.add(AiMessage("user", text))
        val assistantIndex = messages.size
        messages.add(AiMessage("assistant", ""))
        adapter.submit(messages)
        scrollToBottom()
        cancelled = false
        setStreamingUi(true)

        job = viewLifecycleOwner.lifecycleScope.launch {
            val sb = StringBuilder()
            var lastUi = 0L
            try {
                val client = AiClientFactory.create(cfg)
                val apiMessages = ArrayList<AiMessage>()
                apiMessages.add(AiMessage("system", systemPrompt))
                apiMessages.addAll(messages.take(assistantIndex))

                val full = client.chat(apiMessages) { delta ->
                    synchronized(sb) { sb.append(delta) }
                    if (!cancelled) {
                        val now = System.currentTimeMillis()
                        if (now - lastUi > 70) {
                            lastUi = now
                            val snap = synchronized(sb) { sb.toString() }
                            activity?.runOnUiThread {
                                if (!cancelled) {
                                    adapter.updateAt(assistantIndex, snap)
                                    scrollToBottom()
                                }
                            }
                        }
                    }
                }

                if (!cancelled) {
                    messages[assistantIndex] = AiMessage("assistant", full.ifBlank { sb.toString() })
                    adapter.submit(messages)
                    ChatHistoryStore.save(ctx, messages)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                if (!cancelled) {
                    messages[assistantIndex] = AiMessage(
                        "assistant",
                        ctx.getString(R.string.chat_error, e.message ?: e.javaClass.simpleName)
                    )
                    adapter.submit(messages)
                    ChatHistoryStore.save(ctx, messages)
                }
            } finally {
                setStreamingUi(false)
                job = null
            }
        }
    }

    private fun scrollToBottom() {
        val b = _binding ?: return
        if (messages.isNotEmpty()) b.chatList.scrollToPosition(messages.size - 1)
    }
}
