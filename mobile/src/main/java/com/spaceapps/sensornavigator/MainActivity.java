package com.spaceapps.sensornavigator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.*;


public class MainActivity extends ActionBarActivity
	implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

	private static final String TAG = "MainActivity";

	private static final String KEY_IN_RESOLUTION = "is_in_resolution";

	/**
	 * Request code for auto Google Play Services error resolution.
	 */
	protected static final int REQUEST_CODE_RESOLUTION = 1;
	private static final   int REQUEST_ENABLE_BT       = 2;

	/**
	 * Google API client.
	 */
	private GoogleApiClient mGoogleApiClient;

	/**
	 * Determines if the client is in a resolution state, and
	 * waiting for resolution intent to return.
	 */
	private boolean mIsInResolution;
	private BluetoothAdapter m_btAdapter;

	private boolean mScanning;
	private Handler mHandler = new Handler();


	/**
	 * Called when the activity is starting. Restores the activity state.
	 */
	@Override protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		if ( savedInstanceState != null )
		{
			mIsInResolution = savedInstanceState.getBoolean( KEY_IN_RESOLUTION, false );
		}
		else
		{
			getSupportFragmentManager().beginTransaction().add( R.id.main_container, new MainFragment() ).commit();
		}

		BluetoothManager btManager = (BluetoothManager) getSystemService( Context.BLUETOOTH_SERVICE );

		m_btAdapter = btManager.getAdapter();

		if ( m_btAdapter != null && !m_btAdapter.isEnabled() )
		{
			Intent enableIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
			startActivityForResult( enableIntent, REQUEST_ENABLE_BT );
		}
		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if ( !getPackageManager().hasSystemFeature( PackageManager.FEATURE_BLUETOOTH_LE ) )
		{
			Toast.makeText( this, R.string.ble_not_supported, Toast.LENGTH_SHORT ).show();
			finish();
		}
	}

	private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
	{
		@Override public void onLeScan( final BluetoothDevice device, final int rssi, final byte[] scanRecord )
		{
			// your implementation here
			Log.d( "MainActivity", String.format( "onLeScan : device = %s", device.getName() ) );
		}
	};


	/**
	 * Called when the Activity is made visible.
	 * A connection to Play Services need to be initiated as
	 * soon as the activity is visible. Registers {@code ConnectionCallbacks}
	 * and {@code OnConnectionFailedListener} on the
	 * activities itself.
	 */
	@Override protected void onStart()
	{
		super.onStart();
		if ( mGoogleApiClient == null )
		{
			mGoogleApiClient = new GoogleApiClient.Builder( this )
				// Optionally, add additional APIs and scopes if required.
				.addApi( Wearable.API ).addConnectionCallbacks( this ).addOnConnectionFailedListener( this ).build();
		}
		mGoogleApiClient.connect();

//		scanLeDevice( true );

	}

	/**
	 * Called when activity gets invisible. Connection to Play Services needs to
	 * be disconnected as soon as an activity is invisible.
	 */
	@Override protected void onStop()
	{
		if ( mGoogleApiClient != null )
		{
			mGoogleApiClient.disconnect();
		}

		super.onStop();
	}

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	private void scanLeDevice( final boolean enable )
	{
		if ( enable )
		{
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(
				new Runnable()
				{
					@Override public void run()
					{
						mScanning = false;
						m_btAdapter.stopLeScan( leScanCallback );
						Toast.makeText( MainActivity.this, R.string.ble_scanning_stop, Toast.LENGTH_SHORT ).show();
					}
				}, SCAN_PERIOD
			);

			mScanning = true;
			m_btAdapter.startLeScan( leScanCallback );
			Toast.makeText( this, R.string.ble_scanning, Toast.LENGTH_SHORT ).show();
		}
		else
		{
			mScanning = false;
			m_btAdapter.stopLeScan( leScanCallback );
			Toast.makeText( this, R.string.ble_scanning_stop, Toast.LENGTH_SHORT ).show();
		}
	}

	/**
	 * Saves the resolution state.
	 */
	@Override protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );
		outState.putBoolean( KEY_IN_RESOLUTION, mIsInResolution );
	}

	/**
	 * Handles Google Play Services resolution callbacks.
	 */
	@Override protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );
		switch ( requestCode )
		{
			case REQUEST_CODE_RESOLUTION:
				retryConnecting();
				break;
		}
	}

	private void retryConnecting()
	{
		mIsInResolution = false;
		if ( !mGoogleApiClient.isConnecting() )
		{
			mGoogleApiClient.connect();
		}
	}

	/**
	 * Called when {@code mGoogleApiClient} is connected.
	 */
	@Override public void onConnected( Bundle connectionHint )
	{
		Log.i( TAG, "GoogleApiClient connected" );
		// Start making API requests.
		Wearable.NodeApi.addListener(
			mGoogleApiClient, new NodeApi.NodeListener()
			{
				@Override public void onPeerConnected( Node node )
				{
					Log.d( TAG, "A node is connected and its id: " + node.getId() );
				}

				@Override public void onPeerDisconnected( Node node )
				{
					Log.d( TAG, "A node is disconnected and its id: " + node.getId() );
				}
			}
		);

		Wearable.MessageApi.addListener(
			mGoogleApiClient, new MessageApi.MessageListener()
			{
				@Override public void onMessageReceived( MessageEvent messageEvent )
				{
					Log.d( TAG, "You have a message from " + messageEvent.getPath() );
				}
			}
		);

		Wearable.DataApi.addListener(
			mGoogleApiClient, new DataApi.DataListener()
			{
				@Override public void onDataChanged( DataEventBuffer dataEvents )
				{
					Log.d( TAG, "Your data is changed" );
				}
			}
		);

		Wearable.NodeApi.getLocalNode( mGoogleApiClient ).setResultCallback(
			new ResultCallback<NodeApi.GetLocalNodeResult>()
			{
				@Override public void onResult( NodeApi.GetLocalNodeResult result )
				{
					Log.d( TAG, "My node id is " + result.getNode().getId() );
				}
			}
		);


	}

	public void sendMessage( final View v )
	{
		// TODO Send message to watch.

		Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).setResultCallback(
			new ResultCallback<NodeApi.GetConnectedNodesResult>()
			{
				@Override public void onResult( NodeApi.GetConnectedNodesResult result )
				{
					for ( Node node : result.getNodes() )
					{
						Log.d( TAG, "Node " + node.getId() + " is connected" );

						// TODO send a message to wearable


						Wearable.MessageApi.sendMessage(
							mGoogleApiClient,
							node.getId(),
							"/example/test",
							null
						).setResultCallback(
							new ResultCallback<MessageApi.SendMessageResult>()
							{
								@Override public void onResult( MessageApi.SendMessageResult sendMessageResult )
								{
									Log.d( TAG, "SendUpdateMessage: " + sendMessageResult.getStatus() );
								}
							}
						);

					}
				}
			}
		);
	}

	/**
	 * Called when {@code mGoogleApiClient} connection is suspended.
	 */
	@Override public void onConnectionSuspended( int cause )
	{
		Log.i( TAG, "GoogleApiClient connection suspended" );
		retryConnecting();
	}

	/**
	 * Called when {@code mGoogleApiClient} is trying to connect but failed.
	 * Handle {@code result.getResolution()} if there is a resolution
	 * available.
	 */
	@Override public void onConnectionFailed( ConnectionResult result )
	{
		Log.i( TAG, "GoogleApiClient connection failed: " + result.toString() );
		if ( !result.hasResolution() )
		{
			// Show a localized error dialog.
			GooglePlayServicesUtil.getErrorDialog(
				result.getErrorCode(), this, 0, new OnCancelListener()
				{
					@Override public void onCancel( DialogInterface dialog )
					{
						retryConnecting();
					}
				}
			).show();
			return;
		}
		// If there is an existing resolution error being displayed or a resolution
		// activity has started before, do nothing and wait for resolution
		// progress to be completed.
		if ( mIsInResolution )
		{
			return;
		}
		mIsInResolution = true;
		try
		{
			result.startResolutionForResult( this, REQUEST_CODE_RESOLUTION );
		}
		catch ( SendIntentException e )
		{
			Log.e( TAG, "Exception while starting resolution activity", e );
			retryConnecting();
		}
	}
}
