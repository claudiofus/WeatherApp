/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.claudiofus.clock2.alarms;

import android.os.Parcel;
import android.os.Parcelable;

import com.claudiofus.clock2.alarms.misc.DaysOfWeek;
import com.claudiofus.clock2.data.ObjectWithId;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.CLOUDY;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.FOGGY;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.NUM_WEATHER_CONDITIONS;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.RAINY;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.SNOWY;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.SUNNY;
import static com.claudiofus.clock2.alarms.misc.ConditionsOfWeather.WINDY;

/**
 * Created by Phillip Hsu on 5/26/2016.
 */
@AutoValue
public abstract class Alarm extends ObjectWithId implements Parcelable {
    private static final int MAX_MINUTES_CAN_SNOOZE = 30;

    // =================== MUTABLE =======================
    private long snoozingUntilMillis;
    private boolean enabled;
    private final boolean[] recurringDays = new boolean[DaysOfWeek.NUM_DAYS];
    private LinkedHashMap<String,String> weatherConditions = initWeatherConditions();
    private boolean ignoreUpcomingRingTime;
    // ====================================================

    public abstract int hour();
    public abstract int minutes();
    public abstract String label();
    public abstract String ringtone();
    public abstract boolean vibrates();
    /** Initializes a Builder to the same property values as this instance */
    public abstract Builder toBuilder();

    public void copyMutableFieldsTo(Alarm target) {
        target.setId(this.getId());
        target.snoozingUntilMillis = this.snoozingUntilMillis;
        target.enabled = this.enabled;
        System.arraycopy(this.recurringDays, 0, target.recurringDays, 0, DaysOfWeek.NUM_DAYS);
        target.ignoreUpcomingRingTime = this.ignoreUpcomingRingTime;
        target.weatherConditions = this.weatherConditions;
    }

    public static Builder builder() {
        // Unfortunately, default valoues must be provided for generated Builders.
        // Fields that were not set when build() is called will throw an exception.
        return new AutoValue_Alarm.Builder()
                .hour(0)
                .minutes(0)
                .label("")
                .ringtone("")
                .vibrates(false);
    }

    public void snooze(int minutes) {
        if (minutes <= 0 || minutes > MAX_MINUTES_CAN_SNOOZE)
            throw new IllegalArgumentException("Cannot snooze for "+minutes+" minutes");
        snoozingUntilMillis = System.currentTimeMillis() + minutes * 60000;
    }

    public long snoozingUntil() {
        return isSnoozed() ? snoozingUntilMillis : 0;
    }

    public boolean isSnoozed() {
        if (snoozingUntilMillis <= System.currentTimeMillis()) {
            snoozingUntilMillis = 0;
            return false;
        }
        return true;
    }

    /** <b>ONLY CALL THIS WHEN CREATING AN ALARM INSTANCE FROM A CURSOR</b> */
    // TODO: To be even more safe, create a ctor that takes a Cursor and
    // initialize the instance here instead of in AlarmDatabaseHelper.
    public void setSnoozing(long snoozingUntilMillis) {
        this.snoozingUntilMillis = snoozingUntilMillis;
    }

