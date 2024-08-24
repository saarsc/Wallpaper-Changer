package com.saar.wallpaperchanger

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private lateinit var namesRecyclerView: RecyclerView
    private lateinit var artistFilterSpinner: Spinner
    private lateinit var namesAdapter: NamesAdapter
    private lateinit var db: DbHandler
    private var usedFilter: Boolean = false
    private var vinylFilter: Boolean = false
    private var onlyWeekendFilter: Boolean = false
    private var selectedItem: String= "";
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = requireArguments().getString(ARG_PARAM1)
            mParam2 = requireArguments().getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_search, container, false)

        db = DbHandler(view.context)
        namesRecyclerView = view.findViewById(R.id.namesRecyclerView)
//        artistFilterSpinner = view.findViewById(R.id.artistFilterSpinner)

        namesRecyclerView.layoutManager = LinearLayoutManager(view.context)
        namesAdapter = NamesAdapter()
        namesRecyclerView.adapter = namesAdapter

//        loadArtists(view);
        loadNames();

//        artistFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>,
//                view: View,
//                position: Int,
//                id: Long
//            ) {
//                val selectedArtist = parent.getItemAtPosition(position).toString()
//                loadNames(selectedArtist)
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                loadNames()
//            }
//        }
        val dataAdapter =
            ArrayAdapter(view.context, android.R.layout.select_dialog_item, db.allNames() + db.allArtistNames())

        val searchBox = view.findViewById<AutoCompleteTextView>(R.id.searchBox)
//        val isVinyl = view.findViewById<CheckBox>(R.id.cbIsVinyl)
//        val isWeekend = view.findViewById<CheckBox>(R.id.cbWeekend)
//        val searchBy = view.findViewById<SwitchMaterial>(R.id.searchBySwitch)
        searchBox.setAdapter(dataAdapter)
//
        searchBox.threshold = 2

        searchBox.setOnItemClickListener { parent, view, position, id ->
            selectedItem = parent.getItemAtPosition(position).toString()
            applyFilters()
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedItem = s.toString()

                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
//
//        searchBox.setAdapter(arrayAdapterAlbums)
//
//
//        val searchResult = view.findViewById<TextView>(R.id.searchResult)
//
//        searchBy.setOnClickListener { v: View? ->
//            if (searchBy.isChecked) {
//                searchBox.setAdapter(arrayAdapterArtist)
//            } else {
//                searchBox.setAdapter(arrayAdapterAlbums)
//            }
//        }
//
//
//        val searchButton = view.findViewById<Button>(R.id.searchButton)
//        searchButton.setOnClickListener { v: View? ->
//            var result = "לא נמצא נתון"
//            val `val` = searchBox.text.toString()
//            if (searchBy.isChecked) {
//                result = db.searchByArtist(`val`)
//                searchResult.paintFlags = searchResult.paintFlags or Paint.UNDERLINE_TEXT_FLAG
//                isVinyl.visibility = View.GONE
//                isWeekend.visibility = View.GONE
//            } else {
//                result = db.getAlbumDate(`val`)
//                isVinyl.isChecked = db.isVinyl(`val`)
//                isWeekend.isChecked = db.isOnlyWeekend(`val`)
//
//                isVinyl.visibility = View.VISIBLE
//                isWeekend.visibility = View.VISIBLE
//                searchResult.paintFlags =
//                    searchResult.paintFlags and (Paint.UNDERLINE_TEXT_FLAG.inv())
//            }
//            searchResult.text = result
//        }
//
//        searchResult.setOnClickListener { v: View? ->
//            if (searchBy.isChecked) {
//                val dialog = Dialog(view.context)
//                dialog.setContentView(R.layout.all_albums_dialog)
//                dialog.setTitle("Albums...")
//                val myNames = dialog.findViewById<View>(R.id.List) as ListView
//
//                val adapter: ArrayAdapter<Any?> = ArrayAdapter<Any?>(
//                    view.context,
//                    android.R.layout.simple_list_item_1,
//                    android.R.id.text1,
//                    db.getAllAlbumsByArtist(searchBox.text.toString())
//                )
//                myNames.adapter = adapter
//                dialog.show()
//            }
//        }
//
//        isVinyl.setOnClickListener { v: View? ->
//            db.setVinyl(searchBox.text.toString(), isVinyl.isChecked)
//        }
//        isWeekend.setOnClickListener { v: View? ->
//            db.setVinyl(searchBox.text.toString(), isWeekend.isChecked, isWeekend.isChecked)
//            isVinyl.isChecked = isWeekend.isChecked
//        }
//        val imageFrame = view.findViewById<ImageView>(R.id.imageFrame)
//
//        val sp = view.context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
//        val photoPath = sp.getString("PATH", "")
//
//        if (photoPath != "") {
//            Glide.with(view.context).asBitmap().load(photoPath).fitCenter().into(imageFrame)
//        }
//        artistFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                applyFilters()
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                applyFilters()
//            }
//        }
        val filterButton: Button = view.findViewById(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterPopup(view.context)
        }
        return view
    }

//    private fun loadArtists(view: View) {
//        val artists = mutableListOf<String>()
//        artists.add("All Artists")
//        artists.addAll(db.allArtistNames())
//        val adapter = ArrayAdapter(view.getContext(), R.layout.spinnr_item, artists).apply {
//            setDropDownViewResource(R.layout.spinner_dropdown_item)
//        }
//
//        artistFilterSpinner.adapter = adapter
//    }

    private fun loadNames(artist: String = "") {
        namesAdapter.setNames(db.search(artist, usedFilter, vinylFilter, onlyWeekendFilter))
    }

    private fun applyFilters() {
        val artist = if (selectedItem == "All Artists" || selectedItem.isEmpty()) "" else selectedItem
        loadNames(artist)
    }

    private fun showFilterPopup(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.filter_popup, null)
        val usedCheckBox: CheckBox = dialogView.findViewById(R.id.usedCheckBox)
        val vinylCheckBox: CheckBox = dialogView.findViewById(R.id.vinylCheckBox)
        val onlyWeekendCheckBox: CheckBox = dialogView.findViewById(R.id.onlyWeekendCheckBox)
        val applyFiltersButton: Button = dialogView.findViewById(R.id.applyFiltersButton)

        usedCheckBox.isChecked = usedFilter
        vinylCheckBox.isChecked = vinylFilter
        onlyWeekendCheckBox.isChecked = onlyWeekendFilter

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        applyFiltersButton.setOnClickListener {
            usedFilter = usedCheckBox.isChecked
            vinylFilter = vinylCheckBox.isChecked
            onlyWeekendFilter = onlyWeekendCheckBox.isChecked
            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SearchFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}