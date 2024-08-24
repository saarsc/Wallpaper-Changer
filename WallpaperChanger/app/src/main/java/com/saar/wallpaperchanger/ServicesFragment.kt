package com.saar.wallpaperchanger

import android.app.job.JobScheduler
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.saar.wallpaperchanger.utils.jobUtils.scheduleJob

/**
 * A simple [Fragment] subclass.
 * Use the [ServicesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ServicesFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_services, container, false)

        val status = view.findViewById<TextView>(R.id.tvJobStatus)

        val startService = view.findViewById<Button>(R.id.btnStartService)
        val cancelService = view.findViewById<Button>(R.id.btnStopService)

        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, 200f)
            .build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)

        val jobScheduler =
            view.context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val isScheduled = jobScheduler.getPendingJob(774799256) != null
        //        boolean isScheduled = false;
//        try {
//            isScheduled = WorkManager.getInstance(view.getContext()).getWorkInfosForUniqueWork("Wallpaper Changer").get().size() >0;
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (isScheduled) {
            status.text = "The job is active"

            shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor("#34B234"))
        } else {
            status.text = "The job is not active"
            shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor("#C91D1D"))
        }

        ViewCompat.setBackground(status, shapeDrawable)

        startService.setOnClickListener { v: View? ->
            scheduleJob(view.context)
            //            util.scheduleJobWorker(view.getContext());
            status.text = "The job is active"
            shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor("#34B234"))
            ViewCompat.setBackground(status, shapeDrawable)
        }

        cancelService.setOnClickListener { v: View? ->
//            WorkManager.getInstance(view.getContext()).cancelAllWorkByTag("Wallpaper Changer");
            jobScheduler.cancel(774799256)
            status.text = "The jos is not active"
            shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor("#C91D1D"))
            ViewCompat.setBackground(status, shapeDrawable)
        }

        return view
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
         * @return A new instance of fragment servicesFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): ServicesFragment {
            val fragment = ServicesFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}