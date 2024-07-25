package com.mcsl.hbotchamberapp.Controller;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mraa.Uart;
import mraa.UartParity;

public class Co2Sensor {
    private static final String TAG = "SprintIR";
    private static final int SPRINT_BUFSIZE = 15; // 작은 버퍼 크기로 설정

    private Uart UART0; // UART 인터페이스 객체

    private BlockingQueue<Integer> co2DataQueue = new LinkedBlockingQueue<>(4096);
    private ExecutorService dataReadingExecutor;
    private ExecutorService dataProcessingExecutor;

    private volatile boolean running = true;


    public Co2Sensor() {
        try {
            UART0 = new Uart(0);
            if (UART0 != null) {
                UART0.setBaudRate(9600);
                UART0.setMode(8, UartParity.UART_PARITY_NONE, 1);
                UART0.setFlowcontrol(false,false);
                Log.d(TAG, "UART0 initialized and baudrate set.");
            } else {
                Log.e(TAG, "Failed to initialize UART");
            }
        } catch (Exception e) {
            Log.e(TAG, "UART initialization error", e);
        }
        dataReadingExecutor = Executors.newSingleThreadExecutor();
        dataProcessingExecutor = Executors.newSingleThreadExecutor();
    }

    public void init() {
        try {
            calibration();
            Mode_2_select();

        } catch (Exception e) {
            Log.e(TAG, "Initialization error", e);
        }
    }

    public Future<String> loopbackCommand(String message) {
        return dataReadingExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    flush();
                    UART0.writeStr(message);
                    Thread.sleep(10); // 데이터 도착 시간 대기
                    while (!UART0.dataAvailable());

                    String recieveStr = UART0.readStr(50); // 수신 데이터 읽기

                    return recieveStr;

                } catch (Exception e) {
                    Log.e(TAG, "Co2 get data Fail", e);
                    throw e; // 예외를 호출자에게 전달
                }
            }
        });
    }



    public  void Mode_2_select(){
        loopbackCommand("K 2\r\n");
    }

    public void calibration(){
        loopbackCommand("A 32\r\n");  // send: "A 32\r\n"  필터 설정

        loopbackCommand("Q\r\n");  // send: "G\r\n"      calibration 400ppm

    }

//
    public void flush() {
        if (UART0 != null) {
            while (UART0.dataAvailable()) {
                UART0.readStr(SPRINT_BUFSIZE);
            }
        }
    }

    // 리소스 정리
    public void cleanup() {
        UART0.delete(); // UART 인터페이스 닫기
    }
}
