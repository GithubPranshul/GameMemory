package com.example.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.example.memorygame.newmodels.UserImageList
import com.example.memorygame.utills.EXTRA_BOARD_SIZE
import com.example.memorygame.utills.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {
    companion object{
        private const val CREATE_REQUEST_CODE =248
    }

    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? =null
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

        setupBoard()
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
                        setupBoard()
                    })
                }else {

                setupBoard()
                }


            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(TAG,"Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_board,null)
        showAlertDialog("Fetch memory game",boardDownloadView,View.OnClickListener {
            // Grab the text of the game name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if(userImageList?.images == null) {
                Log.e(TAG,"Invalid custom game from Firestore ")
                Snackbar.make(clRoot,"Sorry, we couldn't find any such game ,$gameName ",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size*2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for(imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot,"You're now playing '$customGameName'!",Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setupBoard()
        }.addOnFailureListener { exception ->
            Log.e(TAG,"Exception when retrieving game",exception)
        }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView,View.OnClickListener {
            // Set a new value for board size
            val desireBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rvEasy -> BoardSize.EASY
                R.id.rvMedium -> BoardSize.MEDIUM
                R.id.rvHard -> BoardSize.HARD
                else-> BoardSize.HARD
            }
            // Navigate to a new activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desireBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
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
            gameName = null
            customGameImages = null
            setupBoard()
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

    private fun setupBoard() {
        supportActionBar?.title = gameName?: getString(R.string.app_name)
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
        memoryGame = MemoryGame(boardSize,customGameImages)
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
                 CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW,Color.GREEN,Color.MAGENTA)).oneShot()
             }
         }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
         adapter.notifyDataSetChanged()
    }
}