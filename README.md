# 📱 HbotChamberApp

> **IoT 고압산소챔버 Android 모니터링 및 제어 앱**  
> Tinker Board 2S 기반 챔버 시스템의 실시간 제어 · 모니터링 · 데이터 관리

<br>

![Platform](https://img.shields.io/badge/Platform-Android%2011-3DDC84?style=flat&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java-007396?style=flat&logo=openjdk&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-purple?style=flat)
![Hardware](https://img.shields.io/badge/Hardware-Tinker%20Board%202S-E6522C?style=flat&logo=asus&logoColor=white)
![IDE](https://img.shields.io/badge/IDE-Android%20Studio-3DDC84?style=flat&logo=androidstudio&logoColor=white)

---

## 📌 프로젝트 개요

석사 논문 프로젝트의 일부로 개발한 **고압산소챔버 전용 Android 제어 앱**입니다.  
Tinker Board 2S(SBC) 위에서 직접 실행되며, MRAA 라이브러리를 통해 하드웨어를 직접 제어합니다.

| 항목 | 내용 |
|------|------|
| 개발 환경 | Android Studio |
| OS | Android 11 |
| 타겟 보드 | Tinker Board 2S (SBC) |
| 언어 | Java |
| 아키텍처 | MVVM (ViewModel · LiveData) |
| 하드웨어 제어 | MRAA |
| 인증 | JWT |
| 통신 | WebSocket · REST API |

---

## 🔧 주요 기능

### 1. PID 기반 챔버 가압 · 감압 제어

앱에서 직접 PID 알고리즘을 실행하여 챔버 내부 압력을 정밀하게 제어합니다.

- 비례제어 밸브 (AD5420 DAC, 4~20mA 출력) 제어
- 솔레노이드 밸브 구동 (DRV110)
- 목표 압력값 설정 → PID 연산 → 밸브 조절
- 가압 · 감압 · 유지 단계별 튜닝 파라미터 적용

### 2. 실시간 센서 모니터링 (WebSocket)

| 센서 | 모델 | 측정 대상 |
|------|------|----------|
| 압력 | MBS3000 | 챔버 내부 압력 |
| 온도 · 습도 | HX93BDC | 내부 환경 |
| 산소 농도 | AO-09 | O₂ 농도 |
| CO₂ 농도 | SprintIR-WX-100 | CO₂ 농도 |

### 3. 하드웨어 직접 제어 (MRAA)

Tinker Board 2S의 GPIO · SPI · I2C 핀을 MRAA 라이브러리로 직접 제어합니다.  
ADC 데이터 수집 (MAX1032 SPI ADC) 및 밸브 출력까지 앱 내에서 처리합니다.

### 4. 사용자 인증 및 권한 관리

- JWT 기반 로그인 인증
- RBAC — 역할별 접근 권한 (관리자 / 운영자 / 일반)

### 5. NestJS 서버 · Vue.js 웹 연동

- REST API로 NestJS 서버와 데이터 송수신
- MongoDB에 측정 데이터 저장
- Vue.js 3 웹 대시보드와 동일 데이터 공유

---

## 🏗 아키텍처

```
┌─────────────────────────────────────────┐
│          Android App (MVVM)             │
│                                         │
│  View ←→ ViewModel ←→ Repository       │
│               │                         │
│        LiveData 관찰                    │
└───────────────┬─────────────────────────┘
                │
    ┌───────────┼───────────┐
    │           │           │
  MRAA       WebSocket   REST API
  (HW 제어)  (실시간)    (NestJS)
    │
┌───┴──────────────────┐
│  Tinker Board 2S     │
│  MAX1032 (SPI ADC)   │
│  AD5420  (DAC)       │
│  DRV110  (Solenoid)  │
└──────────────────────┘
```

---

## 📁 레포지토리 구조

```
HbotChamberApp/
├── app/
│   └── src/main/java/
│       ├── model/          # 데이터 모델
│       ├── viewmodel/      # ViewModel (MVVM)
│       ├── view/           # Activity / Fragment
│       ├── repository/     # 데이터 소스 추상화
│       ├── service/        # ADC · Valve · PID 서비스
│       └── network/        # WebSocket · REST 클라이언트
├── build.gradle.kts
└── README.md
```

---

## 🔗 관련 레포지토리

이 앱은 아래 전체 시스템의 일부입니다.

| 레포 | 역할 |
|------|------|
| `HbotChamberApp` | Android 제어 앱 (현재) |
| `RehabilitationBicycle` | 재활자전거 IoT 펌웨어 |

---

## 👤 개발자

| | |
|---|---|
| **이름** | 서보민 (Bromine) |
| **GitHub** | [@bromine1997](https://github.com/bromine1997) |
| **포트폴리오** | [bromine1997.github.io/web-porfolio](https://bromine1997.github.io/web-porfolio) |
