package com.spaceapps.sensornavigator;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.*;

public class MainActivity extends Activity
	implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

	private static final String TAG = "MainActivity";
	private TextView        mTextView;
	private GoogleApiClient mGoogleApiClient;

	@Override protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		final WatchViewStub stub = (WatchViewStub) findViewById( R.id.watch_view_stub );
		stub.setOnLayoutInflatedListener(
			new WatchViewStub.OnLayoutInflatedListener()
			{
				@Override public void onLayoutInflated( WatchViewStub stub )
				{
					mTextView = (TextView) stub.findViewById( R.id.text );
				}
			}
		);

		mGoogleApiClient = new GoogleApiClient.Builder( this ).addApi( Wearable.API )
		                                                      .addConnectionCallbacks( this )
		                                                      .addOnConnectionFailedListener( this )
		                                                      .build();


		if ( !mGoogleApiClient.isConnected() )
		{
			mGoogleApiClient.connect();
		}

		/*Wearable.NodeApi.getLocalNode( mGoogleApiClient ).setResultCallback(
			new ResultCallback<NodeApi.GetLocalNodeResult>()
			{
				@Override public void onResult( NodeApi.GetLocalNodeResult getLocalNodeResult )
				{
					Uri uri = new Uri.Builder().scheme( "wear" ).path( Consts.PATH_CONFIG ).authority(
						getLocalNodeResult.getNode().getId()
					).build();

					Wearable.DataApi.getDataItem( mGoogleApiClient, uri ).setResultCallback(
						new ResultCallback<DataApi.DataItemResult>()
						{
							@Override public void onResult( DataApi.DataItemResult dataItemResult )
							{
								if ( dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null )
								{
									fetchConfig(
										DataMapItem.fromDataItem( dataItemResult.getDataItem() ).getDataMap()
									);
								}
							}
						}
					);
				}
			}
		);*/

	}

	@Override public void onConnected( final Bundle bundle )
	{
		mTextView.setText( "1. Phone Connected." );

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
					mTextView.setText( "4. Message From : " + messageEvent.getPath() );

					Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					long[] vibrationPattern = {0, 500, 50, 300};
					//-1 - don't repeat
					final int indexInPatternToRepeat = -1;
					vibrator.vibrate( vibrationPattern, indexInPatternToRepeat );
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
	}

	@Override public void onConnectionSuspended( final int i )
	{
		mTextView.setText( "3. Connection Suspended." );
	}

	@Override public void onConnectionFailed( final ConnectionResult connectionResult )
	{
		mTextView.setText( "2. Connection Failed." );
	}
}
