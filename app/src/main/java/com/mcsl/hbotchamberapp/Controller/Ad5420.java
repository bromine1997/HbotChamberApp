package com.mcsl.hbotchamberapp.Controller;

import android.util.Log;

import mraa.Dir;
import mraa.Gpio;
import mraa.Spi;
import mraa.Spi_Mode;

public class Ad5420 {

    // Internal addresses of DAC registers
    public static final byte AD5420_STATUS_REG = 0x00;

    public static final byte AD5420_CONTROL_REG = 0x02;

    // Internal flags for various settings
    public static final short  AD5420_REXT = 0x2000;
    public static final short AD5420_OUTEN = 0x1000;
    public static final short AD5420_DCEN = 0x0008;
    public static final short AD5420_4_20_RANGE = 0x0005;


    // Status register flags
    public static final int AD5420_IOUT_FAULT = 0x0004;
    public static final int AD5420_SR_ACTIVE = 0x0002;
    public static final int AD5420_OVRHEAT_FAULT = 0x0001;


    // New constants for register addresses
    public static final byte AD5420_CONTROL_REG_ADDR = 0x55;  // Address of DAC CONTROL register
    public static final byte AD5420_DATA_REG_ADDR = 0x01;     // Address of DAC DATA register
    public static final byte AD5420_RESET_REG_ADDR = 0x56;    // Address of DAC RESET register

    private static final int DAC_VALUE_PER_MA = 4096; // 1mA당 DAC 값 (65535 / 16)

    private int pressCurrent = 0; // 변경: short -> int
    private int ventCurrent = 0; // 변경: short -> int

    private static final int DAC_MAX_VALUE = 0xFFFF; // 16비트 최대값



    private Spi spi1;
    private Gpio latch1;
    private Gpio clear;



    public Ad5420(int spiBus) {
        spi1 = new Spi(0);

        latch1 = new Gpio(26);                  //AAD5420 2번째 소자 latch PIN

        clear = new Gpio(28);                   //CLEAR PIN 공통으로 사용



        clear.dir(Dir.DIR_OUT);

        // Initialize pins to default state
        latch1.write(0);
        clear.write(0);

        spi1.mode(Spi_Mode.SPI_MODE0);
        spi1.frequency(500000);                // 주파수가 너무 높으면 파형 왜곡


        clearSpiBuffer();
    }

    private void clearSpiBuffer() {
        byte[] dummy = new byte[] {0, 0, 0, 0, 0, 0}; // 올바르게 배열을 초기화

        spi1.write(dummy);

    }

    void Daisyclear(){

        clear.write(0);

        try { Thread.sleep(1); } catch (InterruptedException e) {}

        clear.write(1);

        try { Thread.sleep(1); } catch (InterruptedException e) {}

        clear.write(0);
    }

    public void Daisy_reset(){

        byte[] command_reset = new byte[] {
                AD5420_RESET_REG_ADDR, 0 , 1,
                AD5420_RESET_REG_ADDR, 0 , 1
        };


        // 두 번째 칩을 업데이트할 때 48비트 전송
        spi1.write(command_reset);



    }
   public void Daisy_Setup() {
        // Example command to select channel and set configuration for MAX1032
        Log.d("subactivity_IOPORT", "ad5420 begin");



        short config = (AD5420_REXT | AD5420_OUTEN | AD5420_DCEN | AD5420_4_20_RANGE);

        byte configHigh = (byte) ((config >> 8) & 0x00FF);
        byte configLow  = (byte) (config & 0xFF);


        byte[] command_setup_1 = new byte[] {
                0,0,0,
                AD5420_CONTROL_REG_ADDR, configHigh , configLow
        };

       byte[] command_setup = new byte[] {

               AD5420_CONTROL_REG_ADDR, configHigh , configLow,
               AD5420_CONTROL_REG_ADDR, configHigh , configLow
       };

       spi1.write(command_setup_1);

        // 두 번째 칩을 업데이트할 때 48비트 전송

       spi1.write(command_setup);

       //


        try { Thread.sleep(1); } catch (InterruptedException e) {}



    }



