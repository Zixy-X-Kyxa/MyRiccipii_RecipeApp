package com.example.myriccipii

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myriccipii.Recipes.*
import com.example.myriccipii.databinding.ActivityRecipeListBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import org.checkerframework.checker.units.qual.s

class RecipeListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeListBinding
    private val homeFragment: HomepageFragment = HomepageFragment()
    private val profileFragment: ProfileFragment = ProfileFragment()
    private val addMainFragment: AddMainFragment = AddMainFragment()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val btmNav: BottomNavigationView = binding.bottomNavigationView
        val fab: FloatingActionButton = binding.fab
        val action = supportActionBar
        action?.hide()

        btmNav.background = null
        btmNav.menu.getItem(1).isEnabled = false //placeholder
        replaceFragment(homeFragment)

        fab.setOnClickListener {
            replaceFragment(addMainFragment)
        }

        btmNav.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home -> replaceFragment(homeFragment)
                R.id.navigation_profile -> replaceFragment(profileFragment)
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment?) {
        if (fragment != null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.containerForRecipe, fragment)
            ft.commit()
        }
    }

    override fun onBackPressed(){
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Sign Out")
//        builder.setMessage("Are you sure you want to sign out?")
//        builder.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.cancel() }
//        builder.setPositiveButton("Sign Out") { dialogInterface, _ ->
//            dialogInterface.cancel()
//            auth.signOut()
//            val i = Intent(this@RecipeListActivity, LoginActivity::class.java)
//            startActivity(i)
//            finish()
////            super.onBackPressed()
//        }
//        val alertDialog = builder.create()
//        alertDialog.show()
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