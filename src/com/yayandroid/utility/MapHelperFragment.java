/**
 * 
 */
package com.yayandroid.utility;

import java.util.List;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * @author y.bayramoglu
 * 
 */
public class MapHelperFragment extends Fragment {

	private final int CHECK_MY_LOCATION_DELAY = 500;
	private final int REQUEST_CODE_PLAY_SERVICE = 111;
	private final int MAP_ZOOM_LEVEL = 10;
	private final String PROGRESS_MESSAGE_GETTING_LOCATION = "Getting location...";
	private final String TAG_ERROR_DIALOG_FRAGMENT = "Error_Dialog";

	public GoogleMap mapView;
	public Location myLocation;

	private ProgressDialog progressDialog;
	private View progressView;
	private FrameLayout mapHost;
	private SupportMapFragment mapFragment;
	private Handler delayHandler;
	private boolean progressDialogCancelable = true;
	private boolean shouldMoveToMyLocation = false;
	private boolean runningThreadCheckMap = false;
	private boolean runningThreadGetMyLocation = false;
	private boolean runningThreadGetLocationInfo = false;
	private LocInfoHolder[] requiredInformations;

	/**
	 * Enumeration to determine which value is onLocationInfoReceived method. In
	 * this enumeration values may be needed to change according to Country
	 * address standards.
	 * 
	 * @author y.bayramoglu
	 * 
	 */
	public enum LocInfoType {

		ROUTE("route"), NEIGHBORHOOD("neighborhood"), LOCALITY("locality"), COUNTY(
				"administrative_area_level_2"), CITY(
				"administrative_area_level_1"), COUNTRY("country");

		public final String value;

		private LocInfoType(String value) {
			this.value = value;
		}

	}

	/**
	 * Holder class to keep information about a locInfoType has been sent or not
	 * 
	 * @author y.bayramoglu
	 * 
	 */
	private class LocInfoHolder {

		public boolean hasSent = false;
		public LocInfoType type;

		public LocInfoHolder(LocInfoType type) {
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			return type == ((LocInfoHolder) o).type;
		}

	}

	/**
	 * Optional: But if you set this view, its visibility will be set to gone
	 * when mapView has received
	 */
	public void setProgressView(View progress) {
		this.progressView = progress;
	}

	/**
	 * If progressView has also been set, then this view's visibility will be
	 * set to visible when mapView has received. Otherwise it will not be used
	 */
	public void setMapHost(FrameLayout mapHost) {
		this.mapHost = mapHost;
		if (mapHost != null) {
			mapHost.requestTransparentRegion(mapHost);
		}
	}

	/**
	 * This method needs to be set as latest, because this method launches a
	 * thread, which is intended to find mapView and to notify when it is able
	 * or unable to find one
	 */
	public void setMapFragment(SupportMapFragment mapFrag) {
		this.mapFragment = mapFrag;
		this.delayHandler = new Handler();
		CheckMap();
	}

	/**
	 * Changes cancelable attribute of progressDialog, by default it is true
	 */
	public void setProgressDialogCancelable(boolean cancelable) {
		this.progressDialogCancelable = cancelable;
		if (progressDialog != null)
			progressDialog.setCancelable(cancelable);
	}

	/**
	 * This method needs to be called onMapReady, otherwise it will throw
	 * nullPointerException because of map is not initialized yet. It animates
	 * map to myLocation whenever it is detected.
	 */
	public void moveToMyLocation(boolean shouldDisplayProgressDialog) {
		shouldMoveToMyLocation = true;
		GetMyLocation(shouldDisplayProgressDialog);
	}

	/**
	 * This method needs to be called onMapReady, otherwise it will throw
	 * nullPointerException because of map is not initialized yet. It starts a
	 * thread which looks for myLocation, and calls onMyLocationReady statement
	 * when it finds.
	 */
	public void getMyLocation(boolean shouldDisplayProgressDialog) {
		shouldMoveToMyLocation = false;
		GetMyLocation(shouldDisplayProgressDialog);
	}

