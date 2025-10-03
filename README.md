# Screen Time Tracker

📌 **Description**
A Java command-line application to track computer screen time by monitoring running apps.
It logs app usage to a CSV file and generates daily or weekly reports with top-used apps and ASCII bar charts.
It also detects idle periods and allows configurable tracking intervals.

---

## 🚀 Features

* Tracks running apps on **Windows, Linux, and Mac**
* **Idle detection**: marks user as idle after configurable minutes
* Logs usage to `usage_logs.csv`
* Generates **daily and weekly reports**
* **ASCII bar charts** for visual representation of app usage
* Configurable tracking interval and idle threshold
* **Live report** option for real-time updates
* **Stop command** to gracefully end tracking

---

## ⚡ Commands

| Command                          | Description                                                                              |
| -------------------------------- | ---------------------------------------------------------------------------------------- |
| `start [interval] [idle] [live]` | Start tracking (interval in minutes, idle threshold in minutes, live report: true/false) |
| `report_day`                     | Show today’s usage report                                                                |
| `report_week`                    | Show last 7 days usage report                                                            |
| `stop`                           | Stop tracking                                                                            |
| `help`                           | Show all commands                                                                        |

---

## 📂 Usage Examples

Start tracking every 1 minute, idle after 5 minutes, live report enabled:

```bash
java ScreenTimeTracker start 1 5 true
```

Show today’s report:

```bash
java ScreenTimeTracker report_day
```

Show last 7 days report:

```bash
java ScreenTimeTracker report_week
```

Stop tracking while it’s running:

```bash
type stop in console
```

---

## 🖥 Requirements

* Java 8 or higher
* Works on **Windows, Linux, Mac**
* Terminal or Command Prompt

---

## 📝 Notes

* Logs are stored in `usage_logs.csv` in the same directory
* App names containing commas are handled correctly
* Live report prints the **top 5 apps** every interval

---

## 🔄 Project Flow Diagram

```
start program
      |
parse CLI args (interval, idle, live report)
      |
start tracking
      |
get running apps (windows/linux/mac)
      |
compare with last apps (idle detection)
      |
log apps to CSV
      |
print live report (optional)
      |
wait interval
      |
check 'stop' command
      |
stop tracking
      |
generate reports (daily/weekly)
```
