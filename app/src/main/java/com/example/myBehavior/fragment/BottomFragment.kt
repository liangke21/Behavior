package com.example.myBehavior.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myBehavior.R
import com.example.myBehavior.databinding.BottomFragmentBinding
import com.example.myBehavior.databinding.TopFragmentBinding
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.State

class BottomFragment : Fragment() {
 private lateinit var binding:BottomFragmentBinding
    companion object {
        const val TAG="BottomFragment"
        fun newInstance() = BottomFragment()
    }

    private lateinit var viewModel: BottomViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomFragmentBinding.inflate(layoutInflater, container, false)

        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.BOTTOM_SHEET)
        binding.bottomButton1.setOnClickListener {
            bi.setState(State.STATE_EXPANDED)
        }

        binding.bottomButton2.setOnClickListener {
            bi.setState(State.STATE_COLLAPSED)
        }
        binding.bottomButton3.setOnClickListener {
            bi.setState(State.STATE_HALF_EXPANDED)
        }
        binding.bottomButton4.setOnClickListener {
            bi.setState(State.STATE_HIDDEN)
        }
        return binding.root
    }

}