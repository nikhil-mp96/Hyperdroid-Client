package com.hyperdroidclient.data.local.remote;

import android.os.Parcel;
import android.os.Parcelable;

import com.hyperdroidclient.dashboard.MainActivity;

/**
 * Created by nikhi on 31-01-2018.
 */

public class User implements Parcelable {

    private String Machine;
    private String Name;
    private String VMID;
    private String SessionEnds;


    public User() {
    }

    public User(String Machine, String Name, String VMID, String SessionEnds) {
        this.Machine = Machine;
        this.Name = Name;
        this.VMID = VMID;
        this.SessionEnds = SessionEnds;
    }


    protected User(Parcel in) {
        Machine = in.readString();
        Name = in.readString();
        VMID = in.readString();
        SessionEnds = in.readString();

    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getMachine() {
        return Machine;
    }

    public void setMachine(String Machine) {
        this.Machine = Machine;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getVMID() {
        return VMID;
    }

    public void setVMID(String VMID) {
        this.VMID = VMID;
    }
    public String getSessionEnds() {
        return SessionEnds;
    }

    public void setSessionEnds(String sessionEnds) {
        SessionEnds = sessionEnds;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(Machine);
        parcel.writeString(Name);
        parcel.writeString(VMID);
        parcel.writeString(SessionEnds);

    }

    @Override
    public String toString() {
        return Name;
    }
}
