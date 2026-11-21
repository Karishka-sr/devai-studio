package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.R
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiMessage
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ChatSession
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.databinding.FragmentChatBinding
import com.devai.studio.util.AiSupport
import com.devai.studio.util.AndroidUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<AiMessage>()
    private var job: Job? = null
    private var streaming = false
    private var sessionId: String = ""

    @Volatile
    private var cancelled = false

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

        val session = ChatSessionsStore.getOrCreateCurrent(requireContext())
        sessionId = session.id
        messages.clear()
        messages.addAll(session.messages)
        adapter.submit(messages)
        scrollToBottom()

        binding.btnSend.setOnClickListener { onSendOrStop() }
        binding.btnSessions.setOnClickListener { showSessions() }
        binding.btnRegenerate.setOnClickListener { regenerate() }
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
                apiMessages.add(AiMessage("system", AppPrefs.systemPrompt(ctx)))
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
                    persist()
                    scrollToBottom()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!cancelled) {
                    messages[assistantIndex] = AiMessage(
                        "assistant",
                        ctx.getString(R.string.chat_error, e.message ?: e.javaClass.simpleName)
                    )
                    adapter.submit(messages)
                    persist()
                }
            } finally {
                setStreamingUi(false)
                job = null
            }
        }
    }

    private fun persist() {
        if (sessionId.isBlank() || !isAdded) return
        ChatSessionsStore.updateMessages(requireContext(), sessionId, messages)
    }

    private fun regenerate() {
        if (streaming) return
        val lastUser = messages.indexOfLast { it.role == "user" }
        if (lastUser < 0) return
        val text = messages[lastUser].content
        while (messages.size > lastUser) messages.removeAt(messages.size - 1)
        adapter.submit(messages)
        persist()
        binding.inputText.setText(text)
        send()
    }

    private fun loadSession(s: ChatSession) {
        sessionId = s.id
        ChatSessionsStore.setCurrent(requireContext(), s.id)
        messages.clear()
        messages.addAll(s.messages)
        adapter.submit(messages)
        scrollToBottom()
        updateModelLabel()
    }

    private fun showSessions() {
        val ctx = requireContext()
        persist()
        val all = ChatSessionsStore.list(ctx)
        val titles = all.map { it.title }.toTypedArray()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.chat_sessions)
            .setItems(titles) { _, i -> loadSession(all[i]) }
            .setPositiveButton(R.string.chat_new_session) { _, _ ->
                loadSession(ChatSessionsStore.create(ctx))
            }
            .setNeutralButton(R.string.chat_delete_session) { _, _ -> deleteCurrent() }
            .show()
    }

    private fun deleteCurrent() {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.chat_delete_session)
            .setMessage(R.string.chat_delete_session_confirm)
            .setPositiveButton(R.string.common_ok) { _, _ ->
                ChatSessionsStore.delete(ctx, sessionId)
                loadSession(ChatSessionsStore.getOrCreateCurrent(ctx))
                AndroidUtils.toast(ctx, getString(R.string.chat_session_deleted))
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun scrollToBottom() {
        val b = _binding ?: return
        if (messages.isNotEmpty()) b.chatList.scrollToPosition(messages.size - 1)
    }
}
