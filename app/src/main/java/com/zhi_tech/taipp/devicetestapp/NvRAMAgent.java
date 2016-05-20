package com.zhi_tech.taipp.devicetestapp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Created by taipp on 5/20/2016.
 */
public interface NvRAMAgent extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements NvRAMAgent
    {
        private static final String DESCRIPTOR = "NvRAMAgent";
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an NvRAMAgent interface,
         * generating a proxy if needed.
         */

        public static NvRAMAgent asInterface(IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }

            IInterface iin = (IInterface)obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof NvRAMAgent))) {
                return ((NvRAMAgent)iin);
            }

            return new NvRAMAgent.Stub.Proxy(obj);
        }

        public IBinder asBinder()
        {
            return this;
        }
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException
        {
            switch (code)
            {
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }

                case TRANSACTION_readFile:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    byte[] _result = this.readFile(_arg0);
                    reply.writeNoException();
                    reply.writeByteArray(_result);
                    return true;
                }

                case TRANSACTION_writeFile:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    byte[] _arg1;
                    _arg1 = data.createByteArray();
                    int _result = this.writeFile(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
            }

            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements NvRAMAgent
        {
            private IBinder mRemote;
            Proxy(IBinder remote)
            {
                mRemote = remote;
            }

            public IBinder asBinder()
            {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }

            public byte[] readFile(int file_lid) throws RemoteException
            {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                byte[] _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(file_lid);
                    mRemote.transact(Stub.TRANSACTION_readFile, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createByteArray();
                }

                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public int writeFile(int file_lid, byte[] buff) throws RemoteException
            {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(file_lid);
                    _data.writeByteArray(buff);
                    mRemote.transact(Stub.TRANSACTION_writeFile, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
        static final int TRANSACTION_readFile = (IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_writeFile = (IBinder.FIRST_CALL_TRANSACTION + 1);
    }

    public byte[] readFile(int file_lid) throws RemoteException;
    public int writeFile(int file_lid, byte[] buff) throws RemoteException;

}
