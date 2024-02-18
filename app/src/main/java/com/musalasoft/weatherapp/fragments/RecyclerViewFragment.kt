package com.musalasoft.weatherapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity

class RecyclerViewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.fragment_recycler_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val view = view ?: return
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.setLayoutManager(LinearLayoutManager(view.context))
        val activity = activity
        val bundle = this.arguments
        if (activity is MainActivity && bundle != null && bundle.containsKey("day")) {
            val mainActivity: MainActivity? = getActivity() as MainActivity?
            recyclerView.setAdapter(mainActivity!!.getAdapter(bundle.getInt("day")))
        }
    }
}
