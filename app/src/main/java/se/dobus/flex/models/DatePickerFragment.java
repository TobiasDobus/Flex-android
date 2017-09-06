package se.dobus.flex.models;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

import se.dobus.flex.AddFlexActivity;
import se.dobus.flex.CalendarActivity;

/**
 * Created by tobiasn on 05/09/2017.
 */

public class DatePickerFragment extends DialogFragment{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        System.out.println(this.getTag());
        switch (this.getTag()){
            case "AddFlexDatePicker":
                return new DatePickerDialog(getActivity(), (AddFlexActivity)getActivity(), year, month, day);
            case "CalendarDatePicker":
                return new DatePickerDialog(getActivity(), (CalendarActivity)getActivity(), year, month, day);
        }

        // Create a new instance of DatePickerDialog and return it
        return null;
    }

}
