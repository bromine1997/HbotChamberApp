package com.mcsl.hbotchamberapp.Controller;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

            // startDataReadingTask();
            // startDataProcessingTask();
        } catch (Exception e) {
            Log.e(TAG, "Initialization error", e);
        }
    }

    public void loopbackCommand(String message) {
        dataReadingExecutor.submit(() -> {
            try {
                flush();
                UART0.writeStr(message);
                Thread.sleep(10); // 데이터 도착 시간 대기
                while (!UART0.dataAvailable()) ;
                String recieveStr = UART0.readStr(50); // 수신 데이터 읽기


                Log.d(TAG, "Receive: " + recieveStr);
            } catch (Exception e) {
                Log.e(TAG, "Loopback test error", e);
            }
        });
    }
    private int extractCo2Value(String data) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            Log.e(TAG, "Invalid CO2 data format: " + data);
            return -1; // 또는 다른 오류 값을 반환
        }
    }

    public Integer getCo2DataFromQueue() {
        try {
            return co2DataQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1; // 또는 다른 오류 값을 반환
        }
    }

    public  void Mode_2_select(){
        loopbackCommand("K 2\r\n");
    }

    public void calibration(){
        loopbackCommand("A 32\r\n");  // send: "A 32\r\n"  필터 설정

        loopbackCommand("Q\r\n");  // send: "G\r\n"      calibration 400ppm

    }

//    private void startDataReadingTask() {
//        flush();
//        dataReadingExecutor.submit(() -> {
//            while (running) {
//                try {
//                    if (UART0.dataAvailable()) {
//                        serialEvent2();
//                    } else {
//                        Thread.sleep(10);  // Wait for a short period before checking again
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Data reading error", e);
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        });
//    }
//
//
//    public String getData() {
//        try {
//            return rawDataQueue.take();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            return "";
//        }
//    }
//
//    private void startDataProcessingTask() {
//        dataProcessingExecutor.submit(() -> {
//            while (running) {
//                try {
//                    String inStr = rawDataQueue.take();
//                    if (inStr.contains("?")) {
//                        Log.e(TAG, "Invalid data received: " + inStr);
//                        continue; // 무효한 데이터를 무시하고 다음 데이터를 읽음
//                    }
//
//                    StringBuilder inputString = new StringBuilder();
//                    boolean stringComplete = false;
//                    for (char inChar : inStr.toCharArray()) {
//                        inputString.append(inChar);
//                        if (inChar == '\n') {
//                            stringComplete = true;
//                        }
//                    }
//
//                    if (stringComplete) {
//                        Log.d(TAG, "String complete, adding to queue: " + inputString.toString());
//                        rawDataQueue.offer(inputString.toString().trim());
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    Log.e(TAG, "Data processing interrupted", e);
//                } catch (Exception e) {
//                    Log.e(TAG, "Error processing UART data", e);
//                }
//            }
//        });
//    }

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
