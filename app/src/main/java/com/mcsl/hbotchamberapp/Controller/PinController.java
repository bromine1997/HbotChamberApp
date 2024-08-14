package com.mcsl.hbotchamberapp.Controller;



import mraa.Dir;
import mraa.Gpio;
import mraa.I2c;
import mraa.mraa;

public class PinController {

    private static final int LED_PIN_1 = 32; // 예시 핀 번호
    private static final int LED_PIN_2 = 38; // 예시 핀 번호
    private static final int LED_PIN_3 = 40; // 예시 핀 번호

    private static final int controlProportionPress_ON_OFF_PIN = 33; // 가압 비례제어 벨브 온오프
    private static final int controlProportionVent_ON_OFF_PIN = 35; //  감압 비례제어 벨브 온오프

    private static final int MCP23017_ADDRESS = 0x20; // MCP23017의 기본 주소입니다. A0, A1, A2 핀 설정에 따라 변경할 수 있습니다.

    // MCP23017 내부 레지스터 주소
    private static final byte IODIRA = 0x00; // 포트 A의 방향 설정 레지스터
    private static final byte IODIRB = 0x01; // 포트 B의 방향 설정 레지스터
    private static final byte GPIOA = 0x12; // 포트 A 데이터 레지스터
    private static final byte GPIOB = 0x13; // 포트 B 데이터 레지스터

    private byte currentOutputs = 0x00; // 현재 포트 A의 출력 상태를 유지하기 위한 변수



    private Gpio ledGpio1;
    private Gpio ledGpio2;
    private Gpio ledGpio3;
    private Gpio controlProportionPressONOFF_PIN;
    private Gpio controlProportionVentONOFF_PIN;
    private I2c i2c6;


    public PinController() {
        mraa.init();
        initializePins();
        initializeI2C();
    }

    private void initializePins() {
        ledGpio1 = new Gpio(LED_PIN_1);                //32번 LED
        ledGpio1.dir(Dir.DIR_OUT);

        ledGpio2 = new Gpio(LED_PIN_2);                //38번 LED
        ledGpio2.dir(Dir.DIR_OUT);

        ledGpio3 = new Gpio(LED_PIN_3);                //40번 LED
        ledGpio3.dir(Dir.DIR_OUT);

        controlProportionPressONOFF_PIN = new Gpio(controlProportionPress_ON_OFF_PIN);         //비례제어벨브 1  : PRESS ON/OFF
        controlProportionPressONOFF_PIN.dir(Dir.DIR_OUT);
        controlProportionPressONOFF_PIN.write(0);

        controlProportionVentONOFF_PIN = new Gpio(controlProportionVent_ON_OFF_PIN);          //비례제어벨브 2  : Vent  ON/OFF
        controlProportionVentONOFF_PIN.dir(Dir.DIR_OUT);
        controlProportionVentONOFF_PIN.write(0);
    }

    private void initializeI2C() {
        i2c6 = new I2c(0);
        try {
            i2c6.address((short) 0x20); // MCP23017의 기본 주소
            i2c6.writeReg((byte) 0x00, (byte) 0x00); // 모든 A 포트 핀을 입력
            i2c6.writeReg((byte) 0x01, (byte) 0xFF); // 모든 B 포트 핀을 출력
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte readInputs() {
        byte inputStatus = 0;
        try {
            inputStatus = (byte) i2c6.readReg(GPIOB); // 포트 B의 입력 상태 읽기
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputStatus;
    }

    public void toggleLed(int ledNumber) {
        Gpio ledGpio;
        switch (ledNumber) {
            case 1:
                ledGpio = ledGpio1;
                break;
            case 2:
                ledGpio = ledGpio2;
                break;
            case 3:
                ledGpio = ledGpio3;
                break;
            default:
                throw new IllegalArgumentException("Invalid LED number");
        }
        ledGpio.write(ledGpio.read() == 0 ? 1 : 0);
    }
    // 비례제어 밸브 press on/off toggle

    public void Proportion_Press_OFF() {
        controlProportionPressONOFF_PIN.write(0);
    }
    public void Proportion_Press_ON() {
        controlProportionPressONOFF_PIN.write(1);
    }

    public void Proportion_VENT_OFF() {
        controlProportionVentONOFF_PIN.write(0);
    }
    public void Proportion_VENT_ON() {
        controlProportionVentONOFF_PIN.write(1);
    }



    // 가압 솔벨브 온오프
    public void Toggle_Solenoid_Press() {
        toggleI2C_External_GpioPin(0);
    }

    // 감압 솔벨브 온오프
    public void Toggle_Solenoid_Vent() {
        toggleI2C_External_GpioPin(1);
    }


    public void toggleI2C_External_GpioPin(int pin) {               // IO Expander GPIOA(출력) : 솔벨브 0번~ 7번까지 온오프
        try {
            byte currentOutputs = (byte) i2c6.readReg((byte) 0x12); // 현재 포트 A의 출력 상태 읽기
            currentOutputs ^= (1 << pin);                           // 특정 핀의 상태 토글
            i2c6.writeReg((byte) 0x12, currentOutputs);             // 변경된 상태로 포트 A 업데이트
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 새로운 메소드: 핀을 ON 상태로 설정
    public void Sol_ON(int pin) {
        currentOutputs= 0;
        currentOutputs |= (1 << pin);                           // 특정 핀을 ON 상태로 설정
        i2c6.writeReg((byte) 0x12, currentOutputs);             // 변경된 상태로 포트 A 업데이트

    }

    // 새로운 메소드: 핀을 OFF 상태로 설정
    public void SOL_OFF(int pin) {
        currentOutputs= 0;
        currentOutputs &= ~(1 << pin);                          // 특정 핀을 OFF 상태로 설정
        i2c6.writeReg((byte) 0x12, currentOutputs);             // 변경된 상태로 포트 A 업데이트

    }

    public void Sol_OUPUT(int pin, int state) {

        if (state == 1) {
            currentOutputs |= (1 << pin);                       // 특정 핀을 ON 상태로 설정
        } else if (state == 0) {
            currentOutputs &= ~(1 << pin);
        }

        i2c6.writeReg((byte) 0x12, currentOutputs);
    }


}
