# To Do List

> 깔끔하고 직관적인 안드로이드 할 일 관리 앱

Jetpack Compose + Material 3 기반의 네이티브 안드로이드 앱입니다.  
우선순위 분류, 마감 알림, 잠금화면 알림, 아카이브 기능을 갖추고 있습니다.

---

## 주요 기능

### 할 일 관리
- **추가 / 수정 / 삭제** — 하단 시트(Bottom Sheet)에서 빠르게 입력
- **우선순위 분류** — 높음 / 보통 / 낮음 3단계, 색상으로 시각적 구분
- **마감 시간 설정** — 오늘 날짜 기준 시간 선택(AM/PM 스피너)
- **메모 첨부** — 제목 외 선택적 부가 설명 입력
- **작성 중 임시 저장** — 시트를 닫아도 입력 내용이 유지되고, 다시 열면 복원

### 완료 처리 & 아카이브
- **원형 체크박스** — 탭 한 번으로 완료 토글, 채움 애니메이션 제공
- **스와이프 삭제** — 좌측으로 밀어 휴지통 이동
- **Undo** — 삭제 직후 Snackbar에서 5초 이내 복구 가능
- **아카이브 탭** — 날짜별 완료 기록 열람, 날짜 선택기(DatePicker) 제공

### 알림
- **마감 알림** — AlarmManager 기반, 마감 시각에 정확히 발생
- **사전 알림** — 마감 10분 / 30분 / 1시간 전 선택 가능
- **상태바 상시 알림** — Foreground Service로 미완료 할 일 개수 표시
- **잠금화면 알림** — 개별 할 일마다 잠금화면 노출 여부 설정
- **부팅 후 복원** — 기기 재부팅 시 `BOOT_COMPLETED` 수신 후 알람 자동 재등록

### 휴지통
- **Soft Delete** — 삭제 즉시 영구 제거하지 않고 휴지통으로 이동
- **복구 / 영구 삭제** — 휴지통 화면에서 개별 복구 또는 완전 삭제
- **비우기** — 휴지통 전체 일괄 삭제

---

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| UI | Jetpack Compose, Material 3 |
| 아키텍처 | MVVM, Single Activity |
| 상태 관리 | StateFlow, Channel (일회성 이벤트) |
| 로컬 DB | Room (Flow 기반 반응형 쿼리) |
| DI | Hilt + KSP |
| 비동기 | Kotlin Coroutines |
| 알림 | AlarmManager, NotificationCompat, Foreground Service |
| 폰트 | Pretendard |
| 최소 SDK | API 26 (Android 8.0) |
| 타겟 SDK | API 35 (Android 15) |

---

## 아키텍처

```
presentation/
├── screen/
│   ├── MainScreen.kt          # 루트 화면 — 탭(할 일 / 아카이브) 컨테이너
│   ├── TaskItem.kt            # 할 일 카드 & 우선순위 그룹 카드
│   ├── AddTaskBottomSheet.kt  # 할 일 추가 / 수정 시트
│   └── TrashScreen.kt         # 휴지통 화면
├── viewmodel/
│   └── TaskViewModel.kt       # UI 상태, draft 관리, 비즈니스 로직 위임
└── theme/
    ├── Color.kt               # 딥 인디고 팔레트
    ├── Type.kt                # Pretendard 타이포그래피
    └── Theme.kt               # Material 3 ColorScheme 설정

data/
├── local/
│   ├── entity/TaskEntity.kt   # Room Entity (Priority, RepeatType 포함)
│   ├── dao/TaskDao.kt         # 쿼리 정의
│   └── AppDatabase.kt         # Room DB 싱글턴
└── repository/
    ├── TaskRepository.kt      # 인터페이스
    └── TaskRepositoryImpl.kt  # 구현체

service/
├── TodoForegroundService.kt   # 상태바 상시 알림
├── AlarmScheduler.kt          # AlarmManager 래퍼
└── NotificationHelper.kt      # 채널 생성 / 알림 빌더

receiver/
├── TodoAlarmReceiver.kt       # 마감 알람 수신
├── TaskActionReceiver.kt      # 알림에서 완료 처리
└── BootReceiver.kt            # 부팅 후 알람 복원
```

### 데이터 흐름

```
Room DB (Flow)
  └─ Repository
      └─ ViewModel (StateFlow)
          └─ Compose UI (collectAsStateWithLifecycle)
              └─ 사용자 액션 → ViewModel → Repository → Room
                                    └─ AlarmScheduler (AlarmManager)
                                    └─ TodoForegroundService (상태바 갱신)
```

---

## 화면 구성

### 할 일 탭
- 미완료 할 일을 **높음 / 중간 / 낮음** 우선순위 섹션으로 분류
- 각 섹션은 좌측 색상 액센트 바로 구분된 그룹 카드
- sticky 섹션 헤더 (스크롤 시 상단 고정)
- 우측 하단 FAB(+)으로 할 일 추가

### 아카이브 탭
- 날짜 선택기로 특정 날짜에 완료된 항목 열람
- 기본값은 오늘

### 휴지통
- 삭제된 항목 목록, 복구 및 영구 삭제 지원

---

## 데이터 모델

```kotlin
TaskEntity(
    id              : Long,      // PK, 자동 생성
    title           : String,    // 할 일 제목 (필수)
    description     : String?,   // 부가 메모 (선택)
    isDone          : Boolean,   // 완료 여부
    isDeleted       : Boolean,   // Soft Delete 플래그
    priority        : Int,       // 0=낮음, 1=보통, 2=높음
    showOnLockScreen: Boolean,   // 잠금화면 노출 여부
    dueDate         : Long?,     // 마감 시각 (epoch ms)
    createdAt       : Long,      // 생성 시각
    updatedAt       : Long,      // 마지막 수정 시각
    sortOrder       : Int,       // 드래그 정렬 순서
    repeatType      : RepeatType,// NONE / DAILY / WEEKLY / MONTHLY
    reminderMinutes : Int?,      // 사전 알림 (분), null = 없음
)
```

---

## 디자인 시스템

| 항목 | 값 |
|---|---|
| 주 색상 (Primary) | Deep Indigo `#4F46E5` |
| 배경 | Cool Gray `#F2F2F7` |
| 카드 표면 | White `#FFFFFF` |
| 카드 테두리 | `#E5E7EB` 1dp |
| 카드 코너 반경 | 16dp |
| 우선순위 높음 | Red `#EF4444` |
| 우선순위 보통 | Amber `#F59E0B` |
| 우선순위 낮음 | Gray `#9CA3AF` |
| 완료 카드 배경 | Light Blue `#E3F2FD` |

---

## 빌드 및 실행

**요구 사항**
- Android Studio Hedgehog 이상
- JDK 17
- Android 8.0 (API 26) 이상 기기 또는 에뮬레이터

```bash
git clone https://github.com/kanghyeonLee/ToDoList.git
cd ToDoList
./gradlew assembleDebug
```

릴리즈 APK는 `app/release/` 디렉토리에서 확인할 수 있습니다.

---

## 버전 히스토리

| 버전 | 주요 변경 |
|---|---|
| v1.2.1 | 프리미엄 UI 리뉴얼 (딥 인디고 테마, 원형 체크박스, 섹션 헤더), 할 일 작성 draft 유지 |
| v1.2.0 | 완료 토글 및 스와이프 삭제 구현 |
| v1.1.x | 아카이브 탭, 날짜별 완료 필터링 |
| v1.0.x | 기본 CRUD, 우선순위 분류, 알림 |
