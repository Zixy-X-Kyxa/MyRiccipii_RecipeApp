package com.example.myriccipii.Recipes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myriccipii.R
import com.example.myriccipii.databinding.FragmentAddMainBinding
import com.example.myriccipii.databinding.FragmentRecipeDetailBinding

class AddMainFragment : Fragment() {
    private lateinit var binding: FragmentAddMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        binding = FragmentAddMainBinding.inflate(inflater, container, false)
        val fm = requireActivity().supportFragmentManager.beginTransaction()

        binding.toNextFragment.setOnClickListener{
            fm.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            fm.replace(R.id.containerForRecipe, AddNewRecipeFragment())
            fm.addToBackStack(null)
            fm.commit()
        }

        return binding.root
    }

}