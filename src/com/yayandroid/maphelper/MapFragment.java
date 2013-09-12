package com.yayandroid.maphelper;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.SupportMapFragment;
import com.yayandroid.utility.MapHelperFragment;

public class MapFragment extends MapHelperFragment {

	private static MapFragment instance;

	private View contentHolder;
	private TextView textView;
	private String locText;

	public static MapFragment getInstance() {
		if (instance == null)
			instance = new MapFragment();
		return instance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View content = inflater
				.inflate(R.layout.fragment_map, container, false);

		contentHolder = (View) content.findViewById(R.id.mapContent);
		textView = (TextView) content.findViewById(R.id.mapLocationText);

		setProgressView(content.findViewById(R.id.mapProgress));
		setMapHost((FrameLayout) content.findViewById(R.id.mapHost));

		// This is just to show how acts when finding mapView takes long time,
		// you don't really need to use postDelayed
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// This method initializes map controls
				setMapFragment((SupportMapFragment) getActivity()
						.getSupportFragmentManager().findFragmentById(
								R.id.mapFragment));
			}
		}, 1000);

		return content;

	}

	@Override
	public void onMapReady() {
		super.onMapReady();

		contentHolder.setVisibility(View.VISIBLE);
		moveToMyLocation(true);
	}

	@Override
	public void noMapFound(boolean errorAlreadyDisplayed) {
		super.noMapFound(errorAlreadyDisplayed);

		contentHolder.setVisibility(View.VISIBLE);
		if (errorAlreadyDisplayed)
			textView.setText(getActivity().getString(R.string.map_error));
		else
			textView.setText(getActivity()
					.getString(R.string.map_error_unknown));
	}

	@Override
	public void onMyLocationReady() {
		super.onMyLocationReady();

		getlocationInfo(LocInfoType.COUNTRY, LocInfoType.CITY);

	}

	@Override
	public void onLocationInfoReceived(LocInfoType locInfo, String locValue) {
		super.onLocationInfoReceived(locInfo, locValue);

		switch (locInfo) {
		case COUNTRY: {
			if (TextUtils.isEmpty(locText))
				locText = locValue;
			else
				locText = locValue + " - " + locText;
			break;
		}
		case CITY: {
			if (TextUtils.isEmpty(locText))
				locText = locValue;
			else
				locText += " - " + locValue;
			break;
		}
		default:
			break;
		}

		textView.setText(locText);
	}

}
