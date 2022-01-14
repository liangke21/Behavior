package com.example.myBehavior.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myBehavior.databinding.LeftFragmentBinding
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.State

class LeftFragment : Fragment() {
    private lateinit var binding: LeftFragmentBinding

    companion object {
        const val TAG = "LeftFragment"
        fun newInstance() = LeftFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LeftFragmentBinding.inflate(layoutInflater, container, false)

        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.LEFT_SHEET)
        binding.leftButton1.setOnClickListener {
            bi.setState(State.STATE_EXPANDED)
        }
        binding.leftButton2.setOnClickListener {
            bi.setState(State.STATE_COLLAPSED)
        }
        binding.leftButton3.setOnClickListener {
            bi.setState(State.STATE_HALF_EXPANDED)
        }
        binding.leftButton4.setOnClickListener {
            bi.setState(State.STATE_HIDDEN)
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnTouchListener { v, _ ->
            //在这里面拦截点击事件,并进行相应的操作
            v.performClick()
            true
        }
    }


}