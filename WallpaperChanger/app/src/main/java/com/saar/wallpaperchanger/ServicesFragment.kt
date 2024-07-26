package com.saar.wallpaperchanger;

import android.app.job.JobScheduler;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.concurrent.ExecutionException;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ServicesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ServicesFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ServicesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment servicesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ServicesFragment newInstance(String param1, String param2) {
        ServicesFragment fragment = new ServicesFragment();
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
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        final TextView status = view.findViewById(R.id.tvJobStatus);

        final Button startService = view.findViewById(R.id.btnStartService);
        final Button cancelService = view.findViewById(R.id.btnStopService);

        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 200)
                .build();
        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);

        final JobScheduler jobScheduler = (JobScheduler) view.getContext().getSystemService(JOB_SCHEDULER_SERVICE);
        boolean isScheduled = jobScheduler.getPendingJob(774799256) != null;
//        boolean isScheduled = false;
//        try {
//            isScheduled = WorkManager.getInstance(view.getContext()).getWorkInfosForUniqueWork("Wallpaper Changer").get().size() >0;
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (isScheduled) {
            status.setText("The job is active");

            shapeDrawable.setFillColor(ColorStateList.valueOf(Color.parseColor("#34B234")));
        } else {
            status.setText("The job is not active");
            shapeDrawable.setFillColor(ColorStateList.valueOf(Color.parseColor("#C91D1D")));
        }

        ViewCompat.setBackground(status, shapeDrawable);

        startService.setOnClickListener(v -> {
            util.scheduleJob(view.getContext());
//            util.scheduleJobWorker(view.getContext());
            status.setText("The job is active");
            shapeDrawable.setFillColor(ColorStateList.valueOf(Color.parseColor("#34B234")));
            ViewCompat.setBackground(status, shapeDrawable);
        });

        cancelService.setOnClickListener(v -> {
//            WorkManager.getInstance(view.getContext()).cancelAllWorkByTag("Wallpaper Changer");
            jobScheduler.cancel(774799256);
            status.setText("The jos is not active");
            shapeDrawable.setFillColor(ColorStateList.valueOf(Color.parseColor("#C91D1D")));
            ViewCompat.setBackground(status, shapeDrawable);
        });

        return view;
    }
}