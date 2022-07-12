package com.example.myBehavior.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myBehavior.databinding.RightFragmentBinding
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.State

class RightFragment : Fragment() {
   private lateinit var binding: RightFragmentBinding
    companion object {
        const val TAG = "RightFragment"
        fun newInstance() = RightFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RightFragmentBinding.inflate(layoutInflater, container, false)

        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.RIGHT_SHEET)
        binding.rightButton1.setOnClickListener {
            bi.setState(State.STATE_EXPANDED)
        }
        binding.rightButton2.setOnClickListener {
            bi.setState(State.STATE_COLLAPSED)
        }
        binding.rightButton3.setOnClickListener {
            bi.setState(State.STATE_HALF_EXPANDED)
        }
        binding.rightButton4.setOnClickListener {
            bi.setState(State.STATE_HIDDEN)
        }
        return binding.root
    }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                view.setOnTouchListener { v, _ ->
                    v.performClick()
                    true
                }
            }
}