package com.example.myriccipii.Recipes

import android.annotation.SuppressLint
import android.app.Activity
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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.myriccipii.R
import com.example.myriccipii.databinding.ActivityLoginBinding
import com.example.myriccipii.databinding.FragmentEditProfileBinding
import com.example.myriccipii.databinding.FragmentSettingsBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.*


class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bool = true
    private lateinit var imageUri : Uri
    private lateinit var photoFile: File
    private var isReadPermissionGranted = false
    private var isCameraPermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        val fm = requireActivity().supportFragmentManager.beginTransaction()
        val currentUser = auth.currentUser
        val userID: String = currentUser!!.uid

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            isReadPermissionGranted = it[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            isCameraPermissionGranted = it[android.Manifest.permission.CAMERA] ?: isCameraPermissionGranted
        }

        val firebaseStorage = FirebaseStorage.getInstance().reference
        firebaseStorage.child("Users/$userID.jpg").downloadUrl.addOnSuccessListener { uri ->
            Glide.with(requireContext()).load(uri).into(binding.profileImageIcon)
        }.addOnFailureListener {
            binding.profileImageIcon.setImageResource(R.drawable.ic_account_nav_icon)
        }

        db.collection("Users").whereEqualTo("uid", userID).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val doc = task.result
                if (!doc.isEmpty) {
                    for (d in doc) {
                        val email = d.getString("email").toString()
                        binding.fixEmailTV.text = email

                        val username = d.getString("username").toString()
                        binding.editUsernameET.setText(username)
                    }
                }
            }
        }

        binding.uploadProfilePicBtn.setOnClickListener {
            requestPermission()
            if(isReadPermissionGranted == true && isCameraPermissionGranted == true){
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Upload Profile Image")
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

        binding.editUsernameET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val nameView = charSequence.toString()
                if (TextUtils.isEmpty(nameView.trim { it <= ' ' })) {
                    binding.editUsernameV.error = "Required field."
                    bool = false
                } else {
                    binding.editUsernameV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.backBtn.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            fm.replace(R.id.containerForRecipe, SettingsFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        binding.changePasswordBtn.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            fm.replace(R.id.containerForRecipe, ChangePasswordFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        binding.updateProfileBtn.setOnClickListener{
            binding.editUsernameV.error = ""

            val uName:String = binding.editUsernameET.text.toString()

            if (TextUtils.isEmpty(uName.trim { it <= ' ' })) {
                binding.editUsernameV.error = "Required field."
                bool = false
            }

            if(bool){
                val updateName = mapOf<String, Any>(
                    "username" to uName
                )

                db.collection("Users").document(userID).update(updateName).addOnSuccessListener{
                    if(::imageUri.isInitialized){
                        uploadImage(userID)
                    }

                    db.collection("Recipe").whereEqualTo("uid", userID).get().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                for (d in task.result) {
                                    db.collection("Recipe").document(d.id).update(updateName)
                                }
                            }
                        }

                    Toast.makeText(context, "Your profile has been successfully updated!", Toast.LENGTH_LONG).show()

                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                            fm.replace(R.id.containerForRecipe, ProfileFragment())
                            fm.addToBackStack(null)
                            fm.commit()
                        }
                    }, 1000) // Delay 1 secs

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
            binding.profileImageIcon.setImageURI(imageUri)
        }else if(requestCode == 42 && resultCode == Activity.RESULT_OK){
            imageUri = Uri.fromFile(photoFile.absoluteFile)
            binding.profileImageIcon.setImageURI(imageUri)
        }else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun uploadImage(id: String){
        val filename = "${id}.jpg"
        val storageReference = FirebaseStorage.getInstance().getReference("Users/$filename")

        storageReference.putFile(imageUri).addOnSuccessListener {
            binding.profileImageIcon.setImageURI(null)
        }.addOnFailureListener{
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
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