	/**
	 * Trigger method to start a thread which tries to get user's current city
	 * name, and posts onLocationInfoReceived method
	 */
	public void getlocationInfo(LocInfoType... types) {
		this.requiredInformations = new LocInfoHolder[types.length];
		for (int i = 0; i < types.length; i++) {
			requiredInformations[i] = new LocInfoHolder(types[i]);
		}
		GetLocationInfo();
	}

	/**
	 * Returns true if there is already found mapView, false otherwise
	 */
	public boolean isMapReady() {
		return mapView != null;
	}

	/**
	 * This method should be override to do mapView settings, it will be called
	 * automatically after setMapFragment method received supportMapFragment and
	 * if mapView has been found
	 */
	public void onMapReady() {
		if (progressView != null) {
			progressView.setVisibility(View.GONE);
			if (mapHost != null) {
				mapHost.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * This method should be override to display user error that there was no
	 * available MapView, it will be called automatically after setMapFragment
	 * method received supportMapFragment
	 */
	public void noMapFound(boolean errorAlreadyDisplayed) {
		if (progressView != null) {
			progressView.setVisibility(View.GONE);
		}
	}

	/**
	 * This method should be override if myLocation is necessary, and after
	 * calling super method myLocation can be used in any purpose
	 */
	public void onMyLocationReady() {
		if (shouldMoveToMyLocation) {
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
					new LatLng(myLocation.getLatitude(), myLocation
							.getLongitude()), MAP_ZOOM_LEVEL);
			mapView.animateCamera(cameraUpdate);
		}

		DismissProgressDialog();
	}

	/**
	 * If any of information about user's current location then this method
	 * needs to override, because when getlocationInfo method is called it will
	 * push data to this method with locInfoType.
	 */
	public void onLocationInfoReceived(LocInfoType locInfo, String locValue) {

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		/**
		 * Here onDestroyView we need to remove supportMapFragment, otherwise
		 * when recalling this fragment will cause crash because of something
		 * like mapFragment is not available. Little bit tricky.
		 * 
		 * I was removing mapFragment onDestroyView method but it started giving
		 * "you can't call fragmentTransaction.commit() after onSaveInstanceState"
		 * error, so i moved code here.
		 */
		if (mapFragment != null) {
			FragmentTransaction fragmentTransaction = getActivity()
					.getSupportFragmentManager().beginTransaction();
			fragmentTransaction.remove(mapFragment);
			fragmentTransaction.commit();
		}

		super.onSaveInstanceState(outState);
	}

	private void DismissProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing())
			progressDialog.dismiss();
	}

