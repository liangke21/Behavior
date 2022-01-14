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
import com.liangke.viewpoint.enum.State.*

class TopFragment : Fragment() {
    private lateinit var binding: TopFragmentBinding

    companion object {
        const val TAG = "TopFragment"
        fun newInstance() = TopFragment()
    }

    private lateinit var viewModel: TopViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TopFragmentBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this).get(TopViewModel::class.java)
        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.TOP_SHEET)
        binding.topButton1.setOnClickListener {
            bi.setState(STATE_EXPANDED)
        }

        binding.topButton2.setOnClickListener {
            bi.setState(STATE_COLLAPSED)
        }
        binding.topButton3.setOnClickListener {
            bi.setState(STATE_HALF_EXPANDED)
        }
        binding.topButton4.setOnClickListener {
            bi.setState(STATE_HIDDEN)
        }
        return binding.root
    }
}
