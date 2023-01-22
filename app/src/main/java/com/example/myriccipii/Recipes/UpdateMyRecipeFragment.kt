package com.example.myriccipii.Recipes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.myriccipii.Class.Recipe
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentProfileBinding
import com.example.myriccipii.databinding.FragmentUpdateMyRecipeBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.util.*


class UpdateMyRecipeFragment : Fragment() {
    private lateinit var binding: FragmentUpdateMyRecipeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bool = true
    private lateinit var imageUri : Uri
    private lateinit var photoFile: File
    private val profileFragment: ProfileFragment = ProfileFragment()
    private var isReadPermissionGranted = false
    private var isCameraPermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("QueryPermissionsNeeded", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentUpdateMyRecipeBinding.inflate(inflater, container, false)
        val data = arguments
        var recipeID = data!!.get("recipeID").toString()
        val firebaseStorage = FirebaseStorage.getInstance().reference
        val fm = requireActivity().supportFragmentManager.beginTransaction()

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            isReadPermissionGranted = it[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            isCameraPermissionGranted = it[android.Manifest.permission.CAMERA] ?: isCameraPermissionGranted
        }

        binding.backIv.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            fm.replace(R.id.containerForRecipe, ProfileFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        val uploadBtn: Button = binding.updateUploadBtn
        val recipeNameV: TextInputLayout = binding.updateRecipeNameV
        val recipeNameET: TextInputEditText = binding.updateRecipeNameET
        val recipeDescV: TextInputLayout = binding.updateRecipeDescV
        val recipeDescET: TextInputEditText = binding.updateRecipeDescET
        val recipeTypeV: TextInputLayout = binding.updateRecipeTypeV
        val recipeTypeDDL: AutoCompleteTextView = binding.updateRecipeTypeET
        val recipeIngredientV: TextInputLayout = binding.updateAddIngredientV
        val recipeIngredientET: TextInputEditText = binding.updateAddIngredientET
        val recipeStepV: TextInputLayout = binding.updateAddStepV
        val recipeStepET: TextInputEditText = binding.updateAddStepET
        val updateBtn: Button = binding.updateRecipeBtn

        // Get data for db
        firebaseStorage.child("Recipe/$recipeID.jpg").downloadUrl.addOnSuccessListener { uri ->
            Glide.with(requireContext()).load(uri).into(binding.updateRecipeImg)
        }.addOnFailureListener {
            binding.updateRecipeImg.setImageResource(R.drawable.ic_baseline_image_24)
        }

        db.collection("Recipe").document(recipeID).get().addOnCompleteListener{
            if(it.isSuccessful){
                val doc = it.result
                if(doc.exists()){
                    val rName: String = doc.getString("name").toString()
                    recipeNameET.setText(rName)

                    val rDesc: String = doc.getString("description").toString()
                    recipeDescET.setText(rDesc)

                    val rType: String = doc.getString("type").toString()
                    recipeTypeDDL.setText(rType)

                    val rIngredient: String = doc.getString("ingredients").toString()
                    recipeIngredientET.setText(rIngredient)

                    val rStep: String = doc.getString("steps").toString()
                    recipeStepET.setText(rStep)

                    val recipeTypeSpinner = ArrayAdapter.createFromResource(requireContext(), R.array.types, R.layout.drop_down_list)
                    recipeTypeSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    recipeTypeDDL.setAdapter<ArrayAdapter<CharSequence>>(recipeTypeSpinner)
                }
            }
        }

        // Update detail
        uploadBtn.setOnClickListener {
            requestPermission()
            if(isReadPermissionGranted == true && isCameraPermissionGranted == true){
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Upload Photo")
                builder.setItems(R.array.upload_options, DialogInterface.OnClickListener{ _, which ->
                    when(which){
                        0 -> {
                            // Camera
                            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            photoFile = getPhotoFile("tempImg")

                            val fileProvider = FileProvider.getUriForFile(requireContext(), "com.example.myriccipii.fileprovider", photoFile)
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

                            if(takePictureIntent.resolveActivity(requireContext().packageManager) != null){
                                startActivityForResult(takePictureIntent, 42)
                            }else{
                                Toast.makeText(context, "Unable to open camera", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            // Gallery
                            selectImage()
                        }
                    }
                })
                val alert = builder.create()
                alert.show()
            }
        }

        recipeNameET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val nameView = charSequence.toString()
                if (TextUtils.isEmpty(nameView.trim { it <= ' ' })) {
                    recipeNameV.error = "Enter a name for your new recipe."
                    bool = false
                } else if(nameView.length < 5){
                    recipeNameV.error = "The recipe name must be at least 5 characters long."
                    bool = false
                } else {
                    recipeNameV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        recipeDescET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val descView = charSequence.toString()
                if (TextUtils.isEmpty(descView.trim { it <= ' ' })) {
                    recipeDescV.error = "Enter a description for your new recipe."
                    bool = false
                } else if(descView.length < 5){
                    recipeDescV.error = "The recipe description must be at least 5 characters long."
                    bool = false
                } else {
                    recipeDescV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        recipeDescET.setOnTouchListener(View.OnTouchListener { v, event ->
            if (v.id == R.id.update_recipeDescET) {
                v.parent.requestDisallowInterceptTouchEvent(true)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        })

        recipeTypeDDL.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val typeView = charSequence.toString()
                if (TextUtils.isEmpty(typeView.trim { it <= ' ' })) {
                    recipeTypeV.error = "Select a recipe type for your new recipe."
                    bool = false
                }else {
                    recipeTypeV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        recipeIngredientET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val ingredientView = charSequence.toString()
                if (TextUtils.isEmpty(ingredientView.trim { it <= ' ' })) {
                    recipeIngredientV.error = "Enter the ingredients for your new recipe."
                    bool = false
                } else if(ingredientView.length < 10){
                    recipeIngredientV.error = "The ingredients name must be at least 10 characters long."
                    bool = false
                } else {
                    recipeIngredientV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        recipeIngredientET.setOnTouchListener(View.OnTouchListener { v, event ->
            if (v.id == R.id.update_addIngredientET) {
                v.parent.requestDisallowInterceptTouchEvent(true)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        })

        recipeStepET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val stepsView = charSequence.toString()
                if (TextUtils.isEmpty(stepsView.trim { it <= ' ' })) {
                    recipeStepV.error = "Enter the steps on how to make your recipe."
                    bool = false
                } else if(stepsView.length < 10){
                    recipeStepV.error = "The steps description must be at least 10 characters long."
                    bool = false
                } else {
                    recipeStepV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        recipeStepET.setOnTouchListener(View.OnTouchListener { v, event ->
            if (v.id == R.id.update_addStepET) {
                v.parent.requestDisallowInterceptTouchEvent(true)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        })

        updateBtn.setOnClickListener{
            recipeNameV.error = ""
            recipeDescV.error = ""
            recipeTypeV.error = ""
            recipeIngredientV.error = ""
            recipeStepV.error = ""

            val name:String = recipeNameET.text.toString()
            val desc:String = recipeDescET.text.toString()
            val type:String = recipeTypeDDL.text.toString()
            val ingredient:String = recipeIngredientET.text.toString()
            val step:String = recipeStepET.text.toString()

            if (TextUtils.isEmpty(name.trim { it <= ' ' })) {
                recipeNameV.error = "Enter a name for your new recipe."
                bool = false
            }else if(name.length < 5){
                recipeNameV.error = "The recipe name must be at least 5 characters long."
                bool = false
            }

            if (TextUtils.isEmpty(desc.trim { it <= ' ' })) {
                recipeDescV.error = "Enter a description for your new recipe."
                bool = false
            }else if(desc.length < 5){
                recipeDescV.error = "The recipe description must be at least 5 characters long."
                bool = false
            }

            if (TextUtils.isEmpty(type.trim { it <= ' ' })) {
                recipeTypeV.error = "Select a recipe type for your new recipe."
                bool = false
            }

            if (TextUtils.isEmpty(ingredient.trim { it <= ' ' })) {
                recipeIngredientV.error = "Enter the ingredients for your new recipe."
                bool = false
            } else if(ingredient.length < 10){
                recipeIngredientV.error = "The ingredients name must be at least 10 characters long."
                bool = false
            }

            if (TextUtils.isEmpty(step.trim { it <= ' ' })) {
                recipeStepV.error = "Enter the steps on how to make your recipe."
                bool = false
            } else if(step.length < 10){
                recipeStepV.error = "The steps description must be at least 10 characters long."
                bool = false
            }

            if(bool){
                val updateDetail = mapOf<String, Any>(
                    "name" to name,
                    "description" to desc,
                    "type" to type,
                    "ingredients" to ingredient,
                    "steps" to step
                )

                db.collection("Recipe").document(recipeID).update(updateDetail).addOnSuccessListener{
                    if(::imageUri.isInitialized){
                        uploadImage(recipeID)
                    }

                    Toast.makeText(context, "Your recipe has been successfully updated", Toast.LENGTH_SHORT).show()

                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            replaceFragment(profileFragment)
                        }
                    }, 1000)
                }.addOnFailureListener{
                    Toast.makeText(context, "Unable to update, please try later", Toast.LENGTH_LONG).show()
                }
            }
        }

        return binding.root
    }

    private fun getPhotoFile(s: String): File {
        val storageDirectory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(s, ".jpg", storageDirectory)
    }

    private fun selectImage(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 100)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 100 && resultCode == Activity.RESULT_OK){
            imageUri = data?.data!!
            binding.updateRecipeImg.setImageURI(imageUri)
            binding.validateNullImage.visibility = View.GONE
        }else if(requestCode == 42 && resultCode == Activity.RESULT_OK){
            imageUri = Uri.fromFile(photoFile.absoluteFile)
            binding.updateRecipeImg.setImageURI(imageUri)
            binding.validateNullImage.visibility = View.GONE
        }else{
            binding.validateNullImage.text = "You need to upload a photo."
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun uploadImage(id: String){
        val filename = "${id}.jpg"
        val storageReference = FirebaseStorage.getInstance().getReference("Recipe/$filename")

        storageReference.putFile(imageUri).addOnSuccessListener {
            binding.updateRecipeImg.setImageURI(null)
        }.addOnFailureListener{
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceFragment(fragment: Fragment?) {
        if (fragment != null) {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            ft.replace(R.id.containerForRecipe, fragment)
            ft.commit()
        }
    }

    private fun requestPermission(){
        isReadPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val permissionRequest : MutableList<String> = ArrayList()

        if(!isReadPermissionGranted){
            permissionRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if(!isCameraPermissionGranted){
            permissionRequest.add(android.Manifest.permission.CAMERA)
        }

        if(permissionRequest.isNotEmpty()){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }
}