    public void DaisyCurrentWrite(char Channel, short data) {                    //channel : press(0) or vent(1)     , data : 전류 크기
        byte[] Command_write;

        byte command_Hdata = (byte) ((data >> 8) & 0x00FF);
        byte command_Ldata = (byte) (data & 0xFF);

        if (Channel == 0) {
            // 첫 번째 칩만 업데이트할 때 (24비트 전송)
            Command_write = new byte[] {0,0,0,AD5420_DATA_REG_ADDR, command_Hdata, command_Ldata};

            // SPI로 데이터 전송
            spi1.write(Command_write);  // SPI 전송 후 자동으로 Latch가 내려감

            pressCurrent = data & 0xFFFF; // 16비트 값 저장

        } else if (Channel == 1) {
            // 두 번째 칩을 업데이트할 때 (48비트 전송)
            Command_write = new byte[] {AD5420_DATA_REG_ADDR, command_Hdata, command_Ldata, 0, 0, 0};

            // SPI로 데이터 전송
            spi1.write(Command_write);  // SPI 전송 후 자동으로 Latch가 내려감


            ventCurrent = data & 0xFFFF; // 16비트 값 저장

        } else {
            // 올바르지 않은 채널 번호일 때 (예외 처리)
            throw new IllegalArgumentException("Invalid Channel: " + Channel);
        }


    }



    private void sendToAD5420(int inputValue) {
        // 입력 값에 따라 4~20mA 범위 내의 전류값을 계산
        int minCurrent = 4; // mA
        int maxCurrent = 20; // mA
        int maxDacValue = 0xFFFF; // 16-bit DAC

        // 입력값을 DAC 값으로 변환
        int dacValue = (int)((inputValue / 100.0) * (maxDacValue * (maxCurrent - minCurrent) / 20.0) + (minCurrent * maxDacValue / 20.0));

        // DAC 값을 설정
        // ad5420.ad5420WriteData(dacValue);

        // 현재 설정을 출력

    }


    public void PressValveCurrentUp() {
        pressCurrent += DAC_VALUE_PER_MA;
        if (pressCurrent > DAC_MAX_VALUE) {
            pressCurrent = DAC_MAX_VALUE;
        }
        Log.d("CurrentValue", "Press Valve Current: " + pressCurrent);
        DaisyCurrentWrite((char) 0, (short)pressCurrent);
    }

    public void PressValveCurrentDown() {
        if (pressCurrent >= DAC_VALUE_PER_MA) {
            pressCurrent -= DAC_VALUE_PER_MA;
        } else {
            pressCurrent = 0;
        }
        DaisyCurrentWrite((char) 0, (short)pressCurrent);
    }

    public void VentValveCurrentUp() {
        ventCurrent += DAC_VALUE_PER_MA;
        if (ventCurrent > DAC_MAX_VALUE) {
            ventCurrent = DAC_MAX_VALUE;
        }
        DaisyCurrentWrite((char) 1, (short)ventCurrent);
    }

    public void VentValveCurrentDown() {
        if (ventCurrent >= DAC_VALUE_PER_MA) {
            ventCurrent -= DAC_VALUE_PER_MA;
        } else {
            ventCurrent = 0;
        }
        DaisyCurrentWrite((char) 1,(short) ventCurrent);
    }

    // 현재 압력 밸브의 전류(mA)를 반환
    public double getPressCurrentInMA() {
        return 4.0 + ((pressCurrent / (double) DAC_MAX_VALUE) * 16.0);
    }

    // 현재 배기 밸브의 전류(mA)를 반환
    public double getVentCurrentInMA() {
        return 4.0 + ((ventCurrent / (double) DAC_MAX_VALUE) * 16.0);
    }

}
