package com.example.myBehavior

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.example.myBehavior.databinding.ActivityMainBinding
import com.example.myBehavior.view.MyBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.Direction


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //val bi = LeftSheetBehavior.from(findViewById(R.id.sheet))
        //   val bi = MyButtomSheetBehavior.from(findViewById(R.id.sheet))
        //  val bi = MyBehavior.from(findViewById(R.id.sheet))
        val bi = GlobalBehavior.since(findViewById(R.id.sheet))
        //bi.setFoldDirection(Direction.TOP_SHEET)

        findViewById<Button>(R.id.button).setOnClickListener {

            supportFragmentManager.beginTransaction().replace(R.id.fl2, BlankFragment()).commit()
            Log.d("MainActivity", supportFragmentManager.executePendingTransactions().toString())
            //   bi.setState(STATE_EXPANDED)

        }

        binding.button6.setOnClickListener {
            //    bi.setState(STATE_COLLAPSED)
        }

        binding.button2.setOnClickListener {
            //  bi.setState(STATE_HALF_EXPANDED)
        }
    }
}