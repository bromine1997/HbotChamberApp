package com.mcsl.hbotchamberapp.Controller;

import android.util.Log;

import java.util.Arrays;

import mraa.Dir;
import mraa.Gpio;
import mraa.Spi;
import mraa.Spi_Mode;

public class Max1032 {
    // Internal addresses of DAC registers
    private static final String TAG = "max1032";
    private static final int MAX1032_START_BIT = 0x80;  // First bit (1) is the start bit
    private static final int MAX1032_CHANNEL_0 = 0x00;  // Channel 0 selection
    private static final char MAX1032_channel_1 = 0x10; // max1032 1번 채널
    private static final char MAX1032_channel_2 = 0x20; // max1032 2번 채널
    private static final char MAX1032_channel_3 = 0x30; // max1032 3번 채널
    private static final char MAX1032_channel_4 = 0x40; // max1032 4번 채널
    private static final char MAX1032_channel_5 = 0x50; // max1032 5번 채널
    private static final char MAX1032_channel_6 = 0x60; // max1032 6번 채널
    private static final char MAX1032_channel_7 = 0x70; // max1032 7번 채널
    private static final int MAX1032_SINGLE_ENDED = 0x00;  // Single-ended mode
    private static final int MAX1032_DEFAULT_RANGE = 0x07;  // Default range configuration (full-scale)

    //MAX1032 레지스터  설정값
    private static final char MAX1032_START = 0x80; // max1032 start bit
    private static final char MAX1032_single_mode = 0x00; //
    private static final char MAX1032_RangeSelect_0 = 0x00; //
    private static final char MAX1032_RangeSelect_1 = 0x01; //
    private static final char MAX1032_RangeSelect_2 = 0x02; //
    private static final char MAX1032_RangeSelect_3 = 0x03; //

    private static final int MAX1032_MODE_0 = 0x00;  // External clock mode


    private static Spi spi5;
    private static Gpio latch1;


    public Max1032(int spiBus, int latchPin1 ) {
        spi5 = new Spi(spiBus);

        latch1 = new Gpio(latchPin1);

        spi5.mode(Spi_Mode.SPI_MODE0);
        spi5.frequency(20000);

        initialize_max1032_latch();
    }

    private void initialize_max1032_latch() {
        latch1.dir(Dir.DIR_OUT);

        // Initialize pins to default state
        //latch1.write(1);

    }

    public static void Configuration(int channel, int mode) {

        byte[] responseBytes = new byte[4]; // 응답을 저장할 배열

        if (channel < 0 || channel >= 7) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }

        byte command_Configuration = (byte) (MAX1032_START_BIT | (channel << 4)| MAX1032_single_mode | MAX1032_RangeSelect_3);
        byte command_ModeSelect = (byte) (MAX1032_START_BIT| mode<<4|0x08);

        byte command_Read = (byte) (MAX1032_START_BIT| (channel << 4));
        byte[] commandRead_Bytes = new byte[] {command_Read,0,0,0};


        spi5.writeByte(command_Configuration);
        spi5.writeByte(command_ModeSelect);


    }


    public static int Read(int channel) {

        byte[] responseBytes = new byte[4]; // 응답을 저장할 배열

        if (channel < 0 || channel >= 7) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }

        byte command_Read = (byte) (MAX1032_START_BIT| (channel << 4));
        byte[] commandRead_Bytes = new byte[] {command_Read,0,0,0};

        responseBytes=spi5.write(commandRead_Bytes);

        // 응답에서 유효한 14비트 데이터 추출
        //int adcValue = ((responseBytes[2] & 0xFF) << 8) | (responseBytes[3] & 0xFF); // 상위 바이트와 하위 바이트 결합


        return ((responseBytes[2] & 0xFF) << 6) | ((responseBytes[3] & 0xFC) >> 2); // 상위 바이트와 하위 바이트 결합

    }


    public static int ConfigurationChannel_TTTTT(int channel, int mode) {

        byte[] responseBytes = new byte[4]; // 응답을 저장할 배열

        if (channel < 0 || channel >= 7) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }

        byte command_Configuration = (byte) (MAX1032_START_BIT | (channel << 4)| MAX1032_single_mode | MAX1032_RangeSelect_3);
        byte command_ModeSelect = (byte) (MAX1032_START_BIT| mode<<4|0x08);

        byte command_Read = (byte) (MAX1032_START_BIT| (channel << 4));
        byte[] commandRead_Bytes = new byte[] {command_Read,0,0,0};

        //  spi5.write(commandBytes);              //spiWrite를 하면 자동으로 Latch가 내려감 ,.,..이거 고민해서 알고리즘 수정 필요..!

        spi5.writeByte(command_Configuration);
        spi5.writeByte(command_ModeSelect);

        responseBytes=spi5.write(commandRead_Bytes);


        // 응답에서 유효한 14비트 데이터 추출
        int adcValue = ((responseBytes[2] & 0xFF) << 8) | (responseBytes[3] & 0xFF); // 상위 바이트와 하위 바이트 결합
        adcValue = adcValue >> 3; // 하위 2비트 제거하여 14비트로 맞춤


        return adcValue;

    }




    public int[] readAllChannels() {
        int[] values = new int[6];
        for (int channel = 0; channel < 6; channel++) {
            values[channel] = ConfigurationChannel_TTTTT(channel,0);
        }

        Log.d(TAG, "values: " + Arrays.toString(values));
        return values;
    }

    public void ConfigAllChannels() {

        for (int channel = 0; channel < 6; channel++) {
            Configuration(channel,0);
        }

    }

    public int[] ReadAllChannels() {
        int[] values = new int[6];
        for (int channel = 0; channel < 6; channel++) {
            values[channel] = Read(channel);
        }
        Log.d(TAG, "values: " + Arrays.toString(values));
        return values;
    }

}


