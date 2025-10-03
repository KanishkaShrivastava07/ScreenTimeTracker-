# Screen Time Tracker

## Description
a java command-line application to track computer screen time by monitoring running apps. it logs app usage to a csv file and generates daily or weekly reports with top-used apps and ascii bar charts. it also detects idle periods and allows configurable tracking intervals.

---

## Features
- tracks running apps on **windows, linux, and mac**  
- **idle detection**: marks user as idle after configurable minutes  
- logs usage to **usage_logs.csv**  
- generates **daily** and **weekly** reports  
- **ascii bar charts** for visual representation of app usage  
- configurable **tracking interval** and **idle threshold**  
- **live report** option for real-time updates  
- **stop command** to gracefully end tracking  

---

## Commands

| Command | Description |
|---------|------------|
| `start [interval] [idle] [live]` | start tracking (interval in minutes, idle threshold in minutes, live report: true/false) |
| `report_day` | show today’s usage report |
| `report_week` | show last 7 days usage report |
| `stop` | stop tracking |
| `help` | show all commands |

---

## Usage Example

- start tracking every 1 minute, idle after 5 minutes, live report enabled:
```bash
java ScreenTimeTracker start 1 5 true
show today’s report:

bash
Copy code
java ScreenTimeTracker report_day
show last 7 days report:

bash
Copy code
java ScreenTimeTracker report_week
stop tracking while it’s running: type stop in console

Requirements
java 8 or higher

works on windows, linux, mac

terminal or command prompt

Notes
logs are stored in usage_logs.csv in the same directory

app names containing commas are handled correctly

live report prints the top 5 apps every interval

Project Flow Diagram
sql
Copy code
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
