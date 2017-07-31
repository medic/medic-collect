package org.odk.collect.android.widgets;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

import bikramsambat.BsException;
import bikramsambat.BsGregorianDate;
import bikramsambat.android.BsDatePicker;

import java.util.Date;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.odk.collect.android.R;

public class BikramSambatDateWidget extends QuestionWidget {
    private static final String t = "BikramSambatDateWidget";

    private BsDatePicker picker;

    public BikramSambatDateWidget(Context ctx, FormEntryPrompt prompt) {
        super(ctx, prompt);

        LayoutInflater i = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(i.inflate(R.layout.bikram_sambat_date_picker, null));

        picker = new BsDatePicker(this);
        picker.init();

        clearAnswer();
    }

//> QuestionWidget IMPLEMENTATION
    /** reset date to right now */
    @Override public void clearAnswer() { setAnswer(new DateTime()); }

    @Override public IAnswerData getAnswer() {
        DateTime dateTime = getAnswer_DateTime();
        if(dateTime == null) return null;

        return new DateData(dateTime.toDate());
    }

    @Override public void setFocus(Context ctx) {}

    @Override public void setOnLongClickListener(OnLongClickListener l) {}

//> PRIVATE HELPERS
    private DateTime getAnswer_DateTime() {
        try {
            BsGregorianDate greg = picker.getDate_greg();
            if(greg == null) return null;
            return new DateTime(greg.year, greg.month, greg.day, 0, 0);
        } catch(BsException ex) {
            trace("getAnswer_DateTime() :: ecxception caught: %s", ex);
            return null;
        }
    }

    private void setAnswer() {
        if (mPrompt.getAnswerValue() != null) {
            DateTime ldt = new DateTime(((Date) ((DateData) mPrompt.getAnswerValue()).getValue()).getTime());
            setAnswer(ldt);
        } else {
            clearAnswer();
        }
    }

    private void setAnswer(DateTime ldt) {
        BsGregorianDate greg = new BsGregorianDate(ldt.getYear(), ldt.getMonthOfYear(), ldt.getDayOfMonth());
        try {
            picker.setDate(greg);
        } catch(BsException ex) {
            trace("setAnswer() :: exception caught for date %s: %s", greg, ex);
        }
    }

    private static void trace(String message, Object... args) {
        Log.d(t, String.format(message, args));
    }
}
