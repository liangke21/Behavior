package com.example.myBehavior.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myBehavior.R
import com.example.myBehavior.databinding.TopFragmentBinding
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.callbacks.GlobalCallbacks
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.State
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

        val callback=object:GlobalCallbacks(){
            override fun onStateChanged(globalBehavior: View, state: State) {

            }

            override fun onSlide(globalBehavior: View, offset: Int) {

                Log.d(TAG,offset.toString())
            }
        }
        val bi = GlobalBehavior.since(binding.sheet.sheet)
        bi.setFoldDirection(Direction.TOP_SHEET)
        bi.addGlobalCallbacks(callback)
        binding.topButton1.setOnClickListener {
            bi.setState(STATE_EXPANDED)
        }

        binding.topButton2.setOnClickListener {
            bi.setState(STATE_COLLAPSED)
            bi.removeGlobalCallbacks(callback)
        }
        binding.topButton3.setOnClickListener {
            bi.setState(STATE_HALF_EXPANDED)
        }
        binding.topButton4.setOnClickListener {
            bi.setState(STATE_HIDDEN)
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
