import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScreenTimeTracker {

    private static final String LOG_FILE = "usage_logs.csv"; // file to store usage logs
    private static boolean tracking = false; // flag to check if tracking is running
    private static List<String> lastApps = new ArrayList<>(); // stores apps from last check
    private static int idleCount = 0; // counts consecutive idle intervals
    private static int idleMins = 5; // default idle minutes

    // method to ensure log file exists with headers
    private static void initLogFile() {
        File file = new File(LOG_FILE); // create file object
        if (!file.exists()) { // if file does not exist
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) { // create file writer
                bw.write("timestamp,app,minutes"); // write header
                bw.newLine(); // move to next line
            } catch (IOException e) { // handle exception
                e.printStackTrace();
            }
        }
    }

    // method to start tracking apps
    public static void startTracking(int intervalMinutes, boolean liveReport) {
        tracking = true; // set tracking to true
        initLogFile(); // ensure log file is created
        System.out.println("tracking started....(type 'stop' to end)"); // show message

        // create thread to listen for 'stop' command
        new Thread(() -> {
            Scanner sc = new Scanner(System.in); // scanner for input
            while (tracking) { // loop while tracking
                if (sc.nextLine().trim().equalsIgnoreCase("stop")) { // if user types stop
                    tracking = false; // stop tracking
                    System.out.println("tracking stopped."); // message
                    System.exit(0); // exit program
                }
            }
        }).start(); // start the thread

        while (tracking) { // main tracking loop
            try {
                List<String> apps = getRunningApps(); // get running apps

                // idle detection: check if apps same as last time
                if (apps.equals(lastApps)) {
                    idleCount++; // increase idle counter
                    if (idleCount >= idleMins) { // if idle threshold crossed
                        apps = Collections.singletonList("IDLE"); // mark as idle
                    }
                } else {
                    idleCount = 0; // reset idle counter if activity detected
                }

                logUsage(apps); // log apps to file
                lastApps = new ArrayList<>(apps); // update last apps list

                if (liveReport) { // if live report enabled
                    generateReport(false); // show daily report instantly
                }

                Thread.sleep(intervalMinutes * 60 * 1000L); // wait for interval
            } catch (Exception e) { // handle errors
                e.printStackTrace();
            }
        }
    }

    // method to get running apps based on os
    private static List<String> getRunningApps() throws IOException {
        List<String> apps = new ArrayList<>(); // list of apps
        String os = System.getProperty("os.name").toLowerCase(); // detect os
        ProcessBuilder pb; // process builder

        if (os.contains("win")) { // windows
            pb = new ProcessBuilder("tasklist"); // use tasklist command
        } else { // linux or mac
            pb = new ProcessBuilder("ps", "-e", "-o", "comm="); // use ps command
        }

        Process process = pb.start(); // start process
        Scanner sc = new Scanner(process.getInputStream()); // read output

        while (sc.hasNextLine()) { // loop through output
            String line = sc.nextLine().trim(); // read line
            if (line.isEmpty()) continue; // skip empty lines

            if (os.contains("win")) { // for windows
                if (line.toLowerCase().contains(".exe")) { // check .exe
                    apps.add(line.split(" ")[0].trim()); // add app name
                }
            } else { // for linux/mac
                apps.add(line); // add directly
            }
        }
        sc.close(); // close scanner
        return apps; // return apps
    }

    // method to log app usage into csv
    private static void logUsage(List<String> apps) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) { // open file in append mode
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); // get timestamp
            for (String app : apps) { // loop through apps
                bw.write(String.format("\"%s\",\"%s\",1", timestamp, app.replace("\"", "\"\""))); // write log line
                bw.newLine(); // move to new line
            }
        } catch (IOException e) { // handle error
            e.printStackTrace();
        }
    }

    // method to generate daily or weekly report
    private static void generateReport(boolean weekly) {
        Map<String, Integer> usage = new HashMap<>(); // map for usage
        Date now = new Date(); // current date
        Calendar cal = Calendar.getInstance(); // calendar
        cal.setTime(now);
        Date weekAgo = null; // date for weekly report

        if (weekly) { // if weekly
            cal.add(Calendar.DAY_OF_MONTH, -7); // subtract 7 days
            weekAgo = cal.getTime(); // set weekAgo
        }

        try (BufferedReader br = new BufferedReader(new FileReader(LOG_FILE))) { // read file
            String line;
            SimpleDateFormat full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // timestamp format
            SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd"); // date format

            br.readLine(); // skip header line

            while ((line = br.readLine()) != null) { // loop through file
                String[] parts = parseCSV(line); // parse csv line
                if (parts.length < 3) continue; // skip invalid

                Date logDate = full.parse(parts[0]); // parse date
                String app = parts[1]; // app name
                int duration = Integer.parseInt(parts[2]); // duration

                // check if entry is in range
                if (weekly) {
                    if (!logDate.before(weekAgo) && !logDate.after(now)) {
                        usage.put(app, usage.getOrDefault(app, 0) + duration); // add minutes
                    }
                } else { // daily report
                    if (day.format(logDate).equals(day.format(now))) {
                        usage.put(app, usage.getOrDefault(app, 0) + duration); // add minutes
                    }
                }
            }
        } catch (Exception e) { // handle error
            e.printStackTrace();
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(usage.entrySet()); // convert map to list
        list.sort((a, b) -> b.getValue() - a.getValue()); // sort by usage descending

        System.out.println((weekly ? "weekly" : "daily") + " report:"); // report header
        System.out.println("-------------------------");

        int maxUsage = list.isEmpty() ? 0 : list.get(0).getValue(); // max usage for scaling
        int count = 0; // counter

        for (Map.Entry<String, Integer> entry : list) { // loop through usage
            if (count >= 5) break; // top 5 only
            int minutes = entry.getValue(); // usage minutes
            int barLen = maxUsage == 0 ? 0 : (int) ((minutes / (double) maxUsage) * 30); // scale bar length
            String bar = "#".repeat(barLen); // create ascii bar
            System.out.printf("%-20s : %3d min |%s%n", entry.getKey(), minutes, bar); // print line
            count++; // increase counter
        }
        System.out.println(); // empty line
    }

    // method to parse csv with quotes
    private static String[] parseCSV(String line) {
        List<String> values = new ArrayList<>(); // list for values
        boolean inQuotes = false; // track if inside quotes
        StringBuilder sb = new StringBuilder(); // string builder

        for (char c : line.toCharArray()) { // loop characters
            if (c == '"') { // toggle quotes
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) { // split on comma outside quotes
                values.add(sb.toString()); // add value
                sb.setLength(0); // reset buffer
            } else {
                sb.append(c); // add char
            }
        }
        values.add(sb.toString()); // add last value
        return values.toArray(new String[0]); // return as array
    }

    // method to show help commands
    private static void showHelp() {
        System.out.println("screen time tracker commands:");
        System.out.println(" start [interval] [idle] [live] -> start tracking (interval in minutes, idle threshold, live report true/false)");
        System.out.println(" report_day                      -> show today's report");
        System.out.println(" report_week                     -> show last 7 days report");
        System.out.println(" help                            -> show commands");
        System.out.println(" stop                            -> stop tracking");
    }

    // main method
    public static void main(String[] args) {
        if (args.length == 0) { // if no arguments
            showHelp(); // show help
            return; // exit
        }

        switch (args[0].toLowerCase()) { // check command
            case "start":
                int interval = 1; // default interval
                if (args.length >= 2) {
                    try { interval = Integer.parseInt(args[1]); } // parse interval
                    catch (Exception e) {
                        System.out.println("invalid interval, using default 1 minute."); // error message
                    }
                }
                if (args.length >= 3) {
                    try { idleMins = Integer.parseInt(args[2]); } // parse idle
                    catch (Exception e) {
                        System.out.println("invalid idle threshold, using default 5 minutes."); // error message
                    }
                }
                boolean liveReport = args.length >= 4 && args[3].equalsIgnoreCase("true"); // live report flag
                startTracking(interval, liveReport); // start tracking
                break;
            case "report_day":
                generateReport(false); // daily report
                break;
            case "report_week":
                generateReport(true); // weekly report
                break;
            case "help":
                showHelp(); // show help
                break;
            case "stop":
                tracking = false; // stop tracking
                System.out.println("tracking stopped."); // message
                System.exit(0); // exit program
                break;
            default:
                System.out.println("unknown command."); // invalid command
                showHelp(); // show help
        }
    }
}
