package com.zhi_tech.taipp.devicetestapp;

import android.os.IBinder;
import android.os.IInterface;

/**
 * Created by taipp on 5/20/2016.
 */
public interface NvRAMBackupAgent extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements NvRAMBackupAgent
    {
        private static final java.lang.String DESCRIPTOR = "NvRAMBackupAgent";
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an NvRAMAgent interface,
         * generating a proxy if needed.
         */
        public static NvRAMBackupAgent asInterface(android.os.IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof NvRAMBackupAgent))) {
                return ((NvRAMBackupAgent)iin);
            }
            return new NvRAMBackupAgent.Stub.Proxy(obj);
        }
        public android.os.IBinder asBinder()
        {
            return this;
        }
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        {
            switch (code){
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_backupBinregionAll:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.testBackup();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
        private static class Proxy implements NvRAMBackupAgent
        {
            private android.os.IBinder mRemote;
            Proxy(android.os.IBinder remote)
            {
                mRemote = remote;
            }
            public android.os.IBinder asBinder()
            {
                return mRemote;
            }
            public java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }
            public int testBackup() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_backupBinregionAll, _data, _reply, 0);
                    _reply.readException();
                    _reply.createByteArray();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return 1;
            }
        }
        static final int TRANSACTION_backupBinregionAll = (IBinder.FIRST_CALL_TRANSACTION + 0);
    }
    public int testBackup() throws android.os.RemoteException;
}
