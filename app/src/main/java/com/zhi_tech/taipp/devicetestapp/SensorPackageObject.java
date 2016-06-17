package com.zhi_tech.taipp.devicetestapp;

/**
 * Created by taipp on 5/27/2016.
 */
public class SensorPackageObject {

    private char[] header;
    public GyroscopeSensor gyroscopeSensor;
    public AccelerometerSensor accelerometerSensor;
    public MagneticSensor magneticSensor;
    public TemperatureSensor temperatureSensor;
    public LightSensor lightSensor;
    public ProximitySensor proximitySensor;
    private long timestamp;

    public SensorPackageObject() {
        header = new char[2];
        gyroscopeSensor = new GyroscopeSensor();
        accelerometerSensor = new AccelerometerSensor();
        magneticSensor = new MagneticSensor();
        temperatureSensor = new TemperatureSensor();
        lightSensor = new LightSensor();
        proximitySensor = new ProximitySensor();
        timestamp = 0;
    }

    public void setHeader(char[] chars) {
        this.header = chars;
    }

    public void setTimestampValue(long timestampValue) {
        this.timestamp = timestampValue;
    }

    public char[] getHeader() {
        return header;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public class GyroscopeSensor {
        private short X;
        private short Y;
        private short Z;

        public GyroscopeSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(short x, short y, short z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public short getX() {
            return X;
        }
        public short getY() {
            return Y;
        }
        public short getZ() {
            return Z;
        }
    }

    public class AccelerometerSensor {
        private short X;
        private short Y;
        private short Z;

        public AccelerometerSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(short x, short y, short z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public short getX() {
            return X;
        }
        public short getY() {
            return Y;
        }
        public short getZ() {
            return Z;
        }
    }

    public class MagneticSensor {
        private short X;
        private short Y;
        private short Z;

        public MagneticSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(short x, short y, short z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public short getX() {
            return X;
        }
        public short getY() {
            return Y;
        }
        public short getZ() {
            return Z;
        }
    }

    public class TemperatureSensor {
        private short temperature;

        public TemperatureSensor() {
            this.temperature = 0;
        }

        public void setTemperature(short temperature) {
            this.temperature  = temperature;
        }

        public short getTemperature() {
            return temperature;
        }
    }

    public class LightSensor {
        private short lightSensorValue;

        public LightSensor() {
            this.lightSensorValue = 0;
        }

        public void setLightSensorValue(short lightSensorValue) {
            this.lightSensorValue = lightSensorValue;
        }

        public short getLightSensorValue() {
            return lightSensorValue;
        }
    }

    public class ProximitySensor {
        private short proximitySensorValue;

        public ProximitySensor() {
            this.proximitySensorValue = 0;
        }

        public void setProximitySensorValue(short proximitySensorValue) {
            this.proximitySensorValue = proximitySensorValue;
        }

        public short getProximitySensorValue() {
            return proximitySensorValue;
        }
    }


}