	private void ShowProgressDialog(String message) {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(getActivity());
			progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			progressDialog.setCancelable(progressDialogCancelable);
		}
		progressDialog.setMessage(message);
		if (getActivity() != null)
			progressDialog.show();
	}

	/**
	 * Waits until user's current location gets found, up to given flag it
	 * displays progressDialog to user or not. Here also can be display user
	 * some notification about "please turn on gps/wifi" or something.
	 */
	private void GetMyLocation(boolean shouldDisplayProgressDialog) {
		if (runningThreadGetMyLocation)
			return;

		if (shouldDisplayProgressDialog)
			ShowProgressDialog(PROGRESS_MESSAGE_GETTING_LOCATION);

		mapView.setMyLocationEnabled(true);

		new Thread(new Runnable() {

			@Override
			public void run() {
				runningThreadGetMyLocation = true;

				delayHandler.post(new Runnable() {

					@Override
					public void run() {
						myLocation = mapView.getMyLocation();
						if (myLocation == null) {
							delayHandler.postDelayed(this,
									CHECK_MY_LOCATION_DELAY);
						} else {
							if (getActivity() != null) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										onMyLocationReady();
									}
								});
							}
							runningThreadGetMyLocation = false;
						}
					}

				});

			}

		}).start();

	}

	/**
	 * After getting myLocation, this method tries to get user's current
	 * address. First with GeoCoder, if it cannot then it looks up googleApis to
	 * get address online and parseS it.
	 */
	private void GetLocationInfo() {
		if (runningThreadGetLocationInfo || myLocation == null)
			return;

		new Thread(new Runnable() {

			@SuppressLint("NewApi")
			@Override
			public void run() {
				runningThreadGetLocationInfo = true;

				Address address = null;

				if (Build.VERSION_CODES.FROYO < Build.VERSION.SDK_INT) {
					if (Geocoder.isPresent()) {
						try {
							if (getActivity() != null) {
								Geocoder geocoder = new Geocoder(getActivity(),
										Locale.getDefault());
								List<Address> addresses = geocoder
										.getFromLocation(
												myLocation.getLatitude(),
												myLocation.getLongitude(), 1);
								if (addresses.size() > 0) {
									address = addresses.get(0);
								}
							}
						} catch (Exception ignored) {
							/*
							 * After a while, GeoCoder start to throw
							 * "Service not available" exception. really weird
							 * since it was working before (same device, same
							 * Android version etc..)
							 */
						}
					}
				}

				if (address != null) {
					// i.e., GeoCoder success
					parseAddressInformation(address);
				} else {
					// i.e., GeoCoder failed
					fetchInformationUsingGoogleMap();
				}

				runningThreadGetLocationInfo = false;
			}

		}).start();
	}

	/**
	 * Parses found address object from GeoCoder, and posts informations
	 */
	private void parseAddressInformation(Address address) {

		/**
		 * This parsing also may need to be modified up to your country's
		 * addressing system
		 */

		String value = address.getSubLocality();
		if (value != null)
			PostInformation(LocInfoType.COUNTY, value);

		value = address.getLocality();
		if (value != null)
			PostInformation(LocInfoType.LOCALITY, value);

		value = address.getAdminArea();
		if (value != null)
			PostInformation(LocInfoType.CITY, value);

		value = address.getCountryName();
		if (value != null)
			PostInformation(LocInfoType.COUNTRY, value);

		// If there is still some fields to get information about, then post
		// googleMap api to get them
		boolean isThereAnyLeft = false;
		for (int i = 0; i < requiredInformations.length; i++) {
			if (!requiredInformations[i].hasSent) {
				isThereAnyLeft = true;
			}
		}

		if (isThereAnyLeft)
			fetchInformationUsingGoogleMap();

	}

	/**
	 * GoogleApis returns an address as jsonObject according to given
	 * coordinate, and here we parse it to find necessary fields
	 */
	private void fetchInformationUsingGoogleMap() {

		final AndroidHttpClient ANDROID_HTTP_CLIENT = AndroidHttpClient
				.newInstance(MapHelperFragment.class.getName());
		String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng="
				+ myLocation.getLatitude()
				+ ","
				+ myLocation.getLongitude()
				+ "&sensor=false&language=tr";
		try {
			JSONObject googleMapResponse = new JSONObject(
					ANDROID_HTTP_CLIENT.execute(new HttpGet(googleMapUrl),
							new BasicResponseHandler()));

			// many nested loops.. not great -> use expression instead
			// loop among all results
			JSONArray results = (JSONArray) googleMapResponse.get("results");
			for (int i = 0; i < results.length(); i++) {
				// loop among all addresses within this result
				JSONObject result = results.getJSONObject(i);
				if (result.has("address_components")) {
					JSONArray addressComponents = result
							.getJSONArray("address_components");

					for (int j = 0; j < addressComponents.length(); j++) {
						JSONObject addressComponent = addressComponents
								.getJSONObject(j);
						if (result.has("types")) {
							JSONArray types = addressComponent
									.getJSONArray("types");

							for (int k = 0; k < requiredInformations.length; k++) {

								for (int l = 0; l < types.length(); l++) {
									if (requiredInformations[k].type.value
											.equals(types.getString(l))) {
										if (addressComponent.has("long_name")) {
											PostInformation(
													requiredInformations[k].type,
													addressComponent
															.getString("long_name"));
										} else if (addressComponent
												.has("short_name")) {
											PostInformation(
													requiredInformations[k].type,
													addressComponent
															.getString("short_name"));
										}
									}

								}

							}

						}
					}
				}
			}
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
		ANDROID_HTTP_CLIENT.close();
	}

	/**
	 * Notify with LocInfoType and its value, and remove from required fields if
	 * necessary not to look for and post over and over again
	 */
	private void PostInformation(final LocInfoType type, final String value) {
		int requiredIndex = -1;
		for (int i = 0; i < requiredInformations.length; i++) {
			if (requiredInformations[i].type == type) {
				requiredIndex = i;
			}
		}

		if (requiredIndex != -1) {
			if (getActivity() != null
					&& !requiredInformations[requiredIndex].hasSent) {
				requiredInformations[requiredIndex].hasSent = true;
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onLocationInfoReceived(type, value);
					}

				});
			}
		}
	}

	/**
	 * Check Map availability and glEs version compatibility. If all is well,
	 * then calls onMapReady method otherwise calls noMapFound method
	 */
	private void CheckMap() {
		if (runningThreadCheckMap)
			return;

		new Thread(new Runnable() {

			@Override
			public void run() {
				runningThreadCheckMap = true;

				// Check GooglePlayService is available or not int
				int playServiceStatus = GooglePlayServicesUtil
						.isGooglePlayServicesAvailable(getActivity());

				if (playServiceStatus == ConnectionResult.SUCCESS) {
					// Google Play Services are available
					if (getVersionFromPackageManager(getActivity()) >= 2) {
						// OpenGL version is also okay
						GetMapView();
					} else {
						// OpenGL version is not supported
						if (getActivity() != null) {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									noMapFound(false);
								}
							});
						}
						runningThreadCheckMap = false;
					}
				} else if (GooglePlayServicesUtil
						.isUserRecoverableError(playServiceStatus)) {
					// Google Play Services are not available on current device
					new ErrorDialogFragment(playServiceStatus).show(
							getActivity().getSupportFragmentManager(),
							TAG_ERROR_DIALOG_FRAGMENT);
				} else {
					// Any other case that mapView couldn't get found
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								noMapFound(false);
							}
						});
					}
					runningThreadCheckMap = false;
				}

			}

		}).start();

	}

	/**
	 * Waits until map is ready
	 */
	private void GetMapView() {
		delayHandler.post(new Runnable() {

			@Override
			public void run() {
				mapView = mapFragment.getMap();
				if (mapView == null) {
					delayHandler.postDelayed(this, CHECK_MY_LOCATION_DELAY);
				} else {
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								onMapReady();
							}
						});
					}
					runningThreadCheckMap = false;
				}
			}

		});
	}

	@SuppressLint("ValidFragment")
	private class ErrorDialogFragment extends DialogFragment {

		private final String ARG_STATUS = "status";

		public ErrorDialogFragment(int status) {
			Bundle args = new Bundle();
			args.putInt(ARG_STATUS, status);
			this.setArguments(args);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();

			return GooglePlayServicesUtil.getErrorDialog(
					args.getInt(ARG_STATUS), getActivity(),
					REQUEST_CODE_PLAY_SERVICE);
		}

		@Override
		public void onDismiss(DialogInterface dlg) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						noMapFound(true);
					}
				});
			}
			runningThreadCheckMap = false;
		}
	}

	// following from
	// https://android.googlesource.com/platform/cts/+/master/tests/tests/graphics/src/android/opengl/cts/OpenGlEsVersionTest.java

	/*
	 * Copyright (C) 2010 The Android Open Source Project
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */

	private int getVersionFromPackageManager(Context context) {
		PackageManager packageManager = context.getPackageManager();
		FeatureInfo[] featureInfos = packageManager
				.getSystemAvailableFeatures();
		if (featureInfos != null && featureInfos.length > 0) {
			for (FeatureInfo featureInfo : featureInfos) {
				// Null feature name means this feature is the open
				// GLes version feature.
				if (featureInfo.name == null) {
					if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
						return getMajorVersion(featureInfo.reqGlEsVersion);
					} else {
						// Lack of property means OpenGL ES version 1
						return 1;
					}
				}
			}
		}
		return 1;
	}

	/** @see FeatureInfo#getGlEsVersion() */
	private int getMajorVersion(int glEsVersion) {
		return ((glEsVersion & 0xffff0000) >> 16);
	}

}