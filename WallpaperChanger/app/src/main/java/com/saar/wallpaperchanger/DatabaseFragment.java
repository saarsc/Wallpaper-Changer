package com.saar.wallpaperchanger;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DatabaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DatabaseFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DatabaseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DatabaseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DatabaseFragment newInstance(String param1, String param2) {
        DatabaseFragment fragment = new DatabaseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_database, container, false);

        final DbHandler dbHandler = new DbHandler(view.getContext());

        final TextView countInfo = view.findViewById(R.id.tvCountData);
        final TextView timePeriod = view.findViewById(R.id.tvTimePeriod);

        final int alreadyPlayed = dbHandler.getPlace(false);
        final long totalAlbums = dbHandler.getRowsCount();

        final String countString = alreadyPlayed + "/" + totalAlbums;

        final long albumsLeft = alreadyPlayed - totalAlbums;

        try {
            final LocalDate startDate = new SimpleDateFormat("dd.MM.yy").parse(dbHandler.firstAlbumDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            timePeriod.setText(startDate.toString() + " - " + startDate.plusDays(totalAlbums).toString() + " ( " + albumsLeft * -1 + " )");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        countInfo.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(view.getContext());
            dialog.setContentView(R.layout.all_albums_dialog);
            dialog.setTitle("Albums...");
            ListView myNames = (ListView) dialog.findViewById(R.id.List);

            ArrayAdapter<Photo> adapter = new ArrayAdapter(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, dbHandler.availablePhotos("",false));
            myNames.setAdapter(adapter);
            dialog.show();
        });
        final Button restoreDB = view.findViewById(R.id.btnRestore);

        restoreDB.setOnClickListener(v -> {
            dbHandler.restoreDB();
//            dbHandler.restoreStats();
        });

        final Button pickWallpaper = view.findViewById(R.id.mainActivityChangeWallpaper);
        pickWallpaper.setOnClickListener(v -> util.changeWallpaper(view.getContext()));


        final Button resetDB = view.findViewById(R.id.btnResetDB);
        resetDB.setOnClickListener(v -> {
            dbHandler.resetDB();

        });


        countInfo.setText(countString);

        ArrayAdapter<String> arrayAdapterAlbums = new ArrayAdapter<>(view.getContext(), android.R.layout.select_dialog_item, dbHandler.getAllNames());
        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.autoAlbumName);
        Button confirm = view.findViewById(R.id.btnConfirm);

        autoCompleteTextView.setAdapter(arrayAdapterAlbums);
        confirm.setOnClickListener(v->{
            SharedPreferences sp = view.getContext().getSharedPreferences("currentAlbum",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            editor.putString("next_album",autoCompleteTextView.getText().toString());
            editor.apply();
        });

        ImageView imageFrame = view.findViewById(R.id.imageFrame);

        SharedPreferences sp = view.getContext().getSharedPreferences("currentAlbum", Context.MODE_PRIVATE);
        String photoPath = sp.getString("PATH", "");

        if (!photoPath.equals("")) {
            Glide.with(view.getContext()).asBitmap().load(photoPath).fitCenter().into(imageFrame);
        }
        return view;
    }
}