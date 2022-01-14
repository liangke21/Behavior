package com.example.myBehavior

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import com.example.myBehavior.databinding.ActivityMainBinding
import com.example.myBehavior.fragment.BottomFragment
import com.example.myBehavior.fragment.TopFragment
import com.liangke.viewpoint.behavior.GlobalBehavior


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var tagFragment: String

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //val bi = LeftSheetBehavior.from(findViewById(R.id.sheet))
        //   val bi = MyButtomSheetBehavior.from(findViewById(R.id.sheet))
        //  val bi = MyBehavior.from(findViewById(R.id.sheet))

        //bi.setFoldDirection(Direction.TOP_SHEET)


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
            //  bi.setState(STATE_HALF_EXPANDED)
        }
        binding.button4.setOnClickListener {
            //  bi.setState(STATE_HALF_EXPANDED)
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