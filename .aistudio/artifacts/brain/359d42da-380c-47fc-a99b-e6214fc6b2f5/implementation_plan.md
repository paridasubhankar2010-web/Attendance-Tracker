# Enhanced Daily Attendance Screen with Switch Toggles & Calendar Picker

Transform the daily attendance screen into a streamlined, high-speed logging experience with binary Switch toggles (Present/Absent), a Material 3 calendar modal picker, and a bulk "Mark All Present" action.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during Phase 1:
> - **Toggle Style**: Dedicated **Switch toggle switch** on each subject card for toggling between Present and Absent states.
> - **Date Picker Experience**: Full **Material 3 Calendar Modal Picker (`DatePickerDialog`)** combined with quick Previous/Next day navigation controls.
> - **Bulk Logging Action**: A prominent **"Mark All Present"** button for recording all scheduled classes in one tap.

---

## 1. Overview & Core Concept

College students often attend back-to-back lectures and need to log their attendance quickly without navigating complex sub-menus. This feature enhances the **Daily Log** experience to provide:
1. **Material 3 Calendar Dialog**: Instant selection of any past semester date or present day via a date picker dialog, backed by previous/next day stepping arrows and a quick "Today" reset chip.
2. **Present/Absent Switch Toggle**: A clean, responsive binary switch on each subject card:
   - **ON (Checked)**: Marked **Present** (Emerald green accent, check icon, increments attended & total).
   - **OFF (Unchecked)**: Marked **Absent** (Crimson accent, increments total only).
   - **Optional Reset / Not Held**: A subtle reset affordance for days when classes were cancelled or not held.
3. **Bulk "Mark All Present" Action**: One-tap action bar at the top of the day view to mark all enrolled subjects as present simultaneously, with instant undo and count updates.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────┐
│  Daily Attendance Log Screen                           │
├────────────────────────────────────────────────────────┤
│  [ < ]    Thursday, October 30, 2026    [ 📅 ]   [ > ] │
│           [ Jump to Today ]                            │
├────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐  │
│  │ ⚡ Bulk Action:  [ ✓ Mark All Present ]           │  │
│  │ Summary: 3/4 Marked  •  3 Present  •  0 Absent    │  │
│  └──────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────┤
│  SUBJECT CARDS WITH SWITCH TOGGLE                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ● Data Structures & Algorithms           [ 86% ] │  │
│  │ CS201 • Room 302 • Goal: 75%                     │  │
│  │                                                  │  │
│  │ Status: Present ✓         [=== (●) ] ON          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ● Operating Systems                      [ 65% ] │  │
│  │ CS204 • Hall B • Goal: 75% (Below Goal!)         │  │
│  │                                                  │  │
│  │ Status: Absent ✗          [ (●) ===] OFF         │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme Tokens
- **Theme Palette**: Deep Indigo (`#2563EB`) primary, Emerald Green (`#10B981`) for Present / Safe state, Crimson (`#EF4444`) for Absent / Below Goal state.
- **Switch Styling**: Material 3 `Switch` with custom thumb and track colors (`StatusSafe` when checked, `StatusCritical` or muted container when unchecked), accompanied by explicit text labels ("Present" / "Absent") to avoid ambiguity.
- **DatePickerDialog**: Native Material 3 `DatePickerDialog` with `rememberDatePickerState`, pre-selected to the currently viewed date, allowing students to inspect or adjust any past day in the semester.

---

## 3. Key Product Decisions & Trade-Offs

- **Switch Toggle with Tri-State Fallback**:
  - *Chosen Approach*: When a day is not yet recorded, the card displays an unrecorded prompt; tapping the Switch immediately registers Present (ON) or Absent (OFF). Once toggled, switching toggles between Present and Absent. A small reset icon is provided if a student marked a class that was cancelled or not held.
  - *Why*: Gives the direct, clean binary toggle the user requested while preventing false zeros on unheld class days.
- **Batch Repository Transaction for "Mark All Present"**:
  - *Chosen Approach*: Executes bulk update inside a single coroutine/transaction across all subjects in the local Room database, updating both logs and cumulative counters consistently.
  - *Why*: Prevents UI stutter and avoids race conditions in statistics calculation.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                         │
│                                                                   │
│   DailyLogScreen                                                  │
│   ├── DateNavigatorBar (Previous/Next + DatePickerDialog modal)   │
│   ├── BulkActionBar ("Mark All Present" + Daily Progress)        │
│   └── LazyColumn                                                  │
│       └── DailySubjectSwitchCard (M3 Switch + Subject Status)     │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │ Actions & StateFlows
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                       AttendanceViewModel                         │
│   ├── selectedDateEpoch: StateFlow<Long>                          │
│   ├── dailyLogs: StateFlow<List<AttendanceLog>>                   │
│   ├── markDailyPresence(subjectId, status)                        │
│   ├── markAllPresent(dateEpochDay)                                │
│   └── clearDailyPresence(subjectId)                               │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │ Repository Pattern
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                      AttendanceRepository                         │
│   ├── markAttendance(subjectId, dateEpochDay, status)             │
│   └── markAllPresentForDate(dateEpochDay, subjects)               │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │ Room DAO
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                     AttendanceDatabase (Room)                     │
│   ├── SubjectDao & AttendanceDao                                  │
│   └── tables: subjects, attendance_logs                           │
└───────────────────────────────────────────────────────────────────┘
```

### Implementation Steps
1. **Repository & DAO**: Add `markAllPresentForDate(dateEpochDay: Long, subjects: List<Subject>)` to handle bulk attendance updates cleanly.
2. **ViewModel**: Expose `markAllPresentForDate(dateEpochDay: Long)` and handle feedback messages.
3. **DailyLogScreen Component**:
   - Replace three-button row with an intuitive Material 3 `Switch` component showing "Present" (ON) vs "Absent" (OFF) with status badge.
   - Integrate M3 `DatePickerDialog` triggered by a calendar icon button on the top date bar.
   - Add the "Mark All Present" action button with confirmation/undo toast.
4. **Verification**: Run `compile_applet` and unit tests to verify compilation and state transitions.
