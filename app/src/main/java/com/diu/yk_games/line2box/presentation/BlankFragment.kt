package com.diu.yk_games.line2box.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.databinding.FragmentBlankBinding

class BlankFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentBlankBinding
        .inflate(inflater, container, false)
        .root
}