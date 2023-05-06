package com.saar.wallpaperchanger;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
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

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        DbHandler db = new DbHandler(view.getContext());

        ArrayAdapter<String> arrayAdapterAlbums = new ArrayAdapter<>(view.getContext(), android.R.layout.select_dialog_item, db.getAllNames());
        ArrayAdapter<String> arrayAdapterArtist = new ArrayAdapter<>(view.getContext(), android.R.layout.select_dialog_item, db.getAllArtistNames());

        AutoCompleteTextView searchBox = view.findViewById(R.id.searchBox);
        CheckBox isVinyl = view.findViewById(R.id.cbIsVinyl);
        CheckBox isWeekend = view.findViewById(R.id.cbWeekend);
        SwitchMaterial searchBy = view.findViewById(R.id.searchBySwitch);

        searchBox.setOnClickListener(v -> {
            if (searchBy.isChecked()) {
                searchBox.setAdapter(arrayAdapterArtist);
            } else {
                searchBox.setAdapter(arrayAdapterAlbums);

            }
        });

        searchBox.setThreshold(1);

        searchBox.setAdapter(arrayAdapterAlbums);


        TextView searchResult = view.findViewById(R.id.searchResult);

        searchBy.setOnClickListener(v -> {
            if (searchBy.isChecked()) {
                searchBox.setAdapter(arrayAdapterArtist);

            } else {
                searchBox.setAdapter(arrayAdapterAlbums);

            }
        });


        Button searchButton = view.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            String result = "לא נמצא נתון";
            String val = searchBox.getText().toString();
            if (searchBy.isChecked()) {
                result = db.searchByArtist(val);
                searchResult.setPaintFlags(searchResult.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                isVinyl.setVisibility(View.GONE);
                isWeekend.setVisibility(View.GONE);
            } else {
                result = db.getAlbumDate(val);
                isVinyl.setChecked(db.isVinyl(val));
                isWeekend.setChecked(db.isOnlyWeekend(val));

                isVinyl.setVisibility(View.VISIBLE);
                isWeekend.setVisibility(View.VISIBLE);
                searchResult.setPaintFlags(searchResult.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            }
            searchResult.setText(result);
        });

        searchResult.setOnClickListener(v -> {
            if (searchBy.isChecked()) {
                final Dialog dialog = new Dialog(view.getContext());
                dialog.setContentView(R.layout.all_albums_dialog);
                dialog.setTitle("Albums...");
                ListView myNames = (ListView) dialog.findViewById(R.id.List);

                ArrayAdapter<Photo> adapter = new ArrayAdapter(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, db.getAllAlbumsByArtist(searchBox.getText().toString()));
                myNames.setAdapter(adapter);
                dialog.show();
            }
        });

        isVinyl.setOnClickListener(v -> {
            db.setVinyl(searchBox.getText().toString(), isVinyl.isChecked());
        });
        isWeekend.setOnClickListener(v -> {
            db.setVinyl(searchBox.getText().toString(), isWeekend.isChecked(), isWeekend.isChecked());
            isVinyl.setChecked(isWeekend.isChecked());
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