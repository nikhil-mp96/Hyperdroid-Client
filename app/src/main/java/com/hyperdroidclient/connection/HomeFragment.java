package com.hyperdroidclient.connection;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hyperdroidclient.R;
import com.hyperdroidclient.androidVNC.COLORMODEL;
import com.hyperdroidclient.androidVNC.ConnectionBean;
import com.hyperdroidclient.androidVNC.IntroTextDialog;
import com.hyperdroidclient.androidVNC.MostRecentBean;
import com.hyperdroidclient.androidVNC.Utils;
import com.hyperdroidclient.androidVNC.VncCanvasActivity;
import com.hyperdroidclient.androidVNC.VncConstants;
import com.hyperdroidclient.androidVNC.VncDatabase;
import com.hyperdroidclient.androidVNC.androidVNC;
import com.hyperdroidclient.common.BaseFragment;
import com.hyperdroidclient.dashboard.MainActivity;
import com.hyperdroidclient.data.local.SharedPreferenceManager;
import com.hyperdroidclient.data.local.remote.User;
import com.hyperdroidclient.data.local.remote.VirtualMachine;
import com.hyperdroidclient.security.Decryption;
import com.hyperdroidclient.widgets.BaseButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by Archish on 10/16/2017.
 */

public class HomeFragment extends BaseFragment {
    BaseButton bVNC;
    private ConnectionBean selected;
    VncDatabase database;

