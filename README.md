# 📱 HbotChamberApp

> **IoT 고압산소챔버 Android 모니터링 및 제어 앱**  
> Tinker Board 2S 기반 고압산소챔버 시스템의 실시간 제어 · 모니터링 · 데이터 관리  
> 연세대학교 의공학과 석사 학위논문 프로젝트 (2024)

<br>

![Platform](https://img.shields.io/badge/Platform-Android%2011-3DDC84?style=flat&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java-007396?style=flat&logo=openjdk&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-purple?style=flat)
![Hardware](https://img.shields.io/badge/Hardware-Tinker%20Board%202S-E6522C?style=flat&logo=asus&logoColor=white)
![IDE](https://img.shields.io/badge/IDE-Android%20Studio-3DDC84?style=flat&logo=androidstudio&logoColor=white)

---

## 📌 프로젝트 개요

기존 고압산소치료 시스템은 높은 도입 비용, 설치 환경의 제약, 실시간 원격 관리 기능의 부재로 인해 전문 의료기관 외에서의 활용이 제한적이었다. 이를 해결하기 위해 **IoT 기술과 Android OS를 결합한 원격 제어 및 모니터링 시스템**을 개발하였다.

앱은 Tinker Board 2S(SBC) 위에서 직접 실행되며, MRAA 라이브러리를 통해 GPIO·SPI·I2C 핀을 직접 제어한다. 센서 데이터 수집부터 PID 압력 제어, 서버 통신, 사용자 인증까지 챔버 운용에 필요한 모든 기능을 단일 애플리케이션에서 처리한다.

| 항목 | 내용 |
|------|------|
| 개발 환경 | Android Studio |
| OS | Android 11 |
| 타겟 보드 | Tinker Board 2S (ASUS, Rockchip RK3399) |
| 언어 | Java |
| 아키텍처 | MVVM (ViewModel · LiveData) |
| 하드웨어 제어 | MRAA |
| 인증 | JWT (JSON Web Token) |
| 통신 | WebSocket · REST API |
| 서버 | NestJS + MongoDB |
| 웹 클라이언트 | Vue.js 3 |

---

## 🔧 주요 기능

### 1. PID 기반 챔버 가압 · 감압 제어

가압과 감압을 각각 독립적으로 제어하기 위해 **두 개의 PID 제어기**를 구현하였다.

치료 프로파일에서 설정된 압력 값을 Set Point로 설정하고, MBS3000 압력 센서의 측정값을 PV(Process Variable)로 입력한다. PID 연산으로 MV(Manipulated Variable)를 계산하여 AD5420 DAC를 통해 비례제어 밸브(4~20mA)를 구동한다.

```
PID(t) = Kp·e(t) + Ki·∫e(t)dt + Kd·de(t)/dt
```

- **비례항(P)**: 현재 오차에 비례한 즉각적 제어 출력, 빠른 응답 제공
- **적분항(I)**: 과거 오차 누적으로 정상상태 오차 제거
- **미분항(D)**: 오차 변화율 기반으로 오버슈트 억제, 안정성 향상

**검증 결과**
- 가압 속도: 0.028 MPa/min
- 감압 속도: 0.0446 MPa/min
- 가압 시 Overshoot: **0.67% (3.02 ATA)**
- 2 ATA 유지 구간 압력 오차: ±0.02 ATA 이내
- 응급 배기밸브 작동 후 정상 복귀 확인 (35분, 180분 프로파일)

### 2. 실시간 센서 모니터링 (WebSocket)

| 센서 | 모델 | 인터페이스 | 측정 대상 | 출력 방식 |
|------|------|-----------|----------|----------|
| 압력 | MBS3000 (Danfoss, Denmark) | ADC | 챔버 내부 압력 (0~6 bar, ±0.5%) | 4~20mA |
| 온도 · 습도 | HX93BDC (OMEGA, USA) + TH-RP 프로브 | ADC | 온도(-30~100°C), 상대습도(0~100%) | 4~20mA |
| 산소 농도 | AO-09 (ASAIR, China) | ADC | O₂ 농도 (0~100%, ±1%) | 9~13mV 아날로그 |
| CO₂ 농도 | SprintIR-WX-100 (GSS, UK) | UART | CO₂ 농도 (0~100%, NDIR 방식) | UART |

온도·압력·산소·습도 센서의 아날로그 신호는 **MAX1032 SPI ADC**를 통해 디지털 변환 후 **ADUM1400 디지털 아이솔레이터(자기유도 방식)**를 거쳐 Tinker Board 2S로 전송된다. CO₂ 센서는 UART 통신을 사용하며 **ISO7421 디지털 아이솔레이터(광학 절연 방식)**로 절연된다. 이중 절연 구조로 전기적 간섭을 차단하고 의료기기 수준의 전기적 안전성을 확보하였다.

### 3. 하드웨어 직접 제어 (MRAA)

**비례제어 밸브 (가압 · 감압 독립 2채널)**

AD5420 전류 출력 DAC를 **Daisy Chain** 방식으로 연결하여 2개의 비례제어 밸브(TECA2410-10EPS-G-10A)를 순차 제어한다. SPI 통신으로 SDIN·SCLK·LATCH 신호를 전달하며, ADUM1400으로 신호를 절연한다.

```
Tinker Board 2S → ADUM1400 → AD5420 #1 (Press 밸브) → AD5420 #2 (Vent 밸브)
```

**솔레노이드 밸브 (8채널 독립 제어)**

각 채널마다 **VO1400AEFTER Phototransistor Optocoupler**로 신호를 절연하고, **DRV110 고효율 솔레노이드 드라이버**로 VXZ240FGE(SMC, Japan) 솔레노이드 밸브를 구동한다. DRV110은 초기 Peak Current로 빠른 응답을 확보하고, 이후 Hold Current로 전력 소비를 절감하는 지능형 전류 제어 기능을 제공한다. Normal Close 타입을 채택하여 전원 차단 시에도 챔버 내부 환경을 유지한다.

**전원부**

| 변환 | 소자 | 용도 |
|------|------|------|
| 24V → 5V | VR10S05 (XP Power, DC-DC 컨버터) | 디지털 회로 전원 |
| 5V → 3.3V | MIC5233-3.3 (Microchip, 저잡음 선형 레귤레이터) | 디지털 로직 회로 |

### 4. 사용자 인증 및 권한 관리 (RBAC)

JWT(JSON Web Token) 기반 인증을 사용하며, 로컬 관리자 계정(오프라인 대응)과 서버 기반 인증(OkHttp 비동기 통신) 두 가지 방식을 지원한다. 인증 성공 시 발급된 토큰은 SharedPreferences에 저장되어 이후 모든 API 요청에 활용된다.

| 역할 | 권한 |
|------|------|
| Administrator | 모든 시스템 기능에 대한 접근 권한 |
| Operator | 챔버 상태 모니터링 + 기본 제어 기능 접근 |
| User | 모니터링만 가능, 제어 권한 제한 |

### 5. NestJS 서버 · Vue.js 웹 연동

- REST API로 NestJS 서버와 데이터 송수신
- WebSocket으로 실시간 센서 데이터 브로드캐스트 및 프로파일 동기화
- MongoDB에 센서 데이터 · 치료 기록 · 사용자 정보 저장
- Vue.js 3 웹 대시보드와 동일 데이터 공유 (1초 갱신 / 압력 그래프 1분 단위 업데이트)

---

## 🏗 아키텍처

### 소프트웨어 아키텍처 (MVVM)

MVVM(Model-View-ViewModel) 패턴을 기반으로 UI와 비즈니스 로직을 완전히 분리하였다. Android LiveData를 활용한 반응형 프로그래밍으로 센서 데이터 변화를 UI에 즉시 반영하며, Repository 패턴으로 데이터 일관성과 무결성을 보장한다.

```
View ←(Notify)─ ViewModel ←(Callback)─ Model
  │                  │
User Input     LiveData 관찰
```

### 전체 시스템 하드웨어 구조

```
┌──────────────────────────────────────────────────────┐
│                  Chamber Control Board               │
│                                                      │
│  [입력부]              Tinker Board 2S               │
│  Temperature  ─┐                                     │
│  Humidity     ─┤  ISOLATE  ┌──────────────────────┐  │
│  O₂           ─┼──────────▶│  MAX1032 (SPI ADC)   │  │
│  Flow         ─┤           │  ADUM1400 (Isolator) │  │
│  Pressure     ─┘           │  ISO7421 (Isolator)  │  │
│  CO₂ ─(UART)──────────────▶└──────────────────────┘  │
│                                                      │
│  [출력부]                                            │
│  AD5420 Daisy Chain ──▶ Proportional Valve ×2        │
│  DRV110 ×8          ──▶ Solenoid Valve ×8            │
└──────────────────────────────────────────────────────┘
           │
    ┌──────┼──────┐
    │      │      │
  MRAA  WebSocket REST API
(HW제어) (실시간)  (NestJS)
                     │
               MongoDB ◀──▶ Vue.js 3 Web
```

---

## 📱 UI 화면 구성

### 로그인 화면
JWT 기반 인증. 오프라인 환경을 위한 로컬 관리자 계정 인증과 서버 기반 인증 두 가지 방식 지원. 유효성 검증 및 예외 처리 구현.

### 메뉴 화면
시스템 시작 시 GPIO 서비스 · 센서 서비스 · 밸브 서비스 · 웹소켓 서비스 4개 핵심 서비스를 초기화한다.

| 버튼 | 기능 |
|------|------|
| RUN | 치료 프로토콜 실행 화면으로 이동 |
| EDIT | 치료 프로파일 편집 화면으로 이동 |
| IOPORT | 입출력 포트 직접 제어 · 모니터링 |
| EXIT | 모든 서비스 정상 종료 |

### IOPort 제어 화면
실시간 센서값(O₂, CO₂, 습도, 온도, 압력, 유량) 표시 + 솔레노이드 밸브 ON/OFF 제어 + 비례제어 밸브 전류값(4~20mA) 수동 조절 + 디지털 입력 스위치 8채널 상태 모니터링.

### EDIT 화면 (프로파일 편집)
압력-시간 그래프(X축: 시간(분), Y축: 압력(ATA))를 실시간 시각화. 구간별 시작 압력 · 종료 압력 · 지속 시간 설정. 프로파일은 JSON 형식으로 저장되며 WebSocket을 통해 서버와 동기화된다.

### RUN 화면 (치료 실행)
목표 압력 프로파일(검정 선)과 실제 챔버 압력(빨간 선)을 실시간 그래프로 비교. Set Point · 현재 압력 · O₂ · CO₂ · 온도 · 습도를 수치로 표시. DOOR LOCK · RUN · COMPLETE · END 상태 표시등 + RUN/PAUSE/END/EXIT 제어 버튼 제공.

---

## ✅ 시스템 검증 결과 (식약처 기준)

식품의약품안전처 인증 1인용 의료용 고압산소챔버와 9개 항목 규격 비교를 수행하였다.

| 항목 | 식약처 인증 기준 | IoT 챔버 측정값 |
|------|----------------|----------------|
| 가압 속도 | ≤ 0.078 MPa/min | **0.028 MPa/min** ✅ |
| 감압 속도 | ≤ 0.078 MPa/min | **0.0446 MPa/min** ✅ |
| 응급배기밸브 | 2.00bar → 대기압, 120초(±30초) 이내 | **120초 이내** ✅ |
| 산소 농도 | 23.5%(±5%) 이하 | **23.5%(±5%) 이하** ✅ |
| CO₂ 분압 | 0~5000 ppm | **0~5000 ppm** ✅ |
| 상대습도 | 40~60% | **40~60%** ✅ |
| 최고허용 순간온도 | < 40℃ | **< 40℃** ✅ |
| 최고허용 작동온도 | < 32℃ | **< 32℃** ✅ |
| 압력 기록 | 0.03 bar 변화, 1분 간격, 3시간 이상 | **0.01 bar, 1초 간격(앱) / 1분 간격(웹)** ✅ |

---

## 📁 레포지토리 구조

```
HbotChamberApp/
├── app/
│   └── src/main/java/
│       ├── model/          # 데이터 모델
│       ├── viewmodel/      # ViewModel (MVVM)
│       │   ├── IoPortViewModel
│       │   └── RunViewModel
│       ├── view/           # Activity / Fragment
│       │   ├── LoginActivity
│       │   ├── MenuActivity
│       │   ├── IoPortActivity
│       │   ├── EditActivity
│       │   └── RunActivity
│       ├── repository/     # 데이터 소스 추상화
│       ├── service/        # ADC · Valve · PID 서비스
│       │   ├── GpioService
│       │   ├── SensorService
│       │   ├── ValveService
│       │   └── WebSocketService
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

## 📄 참고 논문

> 서보민, "사물인터넷(IoT) 기술을 활용한 안드로이드OS 기반 고압산소챔버 제어 및 모니터링 시스템 개발에 관한 연구", 연세대학교 대학원 의공학과 석사학위논문, 2024.

---

## 👤 개발자

| | |
|---|---|
| **이름** | 서보민 (Bromine) |
| **GitHub** | [@bromine1997](https://github.com/bromine1997) |
| **포트폴리오** | [bromine1997.github.io/web-porfolio](https://bromine1997.github.io/web-porfolio) |


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