    public void stopSnoozing() {
        snoozingUntilMillis = 0;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRecurring(int day, boolean recurring) {
        checkDay(day);
        recurringDays[day] = recurring;
    }

    public boolean isRecurring(int day) {
        checkDay(day);
        return recurringDays[day];
    }

    public boolean hasRecurrence() {
        return numRecurringDays() > 0;
    }

    public int numRecurringDays() {
        int count = 0;
        for (boolean b : recurringDays)
            if (b) count++;
        return count;
    }

    public void ignoreUpcomingRingTime(boolean ignore) {
        ignoreUpcomingRingTime = ignore;
    }

    public boolean isIgnoringUpcomingRingTime() {
        return ignoreUpcomingRingTime;
    }

    public LinkedHashMap<String, String> initWeatherConditions () {
        weatherConditions = new LinkedHashMap<>(NUM_WEATHER_CONDITIONS);
        weatherConditions.put(SUNNY, null);
        weatherConditions.put(RAINY, null);
        weatherConditions.put(CLOUDY, null);
        weatherConditions.put(SNOWY, null);
        weatherConditions.put(FOGGY, null);
        weatherConditions.put(WINDY, null);
        return weatherConditions;
    }

    public void setWeatherCondition (String condition, String value) throws IllegalStateException {
        if (weatherConditions.containsKey(condition)) {
            weatherConditions.put(condition, value);
        } else {
            throw new IllegalStateException("Condition" + condition + " is not included in:" + weatherConditions.keySet());
        }
    }

    public String getWeatherCondition (String condition) throws IllegalStateException {
        if (weatherConditions.containsKey(condition)) {
            return weatherConditions.get(condition);
        } else {
            throw new IllegalStateException("Condition" + condition + " is not included in:" + weatherConditions.keySet());
        }
    }

    public LinkedHashMap<String, String> getWeatherConditions () {
        return weatherConditions;
    }

    public void removeWeatherCondition (String condition) throws IllegalStateException {
        if (weatherConditions.containsKey(condition)) {
            weatherConditions.put(condition, null);
        } else {
            throw new IllegalStateException("Condition" + condition + " is not included in:" + weatherConditions.keySet());
        }
    }

    public String getLabel (int position) throws IllegalStateException {
        return new ArrayList<>(weatherConditions.keySet()).get(position);
    }

    public long ringsAt() {
        // Always with respect to the current date and time
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hour());
        calendar.set(Calendar.MINUTE, minutes());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long baseRingTime = calendar.getTimeInMillis();

        if (!hasRecurrence()) {
            if (baseRingTime <= System.currentTimeMillis()) {
                // The specified time has passed for today
                baseRingTime += TimeUnit.DAYS.toMillis(1);
            }
            return baseRingTime;
        } else {
            // Compute the ring time just for the next closest recurring day.
            // Remember that day constants defined in the Calendar class are
            // not zero-based like ours, so we have to compensate with an offset
            // of magnitude one, with the appropriate sign based on the situation.
            int weekdayToday = calendar.get(Calendar.DAY_OF_WEEK);
            int numDaysFromToday = -1;

            for (int i = weekdayToday; i <= Calendar.SATURDAY; i++) {
                if (isRecurring(i - 1 /*match up with our day constant*/)) {
                    if (i == weekdayToday) {
                        if (baseRingTime > System.currentTimeMillis()) {
                            // The normal ring time has not passed yet
                            numDaysFromToday = 0;
                            break;
                        }
                    } else {
                        numDaysFromToday = i - weekdayToday;
                        break;
                    }
                }
            }

            // Not computed yet
            if (numDaysFromToday < 0) {
                for (int i = Calendar.SUNDAY; i < weekdayToday; i++) {
                    if (isRecurring(i - 1 /*match up with our day constant*/)) {
                        numDaysFromToday = Calendar.SATURDAY - weekdayToday + i;
                        break;
                    }
                }
            }

            // Still not computed yet. The only recurring day is weekdayToday,
            // and its normal ring time has already passed.
            if (numDaysFromToday < 0 && isRecurring(weekdayToday - 1)
                    && baseRingTime <= System.currentTimeMillis()) {
                numDaysFromToday = 7;
            }

            if (numDaysFromToday < 0)
                throw new IllegalStateException("How did we get here?");

            return baseRingTime + TimeUnit.DAYS.toMillis(numDaysFromToday);
        }
    }

    public long ringsIn() {
        return ringsAt() - System.currentTimeMillis();
    }

    /**
     * Returns whether this Alarm is upcoming in the next {@code hours} hours.
     * To return true, this Alarm must not have its {@link #ignoreUpcomingRingTime}
     * member field set to true.
     * @see #ignoreUpcomingRingTime(boolean)
     */
    public boolean ringsWithinHours(int hours) {
        return !ignoreUpcomingRingTime && ringsIn() <= TimeUnit.HOURS.toMillis(hours);
    }

    // ============================ PARCELABLE ==============================
    // Unfortunately, we can't use the Parcelable extension for AutoValue because
    // our model isn't totally immutable. Our mutable properties will be left
    // out of the generated class.

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(hour());
        dest.writeInt(minutes());
        dest.writeString(label());
        dest.writeString(ringtone());
        dest.writeInt(vibrates() ? 1 : 0);
        // Mutable fields must be written after the immutable fields,
        // because when we recreate the object, we can't initialize
        // those mutable fields until after we call build(). Values
        // in the parcel are read in the order they were written.
        dest.writeLong(getId());
        dest.writeLong(snoozingUntilMillis);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeBooleanArray(recurringDays);
        dest.writeSerializable(weatherConditions);
        dest.writeInt(ignoreUpcomingRingTime ? 1 : 0);
    }

    private static Alarm create(Parcel in) {
        Alarm alarm = Alarm.builder()
                .hour(in.readInt())
                .minutes(in.readInt())
                .label(in.readString())
                .ringtone(in.readString())
                .vibrates(in.readInt() != 0)
                .build();
        alarm.setId(in.readLong());
        alarm.snoozingUntilMillis = in.readLong();
        alarm.enabled = in.readInt() != 0;
        in.readBooleanArray(alarm.recurringDays);
        alarm.weatherConditions = (LinkedHashMap<String, String>) in.readSerializable();
        alarm.ignoreUpcomingRingTime = in.readInt() != 0;
        return alarm;
    }

    public static final Parcelable.Creator<Alarm> CREATOR
            = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel source) {
            return Alarm.create(source);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    // ======================================================================

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder hour(int hour);
        public abstract Builder minutes(int minutes);
        public abstract Builder label(String label);
        public abstract Builder ringtone(String ringtone);
        public abstract Builder vibrates(boolean vibrates);
        /* package */ abstract Alarm autoBuild();

        public Alarm build() {
            Alarm alarm = autoBuild();
            doChecks(alarm);
            return alarm;
        }
    }

    private static void doChecks(Alarm alarm) {
        checkTime(alarm.hour(), alarm.minutes());
    }

    private static void checkDay(int day) {
        if (day < DaysOfWeek.SUNDAY || day > DaysOfWeek.SATURDAY) {
            throw new IllegalArgumentException("Invalid day of week: " + day);
        }
    }

    private static void checkTime(int hour, int minutes) {
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            throw new IllegalStateException("Hour and minutes invalid");
        }
    }
}