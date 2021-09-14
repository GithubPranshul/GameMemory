package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryCard
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val card: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {


    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener{
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth: Int = parent.width/boardSize.getWidth() - (2* MARGIN_SIZE)
        val cardHeight: Int = parent.height/boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength: Int = min(cardWidth,cardHeight)
        val view =  LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height =  cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
     holder.bind(position)
    }

    override fun getItemCount() = boardSize.numCards


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val memoryCard = card[position]
            imageButton.setImageResource( if (card[position].isFaceUp) card[position].identifier else R.drawable.ic_launcher_background)

            //  if matched then setting the  boolean property of foundMatch method

            imageButton.alpha = if(memoryCard.isMatched) (.4f) else (1.0f)
           val colorStateList = if(memoryCard.isMatched) ContextCompat.getColorStateList(context,R.color.color_gray) else null
           ViewCompat.setBackgroundTintList(imageButton,colorStateList)

            imageButton.setOnClickListener{
              Log.i(TAG,"Click on position $position")
              cardClickListener.onCardClicked(position)
          }
        }
    }

}
