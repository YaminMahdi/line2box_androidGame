package com.diu.yk_games.line2box.presentation.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.databinding.FragmentBlankChatBinding

class BlankChatFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentBlankChatBinding
        .inflate(inflater, container, false)
        .root
}