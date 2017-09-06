package se.dobus.flex.models;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

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
        System.out.println(this.getTag());
        Bundle bundle = getArguments();
        c.setTime(new Date(bundle.getLong("current_date")));
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        switch (this.getTag()){
            case "AddFlexDatePicker":
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), (AddFlexActivity)getActivity(), year, month, day);
                dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                return dialog;
            case "CalendarDatePicker":
                DatePickerDialog dialog2 = new DatePickerDialog(getActivity(), (CalendarActivity)getActivity(), year, month, day);
                dialog2.getDatePicker().setMaxDate(System.currentTimeMillis());
                return dialog2;
        }

        // Create a new instance of DatePickerDialog and return it
        return null;
    }

}
