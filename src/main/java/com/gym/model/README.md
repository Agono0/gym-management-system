# `model` — Entity Classes (Swing branch)

## Purpose
Plain Java POJOs representing every domain entity — **identical to the
`main` branch**. No Swing, no SQL, no business logic.

## Contents (ported from `main` after merge there)
| Class | Owner |
|-------|-------|
| `Customer.java` | Abdelrhman |
| `Trainer.java` | Ziad |
| `Membership.java` | Ziad |
| `WorkoutProgram.java`, `Session.java`, `Equipment.java` | Yousef |
| `Attendance.java`, `Payment.java`, `ProgressRecord.java` | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Port each entity from `main` once merged there | Each owner (own classes) | To Do |

Rule: copy, don't redesign — any behavioral difference vs `main` is a bug.
