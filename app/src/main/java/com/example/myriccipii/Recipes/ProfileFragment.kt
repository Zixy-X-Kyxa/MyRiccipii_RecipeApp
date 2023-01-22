package com.example.myriccipii.Recipes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myriccipii.Class.Recipe
import com.example.myriccipii.LoginActivity
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentHomepageBinding
import com.example.myriccipii.databinding.FragmentProfileBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var recipeList: MutableList<Recipe>
    private lateinit var imageUri : Uri
    private lateinit var photoFile: File

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        recipeList = mutableListOf()
        val currentUser = auth.currentUser
        val userID: String = currentUser!!.uid
        val fm = requireActivity().supportFragmentManager.beginTransaction()

        // Profile Image
        val firebaseStorage = FirebaseStorage.getInstance().reference
        firebaseStorage.child("Users/$userID.jpg").downloadUrl.addOnSuccessListener { uri ->
            imageUri = uri
            Glide.with(requireContext()).load(uri).into(binding.profileImageIcon)
        }.addOnFailureListener {
            binding.profileImageIcon.setImageResource(R.drawable.ic_account_nav_icon)
            val packageName = requireContext().packageName
            imageUri = Uri.parse("android.resource://" + packageName + "/" + R.drawable.ic_account_nav_icon)
        }

        binding.profileSettingIv.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            fm.replace(R.id.containerForRecipe, SettingsFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        // My Profile
        db.collection("Users").whereEqualTo("uid", userID).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val doc = task.result
                if (!doc.isEmpty) {
                    for (d in doc) {
                        binding.profileNameTv.text = d.getString("username").toString()
                    }
                }
            }
        }

        // My Recipe
        db.collection("Recipe").whereEqualTo("uid",userID).get().addOnSuccessListener{
            recipeList.clear()
            for(doc in it){
                val recipe = Recipe()
                recipe.id = doc.id
                recipe.recipeName = doc.get("name").toString()
                recipe.recipeDescription = doc.get("description").toString()
                recipe.recipeTypes = doc.get("type").toString()
                recipe.ingredient = doc.get("ingredients").toString()
                recipe.step = doc.get("steps").toString()
                recipe.uid = doc.get("uid").toString()
                recipe.userName = doc.get("username").toString()
                recipeList.add(recipe)
            }

            if(recipeList.isEmpty()){
                binding.emptyImg.visibility = View.VISIBLE
                binding.myRecipeView.visibility = View.GONE
            }else if(recipeList.isNotEmpty()){
                binding.emptyImg.visibility = View.GONE
                binding.myRecipeView.visibility = View.VISIBLE
            }
            binding.myRecipeView.adapter?.notifyDataSetChanged()

            binding.myRecipeView.adapter = RecipeRecyclerAdapter(this, recipeList) { _: Recipe, position: Int ->
                val bundle = Bundle()
                bundle.putString("recipeID", recipeList[position].id)

                val listItem = arrayOf("Update My Recipe","Delete This Recipe","Cancel")
                val builder = AlertDialog.Builder(this.context)
                builder.setTitle("Choose an item.")

                builder.setItems(listItem, DialogInterface.OnClickListener { dialogInterface, i ->
                    if(listItem[i] == "Update My Recipe"){
                        val fragmentManager = activity?.supportFragmentManager?.beginTransaction()
                        val fragment = UpdateMyRecipeFragment()
                        fragment.arguments = bundle

                        fragmentManager?.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                        fragmentManager?.replace(R.id.containerForRecipe, fragment)
                        fragmentManager?.addToBackStack(null)
                        fragmentManager?.commit()
                        dialogInterface.cancel()
                    }else if (listItem[i] == "Delete This Recipe"){
                        dialogInterface.cancel()

                        db.collection("Recipe").document(recipeList[position].id).get().addOnCompleteListener{
                            if(it.isSuccessful){
                                val doc = it.result
                                if(doc.exists()){
                                    val recipeName: String = doc.getString("name").toString()

                                    val deleteDialog = AlertDialog.Builder(this.context)
                                    deleteDialog.setTitle("Warning!")
                                    deleteDialog.setMessage("Are you sure you want to delete [$recipeName] ?")
                                    deleteDialog.setNegativeButton("Cancel") { deleteInterface, _ -> deleteInterface.cancel() }
                                    deleteDialog.setPositiveButton("Delete") { deleteInterface, _ ->
                                        deleteInterface.cancel()
                                        db.collection("Recipe").document(recipeList[position].id).delete().addOnCompleteListener{
                                            deleteImg(recipeList[position].id)
                                            Toast.makeText(context, "Recipe Name: [$recipeName], has been deleted successfully!", Toast.LENGTH_LONG).show()
                                            recipeList.remove(recipeList[position])

                                            if(recipeList.isEmpty()){
                                                binding.emptyImg.visibility = View.VISIBLE
                                                binding.myRecipeView.visibility = View.GONE
                                            }else if(recipeList.isNotEmpty()){
                                                binding.emptyImg.visibility = View.GONE
                                                binding.myRecipeView.visibility = View.VISIBLE
                                            }
                                            binding.myRecipeView.adapter?.notifyDataSetChanged()
                                        }
                                    }
                                    val dialog = deleteDialog.create()
                                    dialog.show()
                                }
                            }
                        }
                    }else if (listItem[i] == "Cancel"){
                        dialogInterface.cancel()
                    }

                })

                val alertDialog = builder.create()
                alertDialog.show()
            }
        }
        return binding.root
    }

    fun deleteImg(id: String) {
        val storage = FirebaseStorage.getInstance().reference
        storage.child("Recipe/$id.jpg").delete().addOnSuccessListener {
            Log.e("Book Image [$id]: ", "#Deleted")
        }
    }

    class RecipeRecyclerAdapter(val context: ProfileFragment, private val items: List<Recipe>, val clickListener: (Recipe, Int) -> Unit) :
        RecyclerView.Adapter<RecipeRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.my_recipe_list, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.myRecipeName.text = item.recipeName
            holder.myRecipeType.text = item.recipeTypes
            holder.loadImg(item.id)

            holder.containerView.setOnClickListener { clickListener(item, position) }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView) {
            val myRecipeImg: ImageView = itemView.findViewById<ImageView>(R.id.my_recipe_img)
            val myRecipeName: TextView = itemView.findViewById<TextView>(R.id.my_recipe_name)
            val myRecipeType: TextView = itemView.findViewById<TextView>(R.id.my_recipe_type)

            fun loadImg(Id: String){
                val storageRef = FirebaseStorage.getInstance().reference.child("Recipe/${Id}.jpg")
                storageRef.downloadUrl.addOnSuccessListener (OnSuccessListener<Uri> {
                    Glide.with(context).load(it).into(myRecipeImg)
                }).addOnFailureListener{
                    myRecipeImg.setImageResource(R.drawable.ic_baseline_image_24)
                }
            }
        }

    }

}