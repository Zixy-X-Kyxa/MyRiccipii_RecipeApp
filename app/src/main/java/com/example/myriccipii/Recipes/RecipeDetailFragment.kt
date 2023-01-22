package com.example.myriccipii.Recipes

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myriccipii.Class.Recipe
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentHomepageBinding
import com.example.myriccipii.databinding.FragmentRecipeDetailBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.HashMap

class RecipeDetailFragment : Fragment() {

    private lateinit var binding: FragmentRecipeDetailBinding
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        val data = arguments
        var recipeID = data!!.get("recipeID").toString()
        val firebaseStorage = FirebaseStorage.getInstance().reference
        val fm = requireActivity().supportFragmentManager.beginTransaction()

        val typeView: TextView = binding.headerDetailView
        val recipeDetailImg: ImageView = binding.recipeDetailImg
        val viewName: TextView = binding.viewName
        val viewDesc: TextView = binding.viewDesc
        val ingredientDetails: TextView = binding.ingredientDetails
        val stepsDetails: TextView = binding.stepsDetails
        val madeByWhoTV: TextView = binding.madeByWhoTV

        binding.detailBackBtn.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            fm.replace(R.id.containerForRecipe, HomepageFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        db.collection("Recipe").document(recipeID).get().addOnCompleteListener{
            if(it.isSuccessful){
                val doc = it.result
                if(doc.exists()){
                    firebaseStorage.child("Recipe/$recipeID.jpg").downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(requireContext()).load(uri).into(recipeDetailImg)
                    }.addOnFailureListener { // if book id NOT FOUND in storage
                        recipeDetailImg.setImageResource(R.drawable.ic_baseline_image_24)
                    }

                    typeView.text = doc.getString("type").toString() + " Recipes"
                    viewName.text = doc.getString("name").toString()
                    viewDesc.text = doc.getString("description").toString()
                    ingredientDetails.text = doc.getString("ingredients").toString()
                    stepsDetails.text = doc.getString("steps").toString()
                    madeByWhoTV.text = doc.getString("username").toString()
                }
            }
        }

        return binding.root
    }
}