package com.example.myriccipii

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import com.example.myriccipii.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityLoginBinding
    private var bool = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val action = supportActionBar
        action?.hide()

        val emailLoginV:TextInputLayout = binding.EmailLoginV
        val passwordLoginV:TextInputLayout = binding.PasswordLoginV
        val emailLoginET:TextInputEditText = binding.EmailLoginET
        val passwordLoginET:TextInputEditText = binding.PasswordLoginET
        val loginBtn:Button = binding.loginBtn
        val register:TextView = binding.Register
        val forgetPw:TextView = binding.ForgetPw

        // Sign Up
        register.setOnClickListener {
            val i = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            finish()
        }

        // Forget Password
        forgetPw.setOnClickListener {
            val i = Intent(this@LoginActivity, ForgetPasswordActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            finish()
        }

        // Email validation
        emailLoginET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val emailField = charSequence.toString()
                if (TextUtils.isEmpty(emailField.trim { it <= ' ' })) {
                    emailLoginV.error = "Required field."
                    bool = false
                } else {
                    emailLoginV.error = ""
                    binding.BothFieldError.visibility = View.GONE
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        // Password validation
        passwordLoginET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val pw = charSequence.toString()
                if (TextUtils.isEmpty(pw.trim { it <= ' ' })) {
                    passwordLoginV.error = "Required field."
                    bool = false
                } else {
                    passwordLoginV.error = ""
                    binding.BothFieldError.visibility = View.GONE
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        loginBtn.setOnClickListener {
            val emailLogin: String = emailLoginET.text.toString()
            val pwLogin: String = passwordLoginET.text.toString()
            if (TextUtils.isEmpty(emailLogin.trim { it <= ' ' })) {
                emailLoginV.error = "Required field."
                bool = false
            }
            if (TextUtils.isEmpty(pwLogin.trim { it <= ' ' })) {
                passwordLoginV.error = "Required field."
                bool = false
            }
            if (bool) {
                auth.signInWithEmailAndPassword(emailLogin.trim { it <= ' ' }, pwLogin.trim { it <= ' ' }).addOnCompleteListener { task ->
                        val user = auth.currentUser
                        if (task.isSuccessful) {
                            updateUi(user)
                        } else {
                            updateUi(null)
                        }
                    }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            // Verified email
            if (currentUser.isEmailVerified) {
                checkUserAccessLevel(currentUser)
                binding.BothFieldError.visibility = View.GONE
                binding.EmailLoginV.error = ""
                binding.PasswordLoginV.error = ""
            } else {
                // Haven't verify email
                binding.EmailLoginV.error = "This email has not been verified!"
            }
        } else {
            // If firebase dun hav the input email and password = error msg
            binding.BothFieldError.text = "Invalid email or password, please try again!"
            binding.BothFieldError.visibility = View.VISIBLE
        }
    }

    private fun checkUserAccessLevel(uid: FirebaseUser) {
        // since there is no admin then no do to login to admin
        val userLogin = Intent(this@LoginActivity, RecipeListActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        db.collection("Users").document(uid.uid).get().addOnSuccessListener {
            Toast.makeText(this@LoginActivity, "Login Successfully!", Toast.LENGTH_SHORT).show()
            startActivity(userLogin)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        // null = login, not null = recipe list (home)
        if(user!=null){
            startActivity(Intent(this@LoginActivity, RecipeListActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit")
        builder.setMessage("Are you sure you want to exit the application?")
        builder.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.cancel() }
        builder.setPositiveButton("Exit") { dialogInterface, _ ->
            dialogInterface.cancel()
            finish()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}