/*
 * Copyright (C) 2009 University of Washington
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

package org.odk.collect.android.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.DefaultHttpClient;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.listeners.InstanceUploaderListener;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.utilities.WebUtils;
import org.opendatakit.httpclientandroidlib.Header;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.HttpStatus;
import org.opendatakit.httpclientandroidlib.NameValuePair;
import org.opendatakit.httpclientandroidlib.client.ClientProtocolException;
import org.opendatakit.httpclientandroidlib.client.HttpClient;
import org.opendatakit.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import org.opendatakit.httpclientandroidlib.client.methods.HttpHead;
import org.opendatakit.httpclientandroidlib.client.methods.HttpPost;
import org.opendatakit.httpclientandroidlib.conn.ConnectTimeoutException;
import org.opendatakit.httpclientandroidlib.conn.HttpHostConnectException;
import org.opendatakit.httpclientandroidlib.entity.mime.MultipartEntity;
import org.opendatakit.httpclientandroidlib.entity.mime.content.FileBody;
import org.opendatakit.httpclientandroidlib.entity.mime.content.StringBody;
import org.opendatakit.httpclientandroidlib.message.BasicNameValuePair;
import org.opendatakit.httpclientandroidlib.protocol.HttpContext;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

/**
 * Background task for uploading completed forms.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceUploaderTask extends AsyncTask<Object, Integer, InstanceUploaderTask.Outcome> {

    private static final String t = "InstanceUploaderTask";
    // it can take up to 27 seconds to spin up Aggregate
    private static final int CONNECTION_TIMEOUT = 60000;
    private static final String fail = "Error: ";

    private InstanceUploaderListener mStateListener;

    private static final String SMS_SEND_ACTION = "CTS_SMS_SEND_ACTION";
    private static final String SMS_DELIVERY_ACTION = "CTS_SMS_DELIVERY_ACTION";

    private static final int TIME_OUT = 1000 * 10;

    public static class Outcome {
        public Uri mAuthRequestingServer = null;
        public HashMap<String, String> mResults = new HashMap<String,String>();
    }

    private static String getFileContents(final File file) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final StringBuilder stringBuilder = new StringBuilder();

        boolean done = false;

        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                stringBuilder.append(line);
            }
        }

        reader.close();
        inputStream.close();

        return stringBuilder.toString();
    }

    /**
     * Uploads to urlString the submission identified by id with filepath of instance
     * @param gateway - Phone number to receive the SMS
     * @param id
     * @param instanceFilePath
     * @param toUpdate - Instance URL for recording status update.
     * @throws InterruptedException
     */
    private boolean smsOneSubmission(String gateway, String id, String instanceFilePath, Uri toUpdate, Outcome outcome) throws InterruptedException {

    	String smsFileContent = "";

    	Collect.getInstance().getActivityLogger().logAction(this, gateway, instanceFilePath);

        File instanceFile = new File(instanceFilePath);
        ContentValues cv = new ContentValues();

        // Check gateway phone number
        if (gateway == null || gateway.equals("") || !PhoneNumberUtils.isGlobalPhoneNumber(gateway)) {
        	// invalid phone number
			System.err.println("Invalid phone number for SMS gateway: " + gateway );
			outcome.mResults.put(
				id,
				fail
				+ "Invalid SMS gateway phone number: "
				+ gateway);
			cv.put(InstanceColumns.STATUS,
					InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
				Collect.getInstance().getContentResolver()
			    .update(toUpdate, cv, null, null);
			return true;
        }

        // find all files in parent directory
        File[] allFiles = instanceFile.getParentFile().listFiles();

        // add media files
        for (File f : allFiles) {
            String fileName = f.getName();

            int dotIndex = fileName.lastIndexOf(".");
            String extension = "";
            if (dotIndex != -1) {
                extension = fileName.substring(dotIndex + 1);
            }

            if (fileName.startsWith(".")) {
                // ignore invisible files
                continue;
            }
            if (extension.equals("sms") || fileName.equals(instanceFile.getName() + ".txt")) {
            	// get contents of file
              try {
              	smsFileContent = getFileContents(f);
              } catch (IOException e) {
              	e.printStackTrace();	// Auto-generated catch block
              }

            	// prepare contents
        	    SmsManager smsManager = SmsManager.getDefault();
        	    ArrayList<String> parts = smsManager.divideMessage(smsFileContent);
        	    int numParts = parts.size();

        	    // prepare responses
        	    SmsBroadcastReceiver mSendReceiver;
        	    SmsBroadcastReceiver mDeliveryReceiver;

        	    mSendReceiver = new SmsBroadcastReceiver(SMS_SEND_ACTION);
        	    mDeliveryReceiver = new SmsBroadcastReceiver(SMS_DELIVERY_ACTION);

        	    Intent mSendIntent;
        	    Intent mDeliveryIntent;

        	    mSendIntent = new Intent(SMS_SEND_ACTION);
        	    mDeliveryIntent = new Intent(SMS_DELIVERY_ACTION);

        	    IntentFilter sendIntentFilter = new IntentFilter(SMS_SEND_ACTION);
        	    IntentFilter deliveryIntentFilter = new IntentFilter(SMS_DELIVERY_ACTION);

        	    Collect.getInstance().getApplicationContext().registerReceiver(mSendReceiver, sendIntentFilter);
        	    Collect.getInstance().getApplicationContext().registerReceiver(mDeliveryReceiver, deliveryIntentFilter);

        	    ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
        	    ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

        	    for (int i = 0; i < numParts; i++) {
        	    	sentIntents.add(PendingIntent.getBroadcast(Collect.getInstance().getApplicationContext(), 0, mSendIntent, 0));
        	    	deliveryIntents.add(PendingIntent.getBroadcast(Collect.getInstance().getApplicationContext(), 0, mDeliveryIntent, 0));
        	    }

        	    // Send Message, in parts if necessary
        	    if (smsFileContent.length() > 0) {
        	    	smsManager.sendMultipartTextMessage(gateway, null, parts, sentIntents, deliveryIntents);
        	    	mSendReceiver.waitForCalls(numParts, TIME_OUT);
        	    	mDeliveryReceiver.waitForCalls(numParts, TIME_OUT);

        	    	Set <String> errors = new HashSet <String>();

        	    	if (hasErrors(mSendReceiver, errors, "Send") || hasErrors(mDeliveryReceiver, errors, "Delivery")) {
        	    		String strErrors = "";
        	    		for (String s : errors) {
        	    		    strErrors += " " + s + ";";
        	    		}
        				System.err.println("SMS not sent:" + strErrors );
        				outcome.mResults.put(
        					id,
        					fail
        					+ "SMS not sent: "
        					+ strErrors );
        				cv.put(InstanceColumns.STATUS,
        						InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
        					Collect.getInstance().getContentResolver()
        				    .update(toUpdate, cv, null, null);
        				return true;
        	    	}
        	    }
        	    else {	// trying to send an empty message
        			System.err.println("No content found");
        			outcome.mResults.put(
        				id,
        				fail
        				+ "No content found");
        			cv.put(InstanceColumns.STATUS,
        					InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
        				Collect.getInstance().getContentResolver()
        			    .update(toUpdate, cv, null, null);
        			return true;
        	    }

            } else {
                continue; // ignore other files
            }
        }

        if (smsFileContent.length() == 0) {	// File was not found
			System.err.println("Form does not have SMS data");
			outcome.mResults.put(
				id,
				fail
				+ "Form does not have SMS data");
			cv.put(InstanceColumns.STATUS,
					InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
				Collect.getInstance().getContentResolver()
			    .update(toUpdate, cv, null, null);
			return true;
        }
        else {	// SUCCESS! if it got here, it must have worked
	        outcome.mResults.put(id, Collect.getInstance().getString(R.string.success));
	        cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMITTED);
	        Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
	        return true;
        }
    }

    /**
     * Gets the Results from sending SMS
     * @param receiver - Broadcast receiver to check results
     * @param errors - Errors messages will be added to this set
     * @param action - Name of receiver, used only in console output
     * @return true if errors were found, false if no errors were found
     * @throws InterruptedException
     */
    private boolean hasErrors(SmsBroadcastReceiver receiver,
			Set<String> errors, String action) {

    	String errorMsg = "";

    	// cycle through the results for each message part
    	for (int i = 0; i < receiver.mResultCodes.size(); i++ ) {
			switch ( receiver.mResultCodes.get(i) ) {
			case Activity.RESULT_OK:
			    continue;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				errorMsg = "Unknown error";
		        break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				errorMsg = "No service";
			    break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				errorMsg = "Null PDU";
			    break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				errorMsg = "Airplane mode";
			    break;
			}
			System.err.println(action + " error for message part " + i + ": " + errorMsg);
			errors.add(errorMsg);
    	}
    	if (errorMsg.length() == 0) {
    		return false;	// no errors
    	}
    	else {
    		return true;	// yes, errors
    	}
	}

	/**
     * Determine whether the SMS should be used as the payload
     * @return true if the payload should be the SMS file contents.
     * @author Marc Abbyad (marc@medicmobile.org)
     */
    private boolean isUploadPayloadSms(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
        String protocol = settings.getString(PreferencesActivity.KEY_PROTOCOL,
				Collect.getInstance().getString(R.string.protocol_odk_default));

        // Use SMS file as payload only if Plaform is "Other" and "Upload SMS payload" is selected
        if (protocol.equals(Collect.getInstance().getString(R.string.protocol_medic_mobile))
        		&& settings.getBoolean(PreferencesActivity.KEY_SMS_UPLOAD,
        				Collect.getInstance().getResources().getBoolean(R.bool.default_upload_sms)))
        {
            return true;
        }
        return false;
    }

	/**
     * Prepares the SMS payload to be uploaded
     * @param httppost
     * @param instanceSmsFile - file whose contents are to be uploaded
     * @param errorMessage - response message
     * @return false if the payload was not successfully created.
     * @author Marc Abbyad (marc@medicmobile.org)
     */
    private boolean prepareSmsPayload(HttpPost httppost, File instanceSmsFile, StringBuilder errorMessage) {


        Long timestamp = System.currentTimeMillis();
        String message = "";
        String from = PreferenceManager.getDefaultSharedPreferences(
        					Collect.getInstance()).getString(PreferencesActivity.KEY_OWN_PHONE_NUMBER,
        							Collect.getInstance().getString(R.string.default_own_phone_number) );

        if (from.trim().equals("")) {
        	errorMessage.append(Collect.getInstance().getString(R.string.own_phone_number_missing_error));
        	return false;
        }
        try {
			message = getFileContents(instanceSmsFile);
		} catch (IOException e) {
			e.printStackTrace();
            Log.e(t, e.toString());
            errorMessage.append("Form File Not Found: " + instanceSmsFile.toString());
            return false;
		}

        try {
            //Post Data
            List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
        	nameValuePair.add(new BasicNameValuePair("sent_timestamp", timestamp.toString()));
        	nameValuePair.add(new BasicNameValuePair("message", message));
        	nameValuePair.add(new BasicNameValuePair("from", from));

            //Encoding POST data
        	httppost.setEntity(new UrlEncodedFormEntity(nameValuePair, "utf-8"));

        	//Set header to match content
        	httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        } catch (UnsupportedEncodingException e) {
            // log exception
            e.printStackTrace();
            Log.e(t, e.toString());
            errorMessage.append("Failed to create post data");
            return false;
        }
        return true;
    }
	/**
     * Uploads to urlString the submission identified by id with filepath of instance
     * @param urlString destination URL
     * @param id
     * @param instanceFilePath
     * @param toUpdate - Instance URL for recording status update.
     * @param httpclient - client connection
     * @param localContext - context (e.g., credentials, cookies) for client connection
     * @param uriRemap - mapping of Uris to avoid redirects on subsequent invocations
     * @return false if credentials are required and we should terminate immediately.
     */
    private boolean uploadOneSubmission(String urlString, String id, String instanceFilePath,
    			Uri toUpdate, HttpContext localContext, Map<Uri, Uri> uriRemap, Outcome outcome) {

    	Collect.getInstance().getActivityLogger().logAction(this, urlString, instanceFilePath);

        File instanceFile = new File(instanceFilePath);
        ContentValues cv = new ContentValues();
        Uri u = Uri.parse(urlString);
        HttpClient httpclient = WebUtils.createHttpClient(CONNECTION_TIMEOUT);
        HttpPost httppost = WebUtils.createOpenRosaHttpPost(u);

        // check if we should upload SMS or the default XML
        Boolean uploadSMS = false;
        if (isUploadPayloadSms()) {
        	uploadSMS = true;

        	// check and prepare the data for upload
        	StringBuilder errorMsg = new StringBuilder();
        	if (!prepareSmsPayload(httppost, new File(instanceFile + ".txt"), errorMsg))
        	{
    			Log.d(t, errorMsg.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, Collect.getInstance().getString(R.string.error_prefix) + errorMsg);
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
        	}
        	Collect.getInstance().getActivityLogger().logAction(this, urlString, "Payload prepared for SMS upload");
        }

        boolean openRosaServer = false;
        if (uriRemap.containsKey(u)) {
            // we already issued a head request and got a response,
            // so we know the proper URL to send the submission to
            // and the proper scheme. We also know that it was an
            // OpenRosa compliant server.
            openRosaServer = true;
            u = uriRemap.get(u);

            // if https then enable preemptive basic auth...
            if ( u.getScheme().equals("https") ) {
            	WebUtils.enablePreemptiveBasicAuth(localContext, u.getHost());
            }

            Log.i(t, "Using Uri remap for submission " + id + ". Now: " + u.toString());
        } else {

            // if https then enable preemptive basic auth...
            if ( u.getScheme() != null && u.getScheme().equals("https") ) {
            	WebUtils.enablePreemptiveBasicAuth(localContext, u.getHost());
            }

            // we need to issue a head request
            HttpHead httpHead = WebUtils.createOpenRosaHttpHead(u);

            // prepare response
            HttpResponse response = null;
            try {
                Log.i(t, "Issuing HEAD request for " + id + " to: " + u.toString());

                response = httpclient.execute(httpHead, localContext);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            		// clear the cookies -- should not be necessary?
            		Collect.getInstance().getCookieStore().clear();

                	WebUtils.discardEntityBytes(response);
            		// we need authentication, so stop and return what we've
                    // done so far.
                	outcome.mAuthRequestingServer = u;
                    return false;
                } else if (statusCode == 204) {
                	Header[] locations = response.getHeaders("Location");
                	WebUtils.discardEntityBytes(response);
                    if (locations != null && locations.length == 1) {
                        try {
                            Uri uNew = Uri.parse(URLDecoder.decode(locations[0].getValue(), "utf-8"));
                            if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
                                openRosaServer = true;
                                // trust the server to tell us a new location
                                // ... and possibly to use https instead.
                                uriRemap.put(u, uNew);
                                u = uNew;
                            } else {
                                // Don't follow a redirection attempt to a different host.
                                // We can't tell if this is a spoof or not.
                            	outcome.mResults.put(
                                    id,
                                    fail
                                            + "Unexpected redirection attempt to a different host: "
                                            + uNew.toString());
                                cv.put(InstanceColumns.STATUS,
                                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                                Collect.getInstance().getContentResolver()
                                        .update(toUpdate, cv, null, null);
                                return true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            outcome.mResults.put(id, fail + urlString + " " + e.toString());
                            cv.put(InstanceColumns.STATUS,
                                InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                            Collect.getInstance().getContentResolver()
                                    .update(toUpdate, cv, null, null);
                            return true;
                        }
                    }
                } else {
                    // may be a server that does not handle
                	WebUtils.discardEntityBytes(response);

                    Log.w(t, "Status code on Head request: " + statusCode);

                    if (!uploadSMS && statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
                    	outcome.mResults.put(
                            id,
                            fail
                                    + "Invalid status code on Head request.  If you have a web proxy, you may need to login to your network. ");
                        cv.put(InstanceColumns.STATUS,
                            InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                        Collect.getInstance().getContentResolver()
                                .update(toUpdate, cv, null, null);
                        return true;
                    }
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, fail + "Client Protocol Exception");
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            } catch (ConnectTimeoutException e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, fail + "Connection Timeout");
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, fail + e.toString() + " :: Network Connection Failed");
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, fail + "Connection Timeout");
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            } catch (HttpHostConnectException e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                outcome.mResults.put(id, fail + "Network Connection Refused");
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(t, e.toString());
                WebUtils.clearHttpConnectionManager();
                String msg = e.getMessage();
                if (msg == null) {
                    msg = e.toString();
                }
                outcome.mResults.put(id, fail + "Generic Exception: " + msg);
                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                return true;
            }
        }

        // At this point, we may have updated the uri to use https.
        // This occurs only if the Location header keeps the host name
        // the same. If it specifies a different host name, we error
        // out.
        //
        // And we may have set authentication cookies in our
        // cookiestore (referenced by localContext) that will enable
        // authenticated publication to the server.
        //

        if (uploadSMS) {

            //making POST request
            try {
            	HttpResponse response = httpclient.execute(httppost, localContext);
                // write response to log
                Log.d("Http Post Response:", response.getStatusLine().toString());

                int responseCode = response.getStatusLine().getStatusCode();
                WebUtils.discardEntityBytes(response);


	            if (responseCode != HttpStatus.SC_OK && responseCode != HttpStatus.SC_CREATED && responseCode != HttpStatus.SC_ACCEPTED) {
	            	if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
	            		// clear the cookies -- should not be necessary?
	                	Collect.getInstance().getCookieStore().clear();
	                	outcome.mResults.put(id, fail + response.getStatusLine().getReasonPhrase()
	                            + " (" + responseCode + ") at " + urlString);
	                } else {
	                	outcome.mResults.put(id, fail + response.getStatusLine().getReasonPhrase()
	                            + " (" + responseCode + ") at " + urlString);
	                }
	                cv.put(InstanceColumns.STATUS,
	                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
	                Collect.getInstance().getContentResolver()
	                        .update(toUpdate, cv, null, null);
	                return true;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            Log.e(t, e.toString());
	            WebUtils.clearHttpConnectionManager();
	            String msg = e.getMessage();
	            if (msg == null) {
	                msg = e.toString();
	            }
	            outcome.mResults.put(id, fail + "Generic Exception: " + msg);
	            cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
	            Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
	            return true;
	        }
        }
        else {
	        // get instance file

	        // Under normal operations, we upload the instanceFile to
	        // the server.  However, during the save, there is a failure
	        // window that may mark the submission as complete but leave
	        // the file-to-be-uploaded with the name "submission.xml" and
	        // the plaintext submission files on disk.  In this case,
	        // upload the submission.xml and all the files in the directory.
	        // This means the plaintext files and the encrypted files
	        // will be sent to the server and the server will have to
	        // figure out what to do with them.
	        File submissionFile = new File(instanceFile.getParentFile(), "submission.xml");
	        if ( submissionFile.exists() ) {
	            Log.w(t, "submission.xml will be uploaded instead of " + instanceFile.getAbsolutePath());
	        } else {
	            submissionFile = instanceFile;
	        }

	        if (!instanceFile.exists() && !submissionFile.exists()) {
	        	outcome.mResults.put(id, fail + "instance XML file does not exist!");
	            cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
	            Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
	            return true;
	        }

	        // find all files in parent directory
	        File[] allFiles = instanceFile.getParentFile().listFiles();

	        // add media files
	        List<File> files = new ArrayList<File>();
	        for (File f : allFiles) {
	            String fileName = f.getName();

	            int dotIndex = fileName.lastIndexOf(".");
	            String extension = "";
	            if (dotIndex != -1) {
	                extension = fileName.substring(dotIndex + 1);
	            }

	            if (fileName.startsWith(".")) {
	                // ignore invisible files
	                continue;
	            }
	            if (fileName.equals(instanceFile.getName())) {
	                continue; // the xml file has already been added
	            } else if (fileName.equals(submissionFile.getName())) {
	                continue; // the xml file has already been added
	            } else if (openRosaServer) {
	                files.add(f);
	            } else if (extension.equals("jpg")) { // legacy 0.9x
	                files.add(f);
	            } else if (extension.equals("3gpp")) { // legacy 0.9x
	                files.add(f);
	            } else if (extension.equals("3gp")) { // legacy 0.9x
	                files.add(f);
	            } else if (extension.equals("mp4")) { // legacy 0.9x
	                files.add(f);
	            } else {
	                Log.w(t, "unrecognized file type " + f.getName());
	            }
	        }

	        boolean first = true;
	        int j = 0;
	        int lastJ;
	        while (j < files.size() || first) {
	        	lastJ = j;
	            first = false;

	            httppost = WebUtils.createOpenRosaHttpPost(u);

	            MimeTypeMap m = MimeTypeMap.getSingleton();

	            long byteCount = 0L;

	            // mime post
	            MultipartEntity entity = new MultipartEntity();

	            // add the submission file first...
	            FileBody fb = new FileBody(submissionFile, "text/xml");
	            entity.addPart("xml_submission_file", fb);
	            Log.i(t, "added xml_submission_file: " + submissionFile.getName());
	            byteCount += submissionFile.length();

	            for (; j < files.size(); j++) {
	                File f = files.get(j);
	                String fileName = f.getName();
	                int idx = fileName.lastIndexOf(".");
	                String extension = "";
	                if (idx != -1) {
	                    extension = fileName.substring(idx + 1);
	                }
	                String contentType = m.getMimeTypeFromExtension(extension);

	                // we will be processing every one of these, so
	                // we only need to deal with the content type determination...
	                if (extension.equals("xml")) {
	                    fb = new FileBody(f, "text/xml");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added xml file " + f.getName());
	                } else if (extension.equals("jpg")) {
	                    fb = new FileBody(f, "image/jpeg");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added image file " + f.getName());
	                } else if (extension.equals("3gpp")) {
	                    fb = new FileBody(f, "audio/3gpp");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added audio file " + f.getName());
	                } else if (extension.equals("3gp")) {
	                    fb = new FileBody(f, "video/3gpp");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added video file " + f.getName());
	                } else if (extension.equals("mp4")) {
	                    fb = new FileBody(f, "video/mp4");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added video file " + f.getName());
	                } else if (extension.equals("csv")) {
	                    fb = new FileBody(f, "text/csv");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added csv file " + f.getName());
	                } else if (f.getName().endsWith(".amr")) {
	                    fb = new FileBody(f, "audio/amr");
	                    entity.addPart(f.getName(), fb);
	                    Log.i(t, "added audio file " + f.getName());
	                } else if (extension.equals("xls")) {
	                    fb = new FileBody(f, "application/vnd.ms-excel");
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t, "added xls file " + f.getName());
	                } else if (contentType != null) {
	                    fb = new FileBody(f, contentType);
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.i(t,
	                        "added recognized filetype (" + contentType + ") " + f.getName());
	                } else {
	                    contentType = "application/octet-stream";
	                    fb = new FileBody(f, contentType);
	                    entity.addPart(f.getName(), fb);
	                    byteCount += f.length();
	                    Log.w(t, "added unrecognized file (" + contentType + ") " + f.getName());
	                }

	                // we've added at least one attachment to the request...
	                if (j + 1 < files.size()) {
	                    if ((j-lastJ+1 > 100) || (byteCount + files.get(j + 1).length() > 10000000L)) {
	                        // the next file would exceed the 10MB threshold...
	                        Log.i(t, "Extremely long post is being split into multiple posts");
	                        try {
	                            StringBody sb = new StringBody("yes", Charset.forName("UTF-8"));
	                            entity.addPart("*isIncomplete*", sb);
	                        } catch (Exception e) {
	                            e.printStackTrace(); // never happens...
	                        }
	                        ++j; // advance over the last attachment added...
	                        break;
	                    }
	                }
	            }

	            httppost.setEntity(entity);

	            // prepare response and return uploaded
	            HttpResponse response = null;
	            try {
	                Log.i(t, "Issuing POST request for " + id + " to: " + u.toString());
	                response = httpclient.execute(httppost, localContext);
	                int responseCode = response.getStatusLine().getStatusCode();
	                WebUtils.discardEntityBytes(response);

	                Log.i(t, "Response code:" + responseCode);
	                // verify that the response was a 201 or 202.
	                // If it wasn't, the submission has failed.
	                if (responseCode != HttpStatus.SC_CREATED && responseCode != HttpStatus.SC_ACCEPTED) {
	                    if (responseCode == HttpStatus.SC_OK) {
	                    	outcome.mResults.put(id, fail + "Network login failure? Again?");
	                    } else if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
	                		// clear the cookies -- should not be necessary?
	                    	Collect.getInstance().getCookieStore().clear();
	                    	outcome.mResults.put(id, fail + response.getStatusLine().getReasonPhrase()
	                                + " (" + responseCode + ") at " + urlString);
	                    } else {
	                    	outcome.mResults.put(id, fail + response.getStatusLine().getReasonPhrase()
	                                + " (" + responseCode + ") at " + urlString);
	                    }
	                    cv.put(InstanceColumns.STATUS,
	                        InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
	                    Collect.getInstance().getContentResolver()
	                            .update(toUpdate, cv, null, null);
	                    return true;
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	                Log.e(t, e.toString());
	                WebUtils.clearHttpConnectionManager();
	                String msg = e.getMessage();
	                if (msg == null) {
	                    msg = e.toString();
	                }
	                outcome.mResults.put(id, fail + "Generic Exception: " + msg);
	                cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
	                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
	                return true;
	            }
	        }
        }

        // if it got here, it must have worked
        outcome.mResults.put(id, Collect.getInstance().getString(R.string.success));
        cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMITTED);
        Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
        return true;
    }

    // TODO: This method is like 350 lines long, down from 400.
    // still. ridiculous. make it smaller.

    protected Outcome doInBackground(Object... params) {
    	String uploadMethod = null;
    	if (params[0] != null) {	// protect against converting null
    		uploadMethod = (String) params[0];
    	}
    	Long[] values = (Long[]) params[1];

    	Outcome outcome = new Outcome();

        String selection = InstanceColumns._ID + "=?";
        String[] selectionArgs = new String[(values == null) ? 0 : values.length];
        if ( values != null ) {
	        for (int i = 0; i < values.length; i++) {
	            if (i != values.length - 1) {
	                selection += " or " + InstanceColumns._ID + "=?";
	            }
	            selectionArgs[i] = values[i].toString();
	        }
        }

        String deviceId = new PropertyManager(Collect.getInstance().getApplicationContext())
        						.getSingularProperty(PropertyManager.OR_DEVICE_ID_PROPERTY);
        
        Collect.setCredentials();

        // get shared HttpContext so that authentication and cookies are retained.
        HttpContext localContext = Collect.getInstance().getHttpContext();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());

        Map<Uri, Uri> uriRemap = new HashMap<Uri, Uri>();

        Cursor c = null;
        try {
        	c = Collect.getInstance().getContentResolver()
                    .query(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);

	        if (c.getCount() > 0) {
	            c.moveToPosition(-1);
	            while (c.moveToNext()) {
	                if (isCancelled()) {
	                    return outcome;
	                }
	                publishProgress(c.getPosition() + 1, c.getCount());
	                String instance = c.getString(c.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
	                String id = c.getString(c.getColumnIndex(InstanceColumns._ID));
	                Uri toUpdate = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, id);

	                if(uploadMethod != null && uploadMethod.equals(FormEntryActivity.KEY_UPLOAD_METHOD_SMS)) {
		                String gateway = settings.getString(PreferencesActivity.KEY_SMS_GATEWAY,
		                    				Collect.getInstance().getString(R.string.default_sms_gateway));

		                try {
							if ( !smsOneSubmission(gateway, id, instance, toUpdate, outcome) ) {
							   	return outcome; // get credentials...
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	                else {	// Upload via HTTP
		                int subIdx = c.getColumnIndex(InstanceColumns.SUBMISSION_URI);
		                String urlString = c.isNull(subIdx) ? null : c.getString(subIdx);
		                if (urlString == null) {
		                    urlString = settings.getString(PreferencesActivity.KEY_SERVER_URL,
		                    				Collect.getInstance().getString(R.string.default_server_url));
		                    if ( urlString.charAt(urlString.length()-1) == '/') {
		                    	urlString = urlString.substring(0, urlString.length()-1);
		                    }
		                    // NOTE: /submission must not be translated! It is the well-known path on the server.
		                    String submissionUrl =
		                        settings.getString(PreferencesActivity.KEY_SUBMISSION_URL,
		                        		Collect.getInstance().getString(R.string.default_odk_submission));
		                    if ( submissionUrl.charAt(0) != '/') {
		                    	submissionUrl = "/" + submissionUrl;
		                    }

		                    urlString = urlString + submissionUrl;
		                }

		                // add the deviceID to the request...
		                try {
							urlString += "?deviceID=" + URLEncoder.encode(deviceId, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// unreachable...
						}

		                if ( !uploadOneSubmission(urlString, id, instance, toUpdate, localContext, uriRemap, outcome) ) {
		                	return outcome; // get credentials...
		                }
	                }
	            }
	        }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return outcome;
    }



/**
 * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
 * @param context Context reference to get the TelephonyManager instance from
 * @return country code or null
 */
public static String getUserCountry(Context context) {
    try {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String simCountry = tm.getSimCountryIso();
        if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
            return simCountry.toLowerCase(Locale.US);
        }
        else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
            String networkCountry = tm.getNetworkCountryIso();
            if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                return networkCountry.toLowerCase(Locale.US);
            }
        }
    }
    catch (Exception e) { }
    return null;
}



    @Override
    protected void onPostExecute(Outcome outcome) {
        synchronized (this) {
            if (mStateListener != null) {
                if (outcome.mAuthRequestingServer != null) {
                    mStateListener.authRequest(outcome.mAuthRequestingServer, outcome.mResults);
                } else {
                    mStateListener.uploadingComplete(outcome.mResults);

                    StringBuilder selection = new StringBuilder();
                    Set<String> keys = outcome.mResults.keySet();
                    Iterator<String> it = keys.iterator();

                    String[] selectionArgs = new String[keys.size()+1];
                    int i = 0;
                    selection.append("(");
                    while (it.hasNext()) {
                        String id = it.next();
                        selection.append(InstanceColumns._ID + "=?");
                        selectionArgs[i++] = id;
                        if (i != keys.size()) {
                            selection.append(" or ");
                        }
                    }
                    selection.append(") and status=?");
                    selectionArgs[i] = InstanceProviderAPI.STATUS_SUBMITTED;

                    Cursor results = null;
                    try {
                        results = Collect
                                .getInstance()
                                .getContentResolver()
                                .query(InstanceColumns.CONTENT_URI, null, selection.toString(),
                                        selectionArgs, null);
                        if (results.getCount() > 0) {
                            Long[] toDelete = new Long[results.getCount()];
                            results.moveToPosition(-1);

                            int cnt = 0;
                            while (results.moveToNext()) {
                                toDelete[cnt] = results.getLong(results
                                        .getColumnIndex(InstanceColumns._ID));
                                cnt++;
                            }

                            boolean deleteFlag = PreferenceManager.getDefaultSharedPreferences(
                                    Collect.getInstance().getApplicationContext()).getBoolean(
                                    PreferencesActivity.KEY_DELETE_AFTER_SEND, false);
                            if (deleteFlag) {
                                DeleteInstancesTask dit = new DeleteInstancesTask();
                                dit.setContentResolver(Collect.getInstance().getContentResolver());
                                dit.execute(toDelete);
                            }

                        }
                    } finally {
                        if (results != null) {
                            results.close();
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (this) {
            if (mStateListener != null) {
                // update progress and total
                mStateListener.progressUpdate(values[0].intValue(), values[1].intValue());
            }
        }
    }


    public void setUploaderListener(InstanceUploaderListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }


    public static void copyToBytes(InputStream input, OutputStream output,
            int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        int bytesRead = input.read(buf);
        while (bytesRead != -1) {
            output.write(buf, 0, bytesRead);
            bytesRead = input.read(buf);
        }
        output.flush();
    }
    private static class SmsBroadcastReceiver extends BroadcastReceiver {
    	private int mCalls;
    	private int mExpectedCalls;
    	private String mAction;
    	private Object mLock;
    	private ArrayList<Integer> mResultCodes = new ArrayList<Integer>();

    	SmsBroadcastReceiver(String action) {
	    	mAction = action;
	    	reset();
	    	mLock = new Object();
    	}
    	void reset() {
	    	mExpectedCalls = Integer.MAX_VALUE;
	    	mCalls = 0;
    	}
    	@Override
    	public void onReceive(Context context, Intent intent) {
	    	if (intent.getAction().equals(mAction)) {
		    	synchronized (mLock) {
			    	mCalls += 1;
			    	mResultCodes.add(getResultCode());
			    	if (mCalls >= mExpectedCalls) {
			    		mLock.notify();
			    	}
		    	}
	    	}
    	}
    	public void waitForCalls(int expectedCalls, long timeout) throws InterruptedException {
	    	synchronized(mLock) {
		    	mExpectedCalls = expectedCalls;
		    	if (mCalls < mExpectedCalls) {
		    		mLock.wait(timeout);
		    	}
	    	}
    	}
	}
}
