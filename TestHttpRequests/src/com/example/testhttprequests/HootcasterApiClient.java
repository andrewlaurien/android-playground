package com.example.testhttprequests;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.example.testhttprequests.LoginHandler.LoginError;
import com.google.common.base.Preconditions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

public class HootcasterApiClient {
	private static final String TAG = "HootcasterApiClient";

	private static final String HOST = "api.hootcaster.com";
	private static final String DEV_HOST = "10.0.2.2"; // adb-connected localhost on android

	private final Context context;
	private final AsyncHttpClient asyncHttpClient;

	public HootcasterApiClient(Context context) {
		this.context = context;
		this.asyncHttpClient = new AsyncHttpClient();
		this.asyncHttpClient.setCookieStore(new PersistentCookieStore(this.context));

		if (isDevEnvironment()) {
			// accept certificates from all hostnames
			KeyStore trustStore;
			try {
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			} catch (KeyStoreException ex) {
				throw new RuntimeException(ex);
			}
			try {
				trustStore.load(null, null);
			} catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException(ex);
			} catch (CertificateException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			SSLSocketFactory socketFactory;
			try {
				socketFactory = new AcceptAllSSLSocketFactory(trustStore);
			} catch (KeyManagementException ex) {
				throw new RuntimeException(ex);
			} catch (UnrecoverableKeyException ex) {
				throw new RuntimeException(ex);
			} catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException(ex);
			} catch (KeyStoreException ex) {
				throw new RuntimeException(ex);
			}
			socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			this.asyncHttpClient.setSSLSocketFactory(socketFactory);
		}
	}

	private static boolean IS_DEV_ENVIRONMENT = true; // TODO automatically determine this
	private boolean isDevEnvironment() {
		return IS_DEV_ENVIRONMENT;
	}

	private void jsonPost(
			final String path, final boolean isHttps,
			final JSONObject json, final AsyncHttpResponseHandler handler) {
		final String url = getUrl(path, isHttps);
		HttpEntity httpEntity;
		try {
			httpEntity = new StringEntity(json.toString(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex); // shouldn't ever happen
		}

		Log.i(TAG, "URL: " + url);
		Log.i(TAG, "Data: " + json.toString());
		asyncHttpClient.post(
				context, url, 
				httpEntity, "application/json", 
				handler
				);
	}

	private String getUrl(String path, boolean isHttps) {
		final String scheme = isHttps ? "https" : "http";
		final String host = isDevEnvironment() ? DEV_HOST : HOST;
		return String.format("%s://%s/v1/%s", scheme, host, path);
	}

	public void createAccount(
			final String username, final String password,
			final String fullname, final String registrationId,
			final String email, final String phone, final LoginHandler loginHandler) {

		JSONObject json = new JSONObject();
		try {
			json.put("username", Preconditions.checkNotNull(username));
			json.put("password", Preconditions.checkNotNull(password));
			json.put("fullname", Preconditions.checkNotNull(fullname));
			json.put("registration_id", Preconditions.checkNotNull(registrationId));
			json.put("email", Preconditions.checkNotNull(email));

			if (phone != null)
				json.put("phone", phone);

		} catch (JSONException ex) {
			throw new RuntimeException(ex); // shouldn't ever happen
		}

		jsonPost("account/create", true, json, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, final String response) {
				Log.e(TAG, "Status code: " + statusCode);
				Log.e(TAG, "Got response: " + response);

				JSONObject jsonResponse;
				try {
					jsonResponse = new JSONObject(response);
				} catch (JSONException ex) {
					throw new RuntimeException(ex);
				}

				boolean okay;
				try {
					okay = jsonResponse.getBoolean("okay");
				} catch (JSONException ex) {
					throw new RuntimeException(ex);
				}

				if (okay) {
					loginHandler.handleSuccess();
				} else {
					JSONArray jsonErrors;
					try {
						jsonErrors = jsonResponse.getJSONArray("errors");
					} catch (JSONException ex) {
						throw new RuntimeException(ex);
					}

					EnumSet<LoginError> errors = EnumSet.noneOf(LoginError.class);
					for (int i = 0; i < jsonErrors.length(); i++) {
						String error;
						try {
							error = jsonErrors.getString(i);
						} catch (JSONException ex) {
							throw new RuntimeException(ex);
						}

						if ("username_exists".equals(error))
							errors.add(LoginError.USERNAME_EXISTS);
						else if ("email_exists".equals(error))
							errors.add(LoginError.EMAIL_EXISTS);
						else if ("registration_id_exists".equals(error))
							errors.add(LoginError.REGISTRATION_ID_EXISTS);
						else if ("phone_exists".equals(error))
							errors.add(LoginError.PHONE_EXISTS);
						else
							throw new RuntimeException("Unrecognized error string: " + error);
					}
					loginHandler.handleErrors(errors);
				}
			}

			@Override
			public void onFailure(final Throwable ex, final String response) {
				Log.e(TAG, "Failure: " + ex);
				Log.e(TAG, "Response: " + response);

				if (ex instanceof ConnectException)
					loginHandler.handleConnectionFailure();
				else
					throw new RuntimeException(ex);
			}
		});
	}

	private static class AcceptAllSSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public AcceptAllSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}
}