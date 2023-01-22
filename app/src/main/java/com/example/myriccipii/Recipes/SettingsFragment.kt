package com.example.myriccipii.Recipes

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.myriccipii.LoginActivity
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentHomepageBinding
import com.example.myriccipii.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth


class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val fm = requireActivity().supportFragmentManager.beginTransaction()

        binding.cvBack.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            fm.replace(R.id.containerForRecipe, ProfileFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        binding.cvEditProfile.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            fm.replace(R.id.containerForRecipe, EditProfileFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        binding.cvUserLogout.setOnClickListener{
            val builder = AlertDialog.Builder(this.context)
            builder.setTitle("Sign Out")
            builder.setMessage("Are you sure you want to sign out?")
            builder.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.cancel() }
            builder.setPositiveButton("Sign Out") { dialogInterface, _ ->
                dialogInterface.cancel()
                auth.signOut()
                val intent = Intent(activity, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(context, "Logout Successfully!", Toast.LENGTH_LONG).show()
                activity?.finish()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        return binding.root
    }


}