package org.odk.collect.android.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import bikramsambat.BikramSambatDate;
import bikramsambat.BsCalendar;
import bikramsambat.BsException;
import bikramsambat.BsGregorianDate;

import java.util.Date;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.odk.collect.android.R;

import static bikramsambat.android.BsDatePickerUtils.asDevanagariNumberInput;

public class BikramSambatDateWidget extends QuestionWidget {
    public BikramSambatDateWidget(Context ctx, FormEntryPrompt prompt) {
        super(ctx, prompt);

	// TODO add state change listener(?)

        LayoutInflater i = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(i.inflate(R.layout.bikram_sambat, null));

        asDevanagariNumberInput(this, R.id.txtYear);
        asDevanagariNumberInput(this, R.id.txtDay);

        clearAnswer();
    }

//> QuestionWidget IMPLEMENTATION
    @Override public void cancelLongPress() { super.cancelLongPress(); }

    /** reset date to right now */
    @Override public void clearAnswer() { setAnswer(new DateTime()); }

    @Override public void setOnLongClickListener(OnLongClickListener l) { /* TODO I have no idea what this is for */ }

    @Override
    public IAnswerData getAnswer() {
        clearFocus(); // TODO why is this part of "getting the answer"?  can we remove it?

        DateTime dateTime = getAnswer_DateTime();
        if(dateTime == null) return null;

        return new DateData(dateTime.toDate());
    }

    @Override public void setFocus(Context ctx) {
        // TODO work out why/if this is desirable, and document or remove as appropriate
        // Hide the soft keyboard if it's showing.
        InputMethodManager im = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

//> PRIVATE HELPERS
    private DateTime getAnswer_DateTime() {
        BsGregorianDate greg = getDate_greg();
        if(greg == null) return null;
        return new DateTime(greg.year, greg.month, greg.day, 0, 0);
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
        try {
            BsGregorianDate greg = new BsGregorianDate(ldt.getYear(), ldt.getMonthOfYear(), ldt.getDayOfMonth());
            BikramSambatDate bik = BsCalendar.getInstance().toBik(greg);

android.util.Log.w("HELLO ALEX", String.format("ldt=%s, greg=%s, bik=%s", ldt, greg, bik));

            setYear(bik.year);
            setMonth(bik.month);
            setDay(bik.day);
        } catch(BsException ex) {
            // TODO log the exception properly
            ex.printStackTrace();
        }
    }

//> COPY/PASTED FROM BsDatePickerDialog - TODO share these from the android lib
    private BikramSambatDate getDate_bs() {
        return new BikramSambatDate(getYear(), getMonth(), getDay());
    }

    private BsGregorianDate getDate_greg() {
        return BsCalendar.getInstance().toGreg(getDate_bs());
    }

    private int getDay() {
        try {
            return Integer.parseInt(text(R.id.txtDay));
        } catch(NumberFormatException ex) {
            return 0;
        }
    }

    private int getMonth() {
        return 1 + ((Spinner) findViewById(R.id.spnMonth)).getSelectedItemPosition();
    }

    private int getYear() {
        try {
            return Integer.parseInt(text(R.id.txtYear));
        } catch(NumberFormatException ex) {
            return 0;
        }
    }

    private void setDay(int day) { text(R.id.txtDay, Integer.toString(day)); }
    private void setMonth(int month) {
        Spinner s = (Spinner) findViewById(R.id.spnMonth);
        s.setSelection(month - 1);
    }
    private void setYear(int year) { text(R.id.txtYear, Integer.toString(year)); }

    private String text(int componentId) {
        EditText field = (EditText) findViewById(componentId);
        return field.getText().toString();
    }

    private void text(int componentId, String value) {
        TextView field = (TextView) findViewById(componentId);
        field.setText(value);
    }
}
