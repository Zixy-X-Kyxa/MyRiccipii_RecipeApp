package com.example.myriccipii

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Toast
import com.example.myriccipii.databinding.ActivityForgetPasswordBinding
import com.example.myriccipii.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class ForgetPasswordActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityForgetPasswordBinding
    private var bool = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val action = supportActionBar
        action?.hide()

        // Back to Login
        binding.rpwBackBtn.setOnClickListener{
            val i = Intent(this@ForgetPasswordActivity, LoginActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
            finish()
        }

        binding.recoverPasswordEmailET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val recView = charSequence.toString()
                if (TextUtils.isEmpty(recView.trim { it <= ' ' })) {
                    binding.recoverPasswordEmailV.error = "Required field."
                    bool = false
                } else if (!isValidEmail(recView.trim { it <= ' ' })!!) {
                    binding.recoverPasswordEmailV.error = "Invalid email format, please try again."
                    bool = false
                } else {
                    binding.recoverPasswordEmailV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.recoverPasswordBtn.setOnClickListener{
            binding.recoverPasswordEmailV.error = ""
            val recoverPassword:String = binding.recoverPasswordEmailET.text.toString()

            if (TextUtils.isEmpty(recoverPassword.trim { it <= ' ' })) {
                binding.recoverPasswordEmailV.error = "Required field."
                bool = false
            } else if (!isValidEmail(recoverPassword.trim { it <= ' ' })!!) {
                binding.recoverPasswordEmailV.error = "Invalid email format, please try again."
                bool = false
            }

            if (bool) {
                // send password reset link to user email
                auth.sendPasswordResetEmail(binding.recoverPasswordEmailET.text.toString()).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // valid email - send
                            Toast.makeText(this@ForgetPasswordActivity,
                                "A password reset link has been sent, please check your email!",
                                Toast.LENGTH_LONG).show()
                            val i = Intent(this@ForgetPasswordActivity, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(i)
                            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
                            finish()
                        } else {
                            // invalid email - prompt error
                            binding.recoverPasswordEmailV.error = "This email does not exist, please try again!"
                        }
                    }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val i = Intent(this@ForgetPasswordActivity, LoginActivity::class.java)
        startActivity(i)
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
        finish()
    }

    private fun isValidEmail(email: CharSequence): Boolean? {
        var isValid = true
        val pattern = Pattern.compile("^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(email)
        if (!matcher.matches()) {
            isValid = false
        }
        return isValid
    }

}