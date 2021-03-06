package se.dobus.flex.models;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;

import se.dobus.flex.AddFlexActivity;
import se.dobus.flex.CalendarActivity;

/**
 * Created by tobiasn on 05/09/2017.
 */

public class TimePickerFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        switch (getArguments().getString("type")){
            case "duration":
                return duration(c);
            case "time":
                return time(c);
        }

        // Create a new instance of TimePickerDialog and return it
        return null;
    }

    public TimePickerDialog time(Calendar c){
        c.setTime(new Date(getArguments().getLong("current_date")));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), (AddFlexActivity)getActivity(), hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public TimePickerDialog duration(Calendar c){
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), (AddFlexActivity)getActivity(), hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }
}
