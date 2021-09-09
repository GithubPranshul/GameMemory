package com.example.memorygame.models

import com.example.memorygame.utills.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize) {


    val cards: List<MemoryCard>
    val numsPairFound =0

    init {
        val chosenImages =  DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        cards = randomizedImages.map{MemoryCard(it)}
    }

    fun flipCard(position: Int) {
        /*
           Three cases:
            0 cards previously flipped over ==> flip over the selected card
            1 cards previously flipped over ==> flip the selected card + check if the images match
            2 cards previously flipped over ==> restore cards + flip over the selected card
         */
       val card = cards[position]
        card.isFaceUp = !card.isFaceUp
    }
}