    public static int inteval=1000;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        bVNC = (BaseButton) rootView.findViewById(R.id.bVnc);
        bVNC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                setSelectedFromView();
            }

        });
        database = new VncDatabase(getActivity());
        return rootView;
    }


    public void onStart() {
        super.onStart();
        FirebaseDatabase database;
        final DatabaseReference mDatabase;
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference();

        final ArrayList<VirtualMachine> Devices = new ArrayList<>();

        mDatabase.child("Refresh_Interval").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                inteval = ((Long)dataSnapshot.getValue()).intValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("HyperDroid-Assignment" , databaseError.toString());
            }
        });
        arriveOnPage();
    }

    private void setSelectedFromView() {
        FirebaseDatabase database;
        final DatabaseReference mDatabase;
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference();

        final ArrayList<VirtualMachine> Devices = new ArrayList<>();

        mDatabase.child("Refresh_Interval").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                inteval = ((Long)dataSnapshot.getValue()).intValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("HyperDroid-Assignment" , databaseError.toString());
            }
        });

        Query query = mDatabase.child("VirtualMachine").orderByValue();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for( DataSnapshot dataSnapshot1: dataSnapshot.getChildren() )
                    Devices.add(dataSnapshot1.getValue(VirtualMachine.class));
                Log.i("HyperDroid-Assignment","All Available "+Devices);
                Log.i("HyperDroid-Assignment","Connected At " + MainActivity.SSID);

                Iterator<VirtualMachine> i = Devices.iterator();
                while (i.hasNext())
                {
                    VirtualMachine vm = (VirtualMachine)i.next();
                    if(!vm.getSSID().equals(MainActivity.SSID))
                        i.remove();
                }
                Log.i("HyperDroid-Assignment","All Reachable "+Devices);
                Log.i("HyperDroid-Assignment","Refresh interval time " + inteval);

                i = Devices.iterator();
                while (i.hasNext())
                {
                    VirtualMachine vm = (VirtualMachine)i.next();
                    if(!isAlive(vm.getTimeStamp() , inteval ))
                        i.remove();
                }
                Log.i("HyperDroid-Assignment","Alive VM in Region " + Devices );
                final ArrayList<User> Users = new ArrayList<>();
                Query query2 = mDatabase.child("Users").orderByValue();

                query2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for( DataSnapshot dataSnapshot1: dataSnapshot.getChildren() )
                            Users.add(dataSnapshot1.getValue(User.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                i = Devices.iterator();
                while( i.hasNext() ) {
                    VirtualMachine V = (VirtualMachine)i.next();
                    Iterator i2 = Users.iterator();
                    while (i2.hasNext()) {
                        User U = (User) i2.next();
                        if (V.getVMID().equals(U.getVMID()) && checkActivity(U.getSessionEnds()))
                        {
                            i.remove();
                            break;
                        }

                    }
                }
                Log.i("HyperDroid-Assignment","Finally Available VMS " + Devices );
                if (Devices.size() == 0 )
                {
                    dismissProgressDialog();
                    Toast.makeText(getActivity(),"Please wait for sometime until service is available",Toast.LENGTH_LONG).show();
                }
                else {
                    selected.setAddress(Devices.get(0).getAddress());
                    selected.setPort(Integer.parseInt(Devices.get(0).getPort()));
                    selected.setUseLocalCursor(true);
                    selected.setColorModel("C256");
                    mDatabase.child("Users").child(new SharedPreferenceManager(getActivity().getApplicationContext()).getAccessToken()).child("vmid").setValue(Devices.get(0).getVMID());
                    String password = "";
                    try {
                        password = Decryption.decryptPassword(Devices.get(0).getHash());
                        Log.d("VMPassword", password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    selected.setPassword(password);
                    selected.setKeepPassword(true);
                    new SharedPreferenceManager(getActivity().getApplicationContext()).saveVMKey(Devices.get(0).getVMID());
                    dismissProgressDialog();
                    canvasStart();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    private boolean checkActivity( String dataSnapshot )
    {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date now = new Date();
            Date TimeStamp = sdf.parse(dataSnapshot);
            if( TimeStamp.after(now))
                return true;
        }
        catch (Exception E)
        {}
        return false;
    }

    private boolean isAlive( String dataSnapshot , int interval) {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date now = new Date();
            Date TimeStamp = sdf.parse(dataSnapshot);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(TimeStamp);
            if(Math.abs(calendar.getTimeInMillis()-calendar1.getTimeInMillis()) <= interval )
                return true;
        }
        catch (Exception E)
        {
        }
        return false;
    }

    private void canvasStart() {
        // Log.d("canvasStart()",Utils.getMemoryInfo(this).toString());
        if (selected == null) return;
        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(getActivity());
        if (info.lowMemory) {
            // Low Memory situation.  Prompt.
            Utils.showYesNoPrompt(getActivity(), "Continue?", "Android reports low system memory.\nContinue with VNC connection?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    vnc();
                }
            }, null);
        } else
            vnc();
    }

    private void vnc() {
        saveAndWriteRecent();
        Intent intent = new Intent(getActivity(), VncCanvasActivity.class);
        Log.d("VncConstants", selected.Gen_getValues().toString());
        intent.putExtra(VncConstants.CONNECTION, selected.Gen_getValues());
        startActivity(intent);


    }

    private void saveAndWriteRecent() {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            selected.save(db);
            MostRecentBean mostRecent = getMostRecent(db);
            if (mostRecent == null) {
                mostRecent = new MostRecentBean();
                mostRecent.setConnectionId(selected.get_Id());
                mostRecent.Gen_insert(db);
            } else {
                mostRecent.setConnectionId(selected.get_Id());
                mostRecent.Gen_update(db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void arriveOnPage() {
        ArrayList<ConnectionBean> connections = new ArrayList<>();
        ConnectionBean.getAll(database.getReadableDatabase(), ConnectionBean.GEN_TABLE_NAME, connections, ConnectionBean.newInstance);
        Collections.sort(connections);
        connections.add(0, new ConnectionBean());
        int connectionIndex = 0;
        if (connections.size() > 1) {
            MostRecentBean mostRecent = getMostRecent(database.getReadableDatabase());
            if (mostRecent != null) {
                for (int i = 1; i < connections.size(); ++i) {
                    if (connections.get(i).get_Id() == mostRecent.getConnectionId()) {
                        connectionIndex = i;
                        break;
                    }
                }
            }
        }
        connections.toArray(new ConnectionBean[connections.size()]);
        selected = connections.get(connectionIndex);
        IntroTextDialog.showIntroTextIfNecessary(getActivity(), database);
    }

    static MostRecentBean getMostRecent(SQLiteDatabase db) {
        ArrayList<MostRecentBean> recents = new ArrayList<>(1);
        MostRecentBean.getAll(db, MostRecentBean.GEN_TABLE_NAME, recents, MostRecentBean.GEN_NEW);
        if (recents.size() == 0)
            return null;
        return recents.get(0);
    }


}
