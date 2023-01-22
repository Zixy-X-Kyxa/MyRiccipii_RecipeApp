package com.example.myriccipii.Recipes

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myriccipii.LoginActivity
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentChangePasswordBinding
import com.example.myriccipii.databinding.FragmentEditProfileBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern


class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bool = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        val fm = requireActivity().supportFragmentManager.beginTransaction()
        val currentUser = auth.currentUser

        binding.oldPasswordET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val oldView = charSequence.toString()
                if (TextUtils.isEmpty(oldView.trim { it <= ' ' })) {
                    binding.oldPasswordV.error = "Required field."
                    bool = false
                } else {
                    binding.oldPasswordV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.newPasswordET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val newView = charSequence.toString()
                if (TextUtils.isEmpty(newView.trim { it <= ' ' })) {
                    binding.newPasswordV.error = "Required field."
                    bool = false
                } else if (!isValidPW(newView.trim { it <= ' ' })!!) {
                    binding.newPasswordV.error = "The password must be at least 8 characters long and with numbers."
                    bool = false
                } else {
                    binding.newPasswordV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.confirmPasswordET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val confirmView = charSequence.toString()
                if (TextUtils.isEmpty(confirmView.trim { it <= ' ' })) {
                    binding.confirmPasswordV.error = "Required field."
                    bool = false
                }else if (confirmView != binding.newPasswordET.text.toString()) {
                    binding.confirmPasswordV.error = "Confirm password does not match new password."
                    bool = false
                } else {
                    binding.confirmPasswordV.error = ""
                    bool = true
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        binding.changePasswordBtn.setOnClickListener{
            binding.oldPasswordV.error = ""
            binding.newPasswordV.error = ""
            binding.confirmPasswordV.error = ""

            val old:String = binding.oldPasswordET.text.toString()
            val new:String = binding.newPasswordET.text.toString()
            val confirm:String = binding.confirmPasswordET.text.toString()

            if (TextUtils.isEmpty(old.trim { it <= ' ' })) {
                binding.oldPasswordV.error = "Required field."
                bool = false
            }

            if (TextUtils.isEmpty(new.trim { it <= ' ' })) {
                binding.newPasswordV.error = "Required field."
                bool = false
            } else if (!isValidPW(new.trim { it <= ' ' })!!) {
                binding.newPasswordV.error = "The password must be at least 8 characters long and with numbers."
                bool = false
            }

            if (TextUtils.isEmpty(confirm.trim { it <= ' ' })) {
                binding.confirmPasswordV.error = "Required field."
                bool = false
            } else if (confirm != new) {
                binding.confirmPasswordV.error = "Confirm password does not match new password."
                bool = false
            }

            if (bool) {
                val credential = EmailAuthProvider.getCredential(currentUser?.email.toString(), old)

                currentUser?.reauthenticate(credential)?.addOnCompleteListener{ task ->
                    if(task.isSuccessful){
                        // Authenticate success
                        currentUser.updatePassword(new).addOnCompleteListener{
                            // Update Password if user authenticate success (is it login-able?)
                            if(it.isSuccessful){
                                auth.signOut()
                                val i = Intent(activity,LoginActivity::class.java)
                                startActivity(i)
                                Toast.makeText(context,
                                    "Password changed successfully. You are now required to re-login again.",
                                    Toast.LENGTH_LONG).show()
                                activity?.finish()
                            }
                        }
                    }else{
                        // Authenticate failure
                        binding.oldPasswordV.error = "Invalid current password, please try again."
                    }
                }
            }
        }

        binding.backToProfileBtn.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            fm.replace(R.id.containerForRecipe, EditProfileFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        return binding.root
    }

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