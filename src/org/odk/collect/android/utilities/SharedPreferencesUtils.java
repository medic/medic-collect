package org.odk.collect.android.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.AdminPreferencesActivity;

import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class SharedPreferencesUtils {
	
	static Context mContext = Collect.getInstance().getApplicationContext();
	static String t = "SharedPreferencesUtils";

	static File hashFile = new File(Collect.ODK_ROOT + "/collect.settings");
	static File jsonFile = new File(Collect.ODK_ROOT + "/collect.json");

	public static void deleteSharedPreferenceFiles() {
		if (jsonFile.exists()) { jsonFile.delete(); }
		if (hashFile.exists()) { hashFile.delete(); }
	}
	
	public static boolean filesExist() {
		if (jsonFile.exists() || hashFile.exists()) { return true; }
		else { return false; }
	}
	public static boolean loadSharedPreferences() {
		
		boolean success = false;
		
		// try to load from json file first
		if (jsonFile.exists()) {
			success = loadSharedPreferencesFromJsonFile(jsonFile);
		}
		
		if (!success && hashFile.exists()) {
			success = loadSharedPreferencesFromFile(hashFile);
		}
		
		if (success) {
			deleteSharedPreferenceFiles();
			return true;
		}
		else {
			Log.i(t, "Failed to load settings from file");
			return false;
		}
	}
	
	private static boolean loadSharedPreferencesFromJsonFile(File src) {
		boolean res = false;
		
		FileInputStream stream = null;
		String json = null;
		
		try {
			stream = new FileInputStream(src);
	
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY,  0,  fc.size());
			json = Charset.defaultCharset().decode(bb).toString();
			
			JSONObject jsonObj = new JSONObject(json);
			JSONObject generalPrefs = jsonObj.getJSONObject("general_preferences");
			JSONObject adminPrefs = jsonObj.getJSONObject("admin_preferences");
			
			Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
			Editor adminEdit = mContext.getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0).edit();
	
			prefEdit.clear();
			adminEdit.clear();
			
			putJSONObjectIntoPreferences(generalPrefs, prefEdit);			
			putJSONObjectIntoPreferences(adminPrefs, adminEdit);
	
			prefEdit.commit();
			adminEdit.commit();
			
			Log.i(t, "Loaded json settings into preferences");
			res = true;
	
		} catch (FileNotFoundException e) {
	        Log.e(t, "File not found: " + src.toString(), e);
		} catch (IOException e) {
	        Log.e(t, "Failed to read file: " + src.toString(), e);
		} catch (JSONException e) {
	        Log.e(t, "Failure parsing JSON string: " + json, e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// NOOP
				}
			}
		}
		return res;
	}
	
	private static void putJSONObjectIntoPreferences(JSONObject json, Editor prefs) throws JSONException{
		// should clear and commit in calling function
		
		java.util.Iterator<?> keys = json.keys();
		
		while (keys.hasNext()) {
			String key = (String) keys.next();
			Object v = json.get(key);
	
			if (v instanceof Boolean)
				prefs.putBoolean(key, ((Boolean) v).booleanValue());
			else if (v instanceof Float)
				prefs.putFloat(key, ((Float) v).floatValue());
			else if (v instanceof Integer)
				prefs.putInt(key, ((Integer) v).intValue());
			else if (v instanceof Long)
				prefs.putLong(key, ((Long) v).longValue());
			else if (v instanceof String)
				prefs.putString(key, ((String) v));
		}	
	}
	private static boolean loadSharedPreferencesFromFile(File src) {
		// this should probably be in a thread if it ever gets big
		boolean res = false;
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(src));
			Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
			prefEdit.clear();
			// first object is preferences
			Map<String, ?> entries = (Map<String, ?>) input.readObject();
				
			for (Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();
	
				if (v instanceof Boolean)
					prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					prefEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					prefEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					prefEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.commit();
	
			// second object is admin options
			Editor adminEdit = mContext.getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0).edit();
			adminEdit.clear();
			// first object is preferences
			Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : adminEntries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();
	
				if (v instanceof Boolean)
					adminEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					adminEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					adminEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					adminEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					adminEdit.putString(key, ((String) v));
			}
			adminEdit.commit();
	
			Log.i(t, "Loaded hashmap settings into preferences");
			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}
	
	public static boolean saveSharedPreferencesToFile(File dst, Context context) {
		// this should be in a thread if it gets big, but for now it's tiny
		boolean res = false;
		ObjectOutputStream output = null;
		try {
			output = new ObjectOutputStream(new FileOutputStream(dst));
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(context);
			SharedPreferences adminPreferences = context.getSharedPreferences(
					AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

			// output.writeObject(pref.getAll());
			// output.writeObject(adminPreferences.getAll());

			Gson gson = new Gson();
			output.writeObject(gson.toJson(pref.getAll()));
			output.writeObject(gson.toJson(adminPreferences.getAll()));
			Log.i("AdminPreferencesActivity", gson.toJson(pref.getAll()));
			Log.i("AdminPreferencesActivity", gson.toJson(adminPreferences.getAll()));
			
			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (output != null) {
					output.flush();
					output.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

	public static boolean saveSharedPreferencesToJsonFile(File dst, Context context) {
		// this should be in a thread if it gets big, but for now it's tiny
		
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences adminPreferences = context.getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		Gson gson = new Gson();
		String jsonPrefs = "{\n\"general_preferences\": " + gson.toJson(pref.getAll()) + ",\n" 
						+ "\"admin_preferences\": " + gson.toJson(adminPreferences.getAll())
						+ "}";
		Log.d("AdminPreferencesActivity", jsonPrefs);
		
		return FileUtils.writeStringToFile(dst, jsonPrefs);
	}

}
