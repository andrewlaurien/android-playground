package com.example.testhttprequests;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testhttprequests.api.HootcasterApiClient;
import com.example.testhttprequests.api.handlers.account.CreateAccountHandler;
import com.example.testhttprequests.api.handlers.account.LoginHandler;
import com.example.testhttprequests.api.handlers.contact.ContactsHandler;
import com.example.testhttprequests.api.handlers.contact.FindContactsHandler;
import com.example.testhttprequests.api.handlers.contact.ModifyContactsHandler;
import com.example.testhttprequests.api.models.Contact;
import com.example.testhttprequests.api.models.MatchedContact;
import com.example.testhttprequests.api.models.PotentialContact;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private HootcasterApiClient client;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.client = new HootcasterApiClient(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private String getTextFieldValue(int viewId) {
		return ((TextView)findViewById(viewId)).getText().toString();
	}

	public void onCreateClick(View view) {
		String registrationId = Long.toString((new Random()).nextLong());

		client.createAccount(
				getTextFieldValue(R.id.create_user), getTextFieldValue(R.id.create_pass),
				getTextFieldValue(R.id.create_emailaddress), registrationId,
				new CreateAccountHandler() {
					@Override
					public void handleSuccess() {
						Toast.makeText(getApplication(), "Logged in as: " + getTextFieldValue(R.id.create_user), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleErrors(final EnumSet<CreateAccountError> errors) {
						Toast.makeText(getApplication(), "Login failed: " + errors.toString(), Toast.LENGTH_LONG).show();
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}
				});
	}

	public void onLoginClick(View view) {
		String registrationId = Long.toString((new Random()).nextLong());

		client.login(
				getTextFieldValue(R.id.login_username), getTextFieldValue(R.id.login_password),
				registrationId,
				new LoginHandler() {
					@Override
					public void handleSuccess() {
						Toast.makeText(getApplication(), "Logged in as: " + getTextFieldValue(R.id.login_username), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleErrors(final EnumSet<LoginError> errors) {
						Toast.makeText(getApplication(), "Login failed: " + errors.toString(), Toast.LENGTH_LONG).show();
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}
				});
	}

	public void onContactsClick(View view) {
		client.allContacts(
				new ContactsHandler() {
					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}

					@Override
					public void handleNeedsLogin() {
						Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleSuccess(List<Contact> contacts) {
						Toast.makeText(getApplication(), "Contacts: " + contacts, Toast.LENGTH_SHORT).show();
					}
				});
	}

	public void onBlockedContactsClick(View view) {
		client.blockedContacts(
				new ContactsHandler() {
					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}

					@Override
					public void handleNeedsLogin() {
						Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleSuccess(List<Contact> contacts) {
						Toast.makeText(getApplication(), "Blocked contacts: " + contacts, Toast.LENGTH_SHORT).show();
					}
				});
	}

	public void onBlockAllClick(View view) {
		client.allContacts(
				new ContactsHandler() {
					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}

					@Override
					public void handleNeedsLogin() {
						Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleSuccess(List<Contact> contacts) {
						List<String> usernames = 
								Lists.transform(contacts, new Function<Contact, String>() {
									@Override
									public String apply(Contact contact) {
										return contact.getUsername();
									}
								});
						Toast.makeText(getApplication(), "Got " + usernames + ". Blocking!", Toast.LENGTH_SHORT).show();
						if (!usernames.isEmpty()) {
							client.blockContacts(
									usernames,
									new ModifyContactsHandler() {
										@Override
										public void handleConnectionFailure() {
											throw new RuntimeException("connection failure?!");
										}

										@Override
										public void handleUnknownException(Throwable ex) {
											throw new RuntimeException(ex);
										}

										@Override
										public void handleErrors(
												EnumSet<ModifyContactsError> errors) {
											Toast.makeText(getApplication(), "Errors: " + errors, Toast.LENGTH_SHORT).show();
										}

										@Override
										public void handleNeedsLogin() {
											Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();										
										}

										@Override
										public void handleSuccess() {
											Toast.makeText(getApplication(), "Totes blocked 'em", Toast.LENGTH_SHORT).show();
										}
									});
						}
					}
				});
	}
	public void onUnblockAllClick(View view) {
		client.blockedContacts(
				new ContactsHandler() {
					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}

					@Override
					public void handleNeedsLogin() {
						Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void handleSuccess(List<Contact> contacts) {
						List<String> usernames = 
								Lists.transform(contacts, new Function<Contact, String>() {
									@Override
									public String apply(Contact contact) {
										return contact.getUsername();
									}
								});
						Toast.makeText(getApplication(), "Got " + usernames + ". Unblocking!", Toast.LENGTH_SHORT).show();
						if (!usernames.isEmpty()) {
							client.unblockContacts(
									usernames,
									new ModifyContactsHandler() {
										@Override
										public void handleConnectionFailure() {
											throw new RuntimeException("connection failure?!");
										}

										@Override
										public void handleUnknownException(Throwable ex) {
											throw new RuntimeException(ex);
										}

										@Override
										public void handleErrors(
												EnumSet<ModifyContactsError> errors) {
											Toast.makeText(getApplication(), "Errors: " + errors, Toast.LENGTH_SHORT).show();
										}

										@Override
										public void handleNeedsLogin() {
											Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();										
										}

										@Override
										public void handleSuccess() {
											Toast.makeText(getApplication(), "Totes unblocked 'em", Toast.LENGTH_SHORT).show();
										}
									});
						}
					}
				});
	}

	public void onFindContactsClick(View view) {
		client.findContacts(
				ImmutableMap.of(
						"BaabyKennedy", new PotentialContact("baaby@testusers.com", "1234567890"),
						"MuttonDamon", new PotentialContact("mutton@testusers.com", "9876543210"),
						"SeaBlocked", new PotentialContact("cblocked@testusers.com", "4567891230"),
						"EmmBlocked", new PotentialContact("mblocked@testusers.com", null),
						"UhOhSpaghettios", new PotentialContact("chef@boyardee.com", "9871234560")
						),
						new FindContactsHandler() {

					@Override
					public void handleConnectionFailure() {
						throw new RuntimeException("connection failure?!");
					}

					@Override
					public void handleUnknownException(Throwable ex) {
						throw new RuntimeException(ex);
					}

					@Override
					public void handleNeedsLogin() {
						Toast.makeText(getApplication(), "Needs login!", Toast.LENGTH_SHORT).show();										
					}

					@Override
					public void handleSuccess(
							List<MatchedContact> matchedContacts) {
						Toast.makeText(getApplication(), "Matched " + matchedContacts.size() + " contacts!", Toast.LENGTH_SHORT).show();
						for (int i = 1; i <= matchedContacts.size(); i++)
							Toast.makeText(getApplication(), i + ") " + matchedContacts.get(i-1).toString(), Toast.LENGTH_SHORT).show();
					}
				});
	}
}
