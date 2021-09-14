package com.example.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {


    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var adapter:MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        setupBorad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.mi_refresh ->{
                // setup the game again
                if(memoryGame.getNumMoves()> 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game?",null,View.OnClickListener {
                        setupBorad()
                    })
                }else {

                setupBorad()
                }


            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        when(boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rvEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rvMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rvHard)
        }

        showAlertDialog("Choose new size",boardSizeView,View.OnClickListener {
            // Set a new value for board size
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rvEasy -> BoardSize.EASY
                R.id.rvMedium -> BoardSize.MEDIUM
                R.id.rvHard -> BoardSize.HARD
                else-> BoardSize.HARD
            }
            setupBorad()
        })
    }

    private fun showAlertDialog(title: String,view: View?,positiveButtonClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Ok") {_,_ ->
                positiveButtonClickListener.onClick(null)
            }.show()
    }

    private fun setupBorad() {
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0/9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0/12"
            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this,boardSize,memoryGame.cards,object : MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateGameWithFlip(position: Int) {
        // Error checking
        if(memoryGame.haveWonGame()) {
            // Alert the user of an invalid move
                Snackbar.make(clRoot,"You already won!",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)) {
            // Alert the user fo an invalid move
                Snackbar.make(clRoot,"Invalid move!",Snackbar.LENGTH_LONG).show()
               return
        }
        // Actually flip over the card
         if(memoryGame.flipCard(position)){
             Log.i(TAG,"found a match! Num pairs found: ${memoryGame.numPairFound}")

             val color = ArgbEvaluator().evaluate(
                 memoryGame.numPairFound.toFloat() / boardSize.getNumPairs(),
                 ContextCompat.getColor(this,R.color.color_progress_none),
                 ContextCompat.getColor(this,R.color.color_progress_full)
             ) as Int
             tvNumPairs.setTextColor(color)
             tvNumPairs.text = "Pairs: ${memoryGame.numPairFound} / ${boardSize.getNumPairs()}"
             if(memoryGame.haveWonGame()) {
                 Snackbar.make(clRoot,"You won! Congratulations.",Snackbar.LENGTH_LONG).show()
             }
         }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
         adapter.notifyDataSetChanged()
    }
}