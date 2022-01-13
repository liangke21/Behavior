package com.example.myBehavior.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myBehavior.R
import com.example.myBehavior.databinding.TopFragmentBinding
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.Direction

class TopFragment : Fragment() {
    private lateinit var binding: TopFragmentBinding

    companion object {
        private val TAG = TopFragment::class.java.canonicalName
        fun newInstance() = TopFragment()
    }

    private lateinit var viewModel: TopViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TopFragmentBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this).get(TopViewModel::class.java)
        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.TOP_SHEET)
        binding.topButton1.setOnClickListener {

        }
        return binding.root
    }
}
