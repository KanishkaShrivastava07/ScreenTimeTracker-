import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScreenTimeTracker {

    private static final String LOG_FILE = "usage_logs.csv"; // file to store usage logs
    private static boolean tracking = false; // flag to check if tracking is ongoing
    private static List<String> lastApps = new ArrayList<>(); // stores apps from last check
    private static int idleCount = 0; // counts consecutive idle intervals
    private static int idleMins = 5; // minutes to consider user idle (default)

    // start tracking method
    public static void startTracking(int intervalMinutes, boolean liveReport) {
        tracking = true; // enable tracking
        System.out.println("tracking started....(type 'stop' to end)"); // inform user

        // thread to listen for 'stop' command from user
        new Thread(() -> {
            Scanner sc = new Scanner(System.in); // scanner for user input
            while (tracking) { // loop until tracking stops
                if (sc.nextLine().trim().equalsIgnoreCase("stop")) { // if user types 'stop'
                    tracking = false; // stop tracking
                    System.out.println("tracking stopped."); // notify user
                    break;
                }
            }
        }).start(); // start the thread

        while (tracking) { // main tracking loop
            try {
                List<String> apps = getRunningApps(); // get currently running apps

                // idle detection: if apps same as last check
                if (apps.equals(lastApps)) {
                    idleCount++; // increase idle counter
                    if (idleCount >= idleMins) { // if idle threshold reached
                        apps = Collections.singletonList("IDLE"); // mark as idle
                    }
                } else {
                    idleCount = 0; // reset idle counter if activity detected
                }

                logUsage(apps); // log current apps to csv
                lastApps = new ArrayList<>(apps); // update lastApps

                if (liveReport) { // if live report enabled
                    generateReport(false); // print today's report
                }

                Thread.sleep(intervalMinutes * 60 * 1000L); // wait for interval
            } catch (Exception e) {
                e.printStackTrace(); // print any exceptions
            }
        }
    }

    // get running apps for current os
    private static List<String> getRunningApps() throws IOException {
        List<String> apps = new ArrayList<>(); // list to store apps
        String os = System.getProperty("os.name").toLowerCase(); // detect os
        ProcessBuilder pb;

        if (os.contains("win")) { // windows
            pb = new ProcessBuilder("tasklist"); // use tasklist command
        } else { // linux/mac
            pb = new ProcessBuilder("ps", "-e", "-o", "comm="); // get command names
        }

        Process process = pb.start(); // start the process
        Scanner sc = new Scanner(process.getInputStream()); // read output

        while (sc.hasNextLine()) { // loop through each line
            String line = sc.nextLine().trim(); // trim whitespace
            if (line.isEmpty()) continue; // skip empty lines

            if (os.contains("win")) { // windows: filter .exe
                if (line.toLowerCase().contains(".exe")) {
                    apps.add(line.split(" ")[0].trim()); // add executable name
                }
            } else { // linux/mac
                apps.add(line); // add command name directly
            }
        }
        sc.close(); // close scanner
        return apps; // return list of apps
    }

    // log app usage to csv
    private static void logUsage(List<String> apps) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) { // append mode
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); // current timestamp
            for (String app : apps) { // loop through apps
                // escape quotes in app name and write csv line
                bw.write(String.format("\"%s\",\"%s\",1", timestamp, app.replace("\"", "\"\"")));
                bw.newLine(); // new line per app
            }
        } catch (IOException e) {
            e.printStackTrace(); // print exceptions
        }
    }

    // generate daily or weekly report
    private static void generateReport(boolean weekly) {
        Map<String, Integer> usage = new HashMap<>(); // map app -> total minutes
        Date now = new Date(); // current date
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        Date weekAgo = null;

        if (weekly) { // if weekly report
            cal.add(Calendar.DAY_OF_MONTH, -7); // 7 days ago
            weekAgo = cal.getTime();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(LOG_FILE))) { // read csv
            String line;
            SimpleDateFormat full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // for parsing timestamp
            SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd"); // for daily comparison

            while ((line = br.readLine()) != null) { // read each line
                String[] parts = parseCSV(line); // parse csv line
                if (parts.length < 3) continue; // skip invalid lines

                Date logDate = full.parse(parts[0]); // parse timestamp
                String app = parts[1]; // app name
                int duration = Integer.parseInt(parts[2]); // duration in minutes

                // include in report if within date range
                if (weekly) {
                    if (!logDate.before(weekAgo) && !logDate.after(now)) {
                        usage.put(app, usage.getOrDefault(app, 0) + duration);
                    }
                } else { // daily report
                    if (day.format(logDate).equals(day.format(now))) {
                        usage.put(app, usage.getOrDefault(app, 0) + duration);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // print exceptions
        }

        // sort apps by usage descending
        List<Map.Entry<String, Integer>> list = new ArrayList<>(usage.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());

        // print report header
        System.out.println((weekly ? "weekly" : "daily") + " report:");
        System.out.println("-------------------------");

        int maxUsage = list.isEmpty() ? 0 : list.get(0).getValue(); // for scaling bars
        int count = 0;

        for (Map.Entry<String, Integer> entry : list) { // print top 5 apps
            if (count >= 5) break;
            int minutes = entry.getValue(); // total minutes
            int barLen = maxUsage == 0 ? 0 : (int) ((minutes / (double) maxUsage) * 30); // scale bar
            String bar = "#".repeat(barLen); // create ascii bar
            System.out.printf("%-20s : %3d min |%s%n", entry.getKey(), minutes, bar); // print line
            count++;
        }
        System.out.println(); // blank line
    }

    // csv parsing handling quotes
    private static String[] parseCSV(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') { // toggle inQuotes on quote
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) { // comma outside quotes
                values.add(sb.toString()); // add value
                sb.setLength(0); // reset buffer
            } else {
                sb.append(c); // append character
            }
        }
        values.add(sb.toString()); // add last value
        return values.toArray(new String[0]); // convert to array
    }

    // show cli help
    private static void showHelp() {
        System.out.println("screen time tracker commands:");
        System.out.println(" start [interval] [idle] [live] -> start tracking (interval in minutes, idle threshold, live report true/false)");
        System.out.println(" report_day                      -> show today's report");
        System.out.println(" report_week                     -> show last 7 days report");
        System.out.println(" help                            -> show commands");
        System.out.println(" stop                            -> stop tracking");
    }

    // main entry point
    public static void main(String[] args) {
        if (args.length == 0) { // no arguments, show help
            showHelp();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                int interval = 1; // default interval
                if (args.length >= 2) {
                    try { interval = Integer.parseInt(args[1]); } catch (Exception ignored) {}
                }
                if (args.length >= 3) {
                    try { idleMins = Integer.parseInt(args[2]); } catch (Exception ignored) {}
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
                System.out.println("tracking stopped.");
                break;
            default:
                System.out.println("unknown command.");
                showHelp(); // show help on invalid command
        }
    }
}
