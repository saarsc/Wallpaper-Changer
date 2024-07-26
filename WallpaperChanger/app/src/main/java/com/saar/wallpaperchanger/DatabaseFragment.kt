package com.saar.wallpaperchanger

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZoneId

/**
 * A simple [Fragment] subclass.
 * Use the [DatabaseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DatabaseFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_database, container, false)

        val dbHandler = DbHandler(view.context)

        val countInfo = view.findViewById<TextView>(R.id.tvCountData)
        val timePeriod = view.findViewById<TextView>(R.id.tvTimePeriod)

        val showArtistStats = view.findViewById<Switch>(R.id.switchSeeArtist)
        val vinylOnWeekend = view.findViewById<Switch>(R.id.switchUseVinylOnWeekend)

        val alreadyPlayed = dbHandler.getPlace(false)
        val totalAlbums = dbHandler.rowsCount

        val countString = "$alreadyPlayed/$totalAlbums"

        val albumsLeft = alreadyPlayed - totalAlbums

        try {
            val startDate =
                SimpleDateFormat("dd.MM.yy").parse(dbHandler.firstAlbumDate()).toInstant().atZone(
                    ZoneId.systemDefault()
                ).toLocalDate()
            timePeriod.text = startDate.toString() + " - " + startDate.plusDays(totalAlbums)
                .toString() + " ( " + albumsLeft * -1 + " )"
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        countInfo.setOnClickListener { v: View? ->
            val dialog = Dialog(view.context)
            dialog.setContentView(R.layout.all_albums_dialog)
            val myNames = dialog.findViewById<View>(R.id.List) as ListView
            val adapter: ArrayAdapter<*>
            if (showArtistStats.isChecked) {
                dialog.setTitle("Artist...")
                adapter = ArrayAdapter<Any?>(
                    view.context,
                    android.R.layout.simple_list_item_1,
                    android.R.id.text1,
                    dbHandler.availableArtistsWithPercentage()
                )
            } else {
                dialog.setTitle("Albums...")
                adapter = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_list_item_1,
                    android.R.id.text1,
                    dbHandler.availablePhotos("", false)
                )
            }

            myNames.adapter = adapter
            dialog.show()
        }
        val restoreDB = view.findViewById<Button>(R.id.btnRestore)

        restoreDB.setOnClickListener { v: View? ->
            dbHandler.restoreDB()
        }

        val pickWallpaper = view.findViewById<Button>(R.id.mainActivityChangeWallpaper)
        pickWallpaper.setOnClickListener { v: View? -> util.changeWallpaper(view.context) }


        val resetDB = view.findViewById<Button>(R.id.btnResetDB)
        resetDB.setOnClickListener { v: View? ->
            dbHandler.resetDB()
        }

        val useVinylOnWeekends =
            view.context.getSharedPreferences("vinylWeekend", Context.MODE_PRIVATE)
        vinylOnWeekend.isChecked = useVinylOnWeekends.getBoolean("shouldUseVinyl", true)

        countInfo.text = countString

        val arrayAdapterAlbums =
            ArrayAdapter(view.context, android.R.layout.select_dialog_item, dbHandler.allNames)
        val autoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.autoAlbumName)
        val confirm = view.findViewById<Button>(R.id.btnConfirm)

        autoCompleteTextView.setAdapter(arrayAdapterAlbums)
        confirm.setOnClickListener { v: View? ->
            val sp = view.context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
            val editor = sp.edit()

            editor.putString("next_album", autoCompleteTextView.text.toString())
            editor.apply()
        }

        vinylOnWeekend.setOnClickListener { v: View? ->
            useVinylOnWeekends.edit().putBoolean(
                "shouldUseVinyl",
                !useVinylOnWeekends.getBoolean("shouldUseVinyl", true)
            ).apply()
        }
        val imageFrame = view.findViewById<ImageView>(R.id.imageFrame)

        val sp = view.context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
        val photoPath = sp.getString("PATH", "")

        if (photoPath != "") {
            Glide.with(view.context).asBitmap().load(photoPath).fitCenter().into(imageFrame)
        }

        val export = view.findViewById<Button>(R.id.export_table)
        export.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("text/csv")
            intent.putExtra(Intent.EXTRA_TITLE, "photos.csv")
            startActivityForResult(intent, 1)
        }

        val importData = view.findViewById<Button>(R.id.import_table)
        importData.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.setType("*/*")
            startActivityForResult(intent, 2)
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                activity?.let { data!!.data?.let { it1 -> util.exportData(it, it1) } }
            } else if (requestCode == 2) {
                activity?.let { data!!.data?.let { it1 -> util.importDat(it, it1) } }
            }
        }
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
         * @return A new instance of fragment DatabaseFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): DatabaseFragment {
            val fragment = DatabaseFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}