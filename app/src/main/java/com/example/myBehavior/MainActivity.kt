package com.example.myBehavior

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myBehavior.databinding.ActivityMainBinding
import com.example.myBehavior.view.LeftSheetBehavior
import com.example.myBehavior.view.LeftSheetBehavior.Companion.STATE_COLLAPSED
import com.example.myBehavior.view.LeftSheetBehavior.Companion.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bi = LeftSheetBehavior.from(findViewById(R.id.sheet))
        findViewById<Button>(R.id.button).setOnClickListener {

            bi.setState(STATE_EXPANDED)

            supportFragmentManager.beginTransaction().replace(R.id.fl2,BlankFragment()).commit()
            supportFragmentManager.executePendingTransactions()

        }

        binding.button6.setOnClickListener {
            bi.setState(STATE_COLLAPSED)
        }


    }
}