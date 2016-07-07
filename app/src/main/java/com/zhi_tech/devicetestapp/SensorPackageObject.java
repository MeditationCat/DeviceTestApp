package com.zhi_tech.devicetestapp;

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
    private int[] touchPadXY;

    public SensorPackageObject() {
        header = new char[2];
        gyroscopeSensor = new GyroscopeSensor();
        accelerometerSensor = new AccelerometerSensor();
        magneticSensor = new MagneticSensor();
        temperatureSensor = new TemperatureSensor();
        lightSensor = new LightSensor();
        proximitySensor = new ProximitySensor();
        timestamp = 0;
        touchPadXY = new int[2];
    }
    public void setTouchPadXY(int[] touchPadXY) {
        this.touchPadXY = touchPadXY;
    }

    public int[] getTouchPadXY() {
        return touchPadXY;
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
        private int X;
        private int Y;
        private int Z;

        public GyroscopeSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(int x, int y, int z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public int getX() {
            return X;
        }
        public int getY() {
            return Y;
        }
        public int getZ() {
            return Z;
        }
    }

    public class AccelerometerSensor {
        private int X;
        private int Y;
        private int Z;

        public AccelerometerSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(int x, int y, int z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public int getX() {
            return X;
        }
        public int getY() {
            return Y;
        }
        public int getZ() {
            return Z;
        }
    }

    public class MagneticSensor {
        private int X;
        private int Y;
        private int Z;

        public MagneticSensor() {
            X = 0;
            Y = 0;
            Z = 0;
        }

        public void setValues(int x, int y, int z) {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }
        public int getX() {
            return X;
        }
        public int getY() {
            return Y;
        }
        public int getZ() {
            return Z;
        }
    }

    public class TemperatureSensor {
        private int temperature;

        public TemperatureSensor() {
            this.temperature = 0;
        }

        public void setTemperature(int temperature) {
            this.temperature  = temperature;
        }

        public int getTemperature() {
            return temperature;
        }
    }

    public class LightSensor {
        private int lightSensorValue;

        public LightSensor() {
            this.lightSensorValue = 0;
        }

        public void setLightSensorValue(int lightSensorValue) {
            this.lightSensorValue = lightSensorValue;
        }

        public int getLightSensorValue() {
            return lightSensorValue;
        }
    }

    public class ProximitySensor {
        private int proximitySensorValue;

        public ProximitySensor() {
            this.proximitySensorValue = 0;
        }

        public void setProximitySensorValue(int proximitySensorValue) {
            this.proximitySensorValue = proximitySensorValue;
        }

        public int getProximitySensorValue() {
            return proximitySensorValue;
        }
    }


}
