package com.example.myBehavior

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.myBehavior.view.LeftSheetBehavior
import com.example.myBehavior.view.LeftSheetBehavior.Companion.STATE_EXPANDED


class MainActivity : AppCompatActivity() {
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

           findViewById<Button>(R.id.button).setOnClickListener {

               val bi= LeftSheetBehavior.from(findViewById(R.id.sheet))
               bi.setState(STATE_EXPANDED)

           }
    }
}