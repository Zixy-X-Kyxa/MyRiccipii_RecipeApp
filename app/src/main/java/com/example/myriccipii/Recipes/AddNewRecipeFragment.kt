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
import android.text.method.Touch
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import com.example.myriccipii.LoginActivity
import com.example.myriccipii.R
import com.example.myriccipii.databinding.ActivityLoginBinding
import com.example.myriccipii.databinding.FragmentAddNewRecipeBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.security.AccessController.checkPermission
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList

class AddNewRecipeFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: FragmentAddNewRecipeBinding
    private var bool = true
    private lateinit var imageUri : Uri
    private lateinit var photoFile: File
    private val homeFragment: HomepageFragment = HomepageFragment()
    private var isReadPermissionGranted = false
    private var isCameraPermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("QueryPermissionsNeeded", "SetTextI18n", "InflateParams",
        "ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAddNewRecipeBinding.inflate(inflater, container, false)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            isReadPermissionGranted = it[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            isCameraPermissionGranted = it[android.Manifest.permission.CAMERA] ?: isCameraPermissionGranted
        }

        val newRecipeImg: ImageView = binding.newRecipeImg
        val validateNullImage: TextView = binding.validateNullImage
        val recipeNameV: TextInputLayout = binding.recipeNameV
        val recipeNameET: TextInputEditText = binding.recipeNameET
        val recipeDescV: TextInputLayout = binding.recipeDescV
        val recipeDescET: TextInputEditText = binding.recipeDescET
        val recipeTypeV: TextInputLayout = binding.recipeTypeV
        val recipeTypeDDL: AutoCompleteTextView = binding.recipeTypeET
        val addIngredientV: TextInputLayout = binding.addIngredientV
        val addIngredientET: TextInputEditText = binding.addIngredientET
        val addStepV: TextInputLayout = binding.addStepV
        val addStepET: TextInputEditText = binding.addStepET
//        val addIngredientBtn: ImageView = binding.addIngredientBtn
//        val addStepsBtn: ImageView = binding.addStepsBtn
        val submit: Button = binding.addNewRecipeBtn
        val uploadBtn: Button = binding.uploadBtn
