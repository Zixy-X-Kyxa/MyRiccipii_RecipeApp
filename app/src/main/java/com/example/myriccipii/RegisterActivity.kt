package com.example.myriccipii

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.myriccipii.databinding.ActivityLoginBinding
import com.example.myriccipii.databinding.ActivityRegisterBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.firestore.FirebaseFirestore
import java.util.HashMap
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityRegisterBinding
    private var bool = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val action = supportActionBar
        action?.hide()

        val back:ImageView = binding.regBackBtn
        val userV:TextInputLayout = binding.RegUsernameV
        val userET:TextInputEditText = binding.RegUsernameET
        val emailV:TextInputLayout = binding.RegEmailV
        val emailET:TextInputEditText = binding.RegEmailET
        val pwV:TextInputLayout = binding.RegPasswordV
        val pwET:TextInputEditText = binding.RegPasswordET
        val registerBtn:Button = binding.registerBtn

        // Back Button
        back.setOnClickListener(View.OnClickListener {
            val i = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
            finish()
        })

        // Error for Name
        userET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val nameView = charSequence.toString()
                if (TextUtils.isEmpty(nameView.trim { it <= ' ' })) {
                    userV.error = "Required field."
                    bool = false
                } else {
                    userV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })


        // Error for Email
        emailET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val emailView = charSequence.toString()
                if (TextUtils.isEmpty(emailView.trim { it <= ' ' })) {
                    emailV.error = "Required field."
                    bool = false
                } else if (!isValidEmail(emailView.trim { it <= ' ' })!!) {
                    emailV.error = "Invalid email format, please try again."
                    bool = false
                } else {
                    emailV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })


        // Error for Password
        pwET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val pwView = charSequence.toString()
                if (TextUtils.isEmpty(pwView.trim { it <= ' ' })) {
                    pwV.error = "Required field."
                    bool = false
                } else if (!isValidPW(pwView.trim { it <= ' ' })!!) {
                    pwV.error = "The password must be at least 8 characters long and with numbers."
                    bool = false
                } else {
                    pwV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        registerBtn.setOnClickListener {
            userV.error = ""
            emailV.error = ""
            pwV.error = ""

            val name:String = userET.text.toString()
            val email:String = emailET.text.toString()
            val pw:String = pwET.text.toString()

            // Error for Name when click btn
            if (TextUtils.isEmpty(name.trim { it <= ' ' })) {
                userV.error = "Required field."
                bool = false
            }

            // Error for Email when click btn
            if (TextUtils.isEmpty(email.trim { it <= ' ' })) {
                emailV.error = "Required field."
                bool = false
            } else if (!isValidEmail(email.trim { it <= ' ' })!!) {
                emailV.error = "Invalid email format, please try again."
                bool = false
            }

            // Error for Password when click btn
            if (TextUtils.isEmpty(pw.trim { it <= ' ' })) {
                pwV.error = "Required field."
                bool = false
            } else if (!isValidPW(pw.trim { it <= ' ' })!!) {
                pwV.error = "The password must be at least 8 characters long and with numbers."
                bool = false
            }

            if(bool){
                auth.createUserWithEmailAndPassword(email, pw).addOnSuccessListener(this) { authResult ->
                    val firebaseUser = authResult.user

                    // reg user to db
                    val userDetail = mapOf<String, Any>(
                        "uid" to firebaseUser!!.uid,
                        "username" to name,
                        "email" to email
                    )

                    db.collection("Users").document(firebaseUser.uid).set(userDetail)
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("uid", firebaseUser.uid)
                    intent.putExtra("username", name)
                    intent.putExtra("email", email)

                    // send verification email
                    firebaseUser.sendEmailVerification().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@RegisterActivity,
                                "Registered Successfully. An email verification link has been sent, please check your email.",
                                Toast.LENGTH_LONG).show()
                            startActivity(intent)
                            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
                            finish()
                        }
                    }

                    // if no put, once register will directly jump to login(homepage)
                    auth.signOut()
                }
                // check email exist in firebase
                auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                    val isNewUser = task.result.signInMethods!!.isEmpty()
                    if (isNewUser) {
                        emailV.error = ""
                    } else {
                        // if email exist in db prompt error
                        emailV.error = "This email has been used, please try again."
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val i = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(i)
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
        finish()
    }

    // Validate email regex
    private fun isValidEmail(email: CharSequence): Boolean? {
        var isValid = true
        val pattern = Pattern.compile("^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(email)
        if (!matcher.matches()) {
            isValid = false
        }
        return isValid
    }

    // Validate password regex
    private fun isValidPW(pw: CharSequence): Boolean? {
        var isValid = true
        val pattern =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(pw)
        if (!matcher.matches()) {
            isValid = false
        }
        return isValid
    }
}