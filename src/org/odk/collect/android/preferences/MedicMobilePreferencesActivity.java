/*
 * Copyright (C) 2014 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.preferences;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.text.InputFilter;

/**
 * Handles 'Medic Mobile' specific preferences.
 * 
 * @author Marc Abbyad (marc@medicmobile.org), Carl Hartung (chartung@nafundi.com)
 */
public class MedicMobilePreferencesActivity extends AggregatePreferencesActivity
		implements OnPreferenceChangeListener {

	protected EditTextPreference mSubmissionUrlPreference;
	protected EditTextPreference mFormListUrlPreference;
	protected EditTextPreference mSmsGatewayPreference;
	protected EditTextPreference mOwnPhoneNumberPreference;
	protected Boolean mSmsUploadPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.medic_mobile_preferences);

		SharedPreferences adminPreferences = getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		mFormListUrlPreference = (EditTextPreference) findPreference(PreferencesActivity.KEY_FORMLIST_URL);
		mSubmissionUrlPreference = (EditTextPreference) findPreference(PreferencesActivity.KEY_SUBMISSION_URL);
		mSmsGatewayPreference = (EditTextPreference) findPreference(PreferencesActivity.KEY_SMS_GATEWAY);
		mOwnPhoneNumberPreference = (EditTextPreference) findPreference(PreferencesActivity.KEY_OWN_PHONE_NUMBER);
		mSmsUploadPreference = adminPreferences.getBoolean(PreferencesActivity.KEY_SMS_UPLOAD, getResources().getBoolean(R.bool.default_upload_sms));
		
		PreferenceCategory medicMobilePreferences = (PreferenceCategory) findPreference(getString(R.string.medic_mobile_preferences));

		mFormListUrlPreference.setOnPreferenceChangeListener(this);
		mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });

		mSubmissionUrlPreference.setOnPreferenceChangeListener(this);
		mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });

		mSmsGatewayPreference.setOnPreferenceChangeListener(this);
		mSmsGatewayPreference.setSummary(mSmsGatewayPreference.getText());
		
		mOwnPhoneNumberPreference.setOnPreferenceChangeListener(this);
		mOwnPhoneNumberPreference.setSummary(mOwnPhoneNumberPreference.getText());
		
	}

	/**
	 * Generic listener that sets the summary to the newly selected/entered
	 * value
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary((CharSequence) newValue);
		return true;
	}

}