//        val addIngredientViewList: LinearLayout = binding.addIngredientLL
//        val addStepsViewList: LinearLayout = binding.addStepsLL

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

        recipeDescET.setOnTouchListener(OnTouchListener { v, event ->
            if(v.id == R.id.recipeDescET){
                v.parent.requestDisallowInterceptTouchEvent(true)

                when(event.action){
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

        addIngredientET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val ingredientView = charSequence.toString()
                if (TextUtils.isEmpty(ingredientView.trim { it <= ' ' })) {
                    addIngredientV.error = "Enter the ingredients for your new recipe."
                    bool = false
                } else if(ingredientView.length < 10){
                    addIngredientV.error = "The ingredients name must be at least 10 characters long."
                    bool = false
                } else {
                    addIngredientV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        addIngredientET.setOnTouchListener(OnTouchListener { v, event ->
            if(v.id == R.id.addIngredientET){
                v.parent.requestDisallowInterceptTouchEvent(true)

                when(event.action){
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        })

        addStepET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val stepsView = charSequence.toString()
                if (TextUtils.isEmpty(stepsView.trim { it <= ' ' })) {
                    addStepV.error = "Enter the steps on how to make your recipe."
                    bool = false
                } else if(stepsView.length < 10){
                    addStepV.error = "The steps description must be at least 10 characters long."
                    bool = false
                } else {
                    addStepV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        addStepET.setOnTouchListener(OnTouchListener { v, event ->
            if(v.id == R.id.addStepET){
                v.parent.requestDisallowInterceptTouchEvent(true)

                when(event.action){
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        })

        val recipeTypeSpinner = ArrayAdapter.createFromResource(requireContext(), R.array.types, R.layout.drop_down_list)
        recipeTypeSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recipeTypeDDL.setAdapter<ArrayAdapter<CharSequence>>(recipeTypeSpinner)

//        addIngredientBtn.setOnClickListener{
//            val ingredientView: View = layoutInflater.inflate(R.layout.new_ingredient_row,null,false)
//            val newRowI: EditText = ingredientView.findViewById(R.id.addIngredientRow)
//            val delRowI: ImageView = ingredientView.findViewById(R.id.delIngredientBtn)
//
//              //error
//            if (TextUtils.isEmpty(newRowI.text.trim { it <= ' ' })) {
//                newRowI.error = "Required"
//                bool = false
//            }else {
//                newRowI.error = ""
//                bool = true
//            }
//
//            delRowI.setOnClickListener{
//                addIngredientViewList.removeView(ingredientView)
//            }
//            binding.ingredientInfo.visibility = View.GONE
//            bool = true
//            addIngredientViewList.addView(ingredientView)
//        }
//
//        addStepsBtn.setOnClickListener{
//            val stepView: View = layoutInflater.inflate(R.layout.new_steps_row,null,false)
//            val newRowS: EditText = stepView.findViewById(R.id.addStepRow)
//            val delRowS: ImageView = stepView.findViewById(R.id.delStepBtn)
//
//            delRowS.setOnClickListener{
//                addStepsViewList.removeView(stepView)
//            }
//            binding.stepsInfo.visibility = View.GONE
//            bool = true
//            addStepsViewList.addView(stepView)
//        }

        submit.setOnClickListener{
            recipeNameV.error = ""
            recipeDescV.error = ""
            recipeTypeV.error = ""
            addIngredientV.error = ""
            addStepV.error = ""

            val name:String = recipeNameET.text.toString()
            val desc:String = recipeDescET.text.toString()
            val type:String = recipeTypeDDL.text.toString()
            val ingredient:String = addIngredientET.text.toString()
            val step:String = addStepET.text.toString()

            // not working
//            if(newRecipeImg.drawable == null){
//                validateNullImage.text = "You need to upload a photo."
//            }else{
//                validateNullImage.visibility = View.GONE
//            }

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
                addIngredientV.error = "Enter the ingredients for your new recipe."
                bool = false
            } else if(ingredient.length < 10){
                addIngredientV.error = "The ingredients name must be at least 10 characters long."
                bool = false
            }

            if (TextUtils.isEmpty(step.trim { it <= ' ' })) {
                addStepV.error = "Enter the steps on how to make your recipe."
                bool = false
            } else if(step.length < 10){
                addStepV.error = "The steps description must be at least 10 characters long."
                bool = false
            }

//            if(addIngredientViewList.childCount == 0){
//                binding.ingredientInfo.visibility = View.VISIBLE
//                bool = false
//            }
//
//            if(addStepsViewList.childCount == 0){
//                binding.stepsInfo.visibility = View.VISIBLE
//                bool = false
//            }

            if(bool){
                val ref = db.collection("Recipe").document()
                val firebaseUser = auth.currentUser!!.uid

                if(::imageUri.isInitialized){
                    uploadImage(ref.id)
                }

                db.collection("Users").document(firebaseUser).get().addOnSuccessListener{
                    if(it.exists()){
                        val n: String = it.getString("username").toString()

                        val recipesDetail = mapOf<String, Any>(
                            "name" to name,
                            "description" to desc,
                            "type" to type,
                            "ingredients" to ingredient,
                            "steps" to step,
                            "uid" to firebaseUser,
                            "username" to n
                        )
                        ref.set(recipesDetail)
                    }
                }
                Toast.makeText(context, "Successfully added a new recipe", Toast.LENGTH_LONG).show()
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        replaceFragment(homeFragment)
                    }
                }, 1000)

            }
        }

//        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if(isEnabled){
//                    isEnabled = false
//                    activity?.onBackPressed()
//                }
//            }
//        })

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
            binding.newRecipeImg.setImageURI(imageUri)
            binding.validateNullImage.visibility = View.GONE
        }else if(requestCode == 42 && resultCode == Activity.RESULT_OK){
            imageUri = Uri.fromFile(photoFile.absoluteFile)
            binding.newRecipeImg.setImageURI(imageUri)
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
            binding.newRecipeImg.setImageURI(null)
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