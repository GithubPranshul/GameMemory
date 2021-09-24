package com.example.memorygame

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utills.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import kotlin.math.min

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14

    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var boardSize: BoardSize
    private var numImagePaired = -1
    private  val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)


        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize =  intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagePaired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics(0/${numImagePaired})"

        //Creating method for firebase to store data
        btnSave.setOnClickListener{
            saveDataToFirebase()
        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher{

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnableSavedButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        adapter = ImagePickerAdapter(this,chosenImageUris,boardSize,object : ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
               if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                launchIntentForPhotos()
            } else {
                requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,
                    READ_EXTERNAL_PHOTOS_CODE)
              }
            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchIntentForPhotos()
        } else {
            Toast.makeText(this,"In order to create a custom game, you need to provide access to your photos",Toast.LENGTH_LONG).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE || resultCode!= Activity.RESULT_OK || data == null)  {
            Log.w(TAG,"Did not get data back from the launched activity, user likely canceled flow")
            return
        }
       val selectedUri =  data.data
        val clipData = data.clipData
        if(clipData!= null) {
            Log.i(TAG,"clipData numImages ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagePaired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG,"data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / ${numImagePaired})"
        btnSave.isEnabled = shouldEnableSavedButton()

    }

    private fun shouldEnableSavedButton(): Boolean {
        // check if we should enable the save button or not

        if(chosenImageUris.size != numImagePaired) {
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length< MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"Choose pics"),PICK_PHOTO_CODE)
    }


    private fun saveDataToFirebase() {
        Log.i(TAG,"saveDataToFirebase")
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        Log.i(TAG,"saveDataToFirebase")

        // Check that we're not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if(document != null && document.data!= null){
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exists with the name '$customGameName'.Please choose another name")
                    .setPositiveButton("Ok",null)
                    .show()
                btnSave.isEnabled = true

            } else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{ exception ->
            Log.e(TAG,"Encounter error while saving memory game",exception)
            Toast.makeText(this,"Encountered error while memory game",Toast.LENGTH_SHORT)
                .show()
        }


    }

    private fun handleImageUploading(gameName: String) {
       // Checking for visibility of progressbar
        pbUploading.visibility = View.VISIBLE

        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for((index,photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()} -${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask{photoUploadTask ->
                    Log.i(TAG,"Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl

                }.addOnCompleteListener{downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful) {
                        Log.e(TAG,"Exception with Firebase Storage ",downloadUrlTask.exception)
                        Toast.makeText(this,"Failed to upload image",Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size *100 / chosenImageUris.size
                    Log.i(TAG,"Finsihed uploading $photoUri,num uploaded ${uploadedImageUrls.size}")
                    if(uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName,uploadedImageUrls)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // Todo upload this into fireStore
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with some creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Now you can download your game and let's play! $gameName")
                   // Toast.makeText(this,"Upload complete! Now you can download your game and let's play! $gameName",Toast.LENGTH_SHORT).show()
                    .setPositiveButton("Ok") {_, _, ->

                    val resultData = Intent()
                  resultData.putExtra(EXTRA_GAME_NAME, gameName)
                  setResult(Activity.RESULT_OK, resultData)
                  finish()
                    }.show()
            }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        Log.i(TAG,"Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaleBitMap = BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG,"Scaled width ${scaleBitMap.width} and height ${scaleBitMap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaleBitMap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }
}

// Trying in same module
class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_type,parent,false)
        val cardWidth: Int= parent.width / boardSize.getWidth()
        val cardHeight: Int = parent.height/ boardSize.getHeight()
        val cardSideLength: Int = min(cardWidth,cardHeight)
        val layoutParams= view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }

    }

    override fun getItemCount() = boardSize.getNumPairs()


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind() {
            ivCustomImage.setOnClickListener {
                // Launch intent for select photo
                imageClickListener.onPlaceHolderClicked()
            }
        }
        fun bind(uri: Uri){
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }
    }

}

