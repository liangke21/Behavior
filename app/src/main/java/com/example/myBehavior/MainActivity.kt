package com.example.myBehavior

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.myBehavior.databinding.ActivityMainBinding
import com.example.myBehavior.fragment.BottomFragment
import com.example.myBehavior.fragment.LeftFragment
import com.example.myBehavior.fragment.RightFragment
import com.example.myBehavior.fragment.TopFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var tagFragment: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button1.setOnClickListener {
            tagFragment = BottomFragment.TAG
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.translate_right_enter, R.anim.translate_right_exit)
                .add(binding.fl.id, BottomFragment.newInstance(), tagFragment).commit()
        }

        binding.button2.setOnClickListener {
            tagFragment = TopFragment.TAG
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.translate_right_enter, R.anim.translate_right_exit)
                .add(binding.fl.id, TopFragment.newInstance(), tagFragment).commit()
        }
        binding.button3.setOnClickListener {
            tagFragment = LeftFragment.TAG
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.translate_right_enter, R.anim.translate_right_exit)
                .add(binding.fl.id, LeftFragment.newInstance(), tagFragment).commit()
        }
        binding.button4.setOnClickListener {
            tagFragment = RightFragment.TAG
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.translate_right_enter, R.anim.translate_right_exit)
                .add(binding.fl.id, RightFragment.newInstance(), tagFragment).commit()
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
            val fragment = supportFragmentManager.findFragmentByTag(tagFragment)
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.translate_right_enter, R.anim.translate_right_exit)
                    .remove(it).commit()
                return true
            }

        }
        return super.onKeyDown(keyCode, event)
    }
}