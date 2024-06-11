package br.edu.leonardo.apps.sensors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import br.edu.leonardo.jaf.R;

/**
 * The class for the motion sensors fragment used in the view pager and tab layout in the main
 * activity.
 */
public class MotionSensorsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_motion_sensors, container, false);
    }

}