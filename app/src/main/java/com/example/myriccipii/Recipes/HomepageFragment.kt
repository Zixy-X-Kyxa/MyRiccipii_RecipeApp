package com.example.myriccipii.Recipes

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myriccipii.Class.Recipe
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentAddNewRecipeBinding
import com.example.myriccipii.databinding.FragmentHomepageBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class HomepageFragment : Fragment() {
    private lateinit var binding: FragmentHomepageBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recipeList: MutableList<Recipe>

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        binding = FragmentHomepageBinding.inflate(inflater, container, false)
        recipeList = mutableListOf()

        db.collection("Recipe").get().addOnSuccessListener{
            //recipeList.clear()
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
                binding.emptyLL.visibility = View.VISIBLE
                binding.typeList.visibility = View.GONE
            }else if(recipeList.isNotEmpty()){
                binding.emptyLL.visibility = View.GONE
                binding.typeList.visibility = View.VISIBLE
            }

            // display all from db (main/home page)
            binding.typeList.adapter = RecipeRecyclerAdapter(this@HomepageFragment, recipeList){ _: Recipe, position: Int ->
                val bundle = Bundle()
                bundle.putString("recipeID", recipeList[position].id)

                val fragmentManager = activity?.supportFragmentManager?.beginTransaction()
                val fragment = RecipeDetailFragment()
                fragment.arguments = bundle

                fragmentManager?.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                fragmentManager?.replace(R.id.containerForRecipe, fragment)
                fragmentManager?.addToBackStack(null)
                fragmentManager?.commit()
            }
        }

        binding.recipeTypeGroup.setOnCheckedChangeListener{ _, optionId ->
            run{
                var type = ""
                when(optionId){
                    R.id.r_all -> type = ""
                    R.id.r_breakfast -> type = "Breakfast"
                    R.id.r_lunch -> type = "Lunch"
                    R.id.r_dinner -> type = "Dinner"
                    R.id.r_pizza -> type = "Pizza"
                    R.id.r_dessert -> type = "Desserts"
                    R.id.r_curry -> type = "Curry"
                    R.id.r_appetizer -> type = "Appetizers"
                }

                val rArrList: ArrayList<Recipe> = ArrayList<Recipe>()

                for (r in recipeList) {
                    if (r.recipeTypes.contains(type)) {
                        rArrList.add(r)
                    }
                }

            if(rArrList.isEmpty()){
                binding.emptyHomeImg.visibility = View.VISIBLE
                binding.typeList.visibility = View.GONE
            }else if(rArrList.isNotEmpty()){
                binding.emptyHomeImg.visibility = View.GONE
                binding.typeList.visibility = View.VISIBLE
            }

            // click type and show filtered type list
            // if no put this, it will show all item even tho click filtered type (will not chg based on filter type)
                binding.typeList.adapter = RecipeRecyclerAdapter(this@HomepageFragment, rArrList){ _: Recipe, position: Int ->
                    val bundle = Bundle()
                    bundle.putString("recipeID", rArrList[position].id)

                    val fragmentManager = activity?.supportFragmentManager?.beginTransaction()
                    val fragment = RecipeDetailFragment()
                    fragment.arguments = bundle

                    fragmentManager?.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                    fragmentManager?.replace(R.id.containerForRecipe, fragment)
                    fragmentManager?.addToBackStack(null)
                    fragmentManager?.commit()
                }
            }
        }

        return binding.root
    }

    class RecipeRecyclerAdapter(val context: HomepageFragment, val items: List<Recipe>, val clickListener: (Recipe, Int) -> Unit) :
        RecyclerView.Adapter<RecipeRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_type_list, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items.get(position)

            holder.viewRecipeName.text = item.recipeName
            holder.viewRecipeType.text = item.recipeTypes
            holder.loadImg(item.id)

            holder.containerView.setOnClickListener { clickListener(item, position) }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView) {
            val viewRecipeImg: ImageView = itemView.findViewById<ImageView>(R.id.viewRecipeImg)
            val viewRecipeName: TextView = itemView.findViewById<TextView>(R.id.viewRecipeName)
            val viewRecipeType: TextView = itemView.findViewById<TextView>(R.id.viewRecipeType)

            fun loadImg(Id: String){
                val storageRef = FirebaseStorage.getInstance().reference.child("Recipe/${Id}.jpg")
                storageRef.downloadUrl.addOnSuccessListener (OnSuccessListener<Uri> {
                    Glide.with(context).load(it).into(viewRecipeImg)
                }).addOnFailureListener{
                    viewRecipeImg.setImageResource(R.drawable.ic_baseline_image_24)
                }
            }
        }

    }

}