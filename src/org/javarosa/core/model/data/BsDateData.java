package org.javarosa.core.model.data;

import bikramsambat.BsCalendar;
import bikramsambat.BsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import static org.javarosa.core.model.utils.DateUtils.formatDate;
import static org.javarosa.core.model.utils.DateUtils.parseDate;
import static org.javarosa.core.model.utils.DateUtils.roundDate;
import static org.javarosa.core.model.utils.DateUtils.FORMAT_ISO8601;

public class BsDateData implements IAnswerData {
    private Date d;
    private boolean initialised;

    public BsDateData() { /* used for deserialisation */ }
    public BsDateData(Date d) { setValue(d); }

//> IAnswerData METHODS
    @Override public IAnswerData clone() {
        return new BsDateData(new Date(d.getTime()));
    }

    @Override public void setValue (Object o) {
        if(o == null) throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        d = roundDate((Date) o);
    }

    @Override public Object getValue() {
        return new Date(d.getTime());
    }

    @Override public String getDisplayText() {
        BsCalendar cal = BsCalendar.getInstance();

        try {
            return cal.toBik_dev(cal.gregDateFormat().format(d)) + "bibble";
        } catch(BsException ex) {
            // TODO don't printStackTrace()
            ex.printStackTrace();
            return "????-??-??";
        }
    }

    @Override public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        setValue(ExtUtil.readDate(in));
    }

    @Override public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeDate(out, d);
    }

    @Override public UncastData uncast() {
        return new UncastData(formatDate(d, FORMAT_ISO8601));
    }

    @Override public BsDateData cast(UncastData data) throws IllegalArgumentException {
        // implementation copied from superclass
        Date ret = parseDate(data.value);

        if(ret == null) throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type BsDate");

        return new BsDateData(ret);
    }
}
