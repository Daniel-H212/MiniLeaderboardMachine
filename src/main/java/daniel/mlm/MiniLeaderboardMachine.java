package daniel.mlm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MiniLeaderboardMachine {

    // Hypixel API key
    private static String apikey = null;

    // The URL of the Hypixel leaderboard webpage being scraped
    private static String URL = "https://hypixel.net/duels/leaderboard/bridge";

    // Number of players from the leaderboard to check, starting from the top
    private static int playerCount = 100;

    // Integer of delay between Mojang API requests, in milliseconds
    private static int mojangDelay = 2000;

    // Integer of delay between Hypixel API requests, in milliseconds
    private static int hypixelDelay = 2000;

    // String array of paths of stats to positively include
    private static String[] posPaths = new String[]{"player.stats.Duels.bridge_duel_wins",
            "player.stats.Duels.bridge_doubles_wins",
            "player.stats.Duels.bridge_threes_wins",
            "player.stats.Duels.bridge_four_wins",
            "player.stats.Duels.bridge_2v2v2v2_wins",
            "player.stats.Duels.bridge_3v3v3v3_wins",
            "player.stats.Duels.capture_threes_wins"};

    // String array of paths of stats to negatively include
    private static String[] negPaths = new String[0];

    // Boolean of whether stats are totalled multiplicatively or additively
    private static boolean multiplicative = false;

    // Integer number of decimals to preserve in final processed stats
    private static int decimals = 1;

    // Boolean whether to order list from least to most
    private static boolean reverse = false;

    // Name of output file
    private static String fileName = null;

    // Shows progress...

    private static boolean showProgress = false;

    public static void runMachine(String[] args) {
        if(!processArgs(args)){
            return;
        }
        System.out.println("API key: " + apikey);
        if(showProgress) System.out.println("Getting page.");
        Document doc = getPage();
        if(doc == null) return;
        if(showProgress) System.out.println("Getting players.");
        ArrayList<String> players = getPlayers(doc);
        if(players.isEmpty()) return;
        if(showProgress) System.out.println("Getting uuid.");
        HashMap<String, String> uuidList = getUUIDList(players);
        if(showProgress) System.out.println("Getting stats.");
        HashMap<String, JsonElement> statsList = getStatsList(uuidList);
        if(showProgress) System.out.println("Creating leaderboard.");
        HashMap<String, Double> leaderboard = createLeaderboard(statsList);
        if(showProgress) System.out.println("Sorting leaderboard.");
        ArrayList<String> sortedLeaderboard = sortLeaderboard(leaderboard);
        if(showProgress) System.out.println("Checking file name.");
        checkFileName();
        if(showProgress) System.out.println("Writing to file.");
        writeToFile(sortedLeaderboard);
    }

    /*
     * Reads args into usable format and checks for errors
     * @param args               Arguments entered by the user
     * @return                   Whether processing was successful
     * */
    private static boolean processArgs(String[] args) {
        for(String arg : args){
            if(arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")){
                printHelpMessage();
                return false;
            }
        }
        if(!readArgs(args)) return false;
        if(apikey == null){
            System.out.println("Missing required argument: -apikey, use -help to learn more.");
            return false;
        }
        return true;
    }

    /*
    * Reads args into usable format
    * @param args               Arguments entered by the user
    * @return                   Whether a valid api key was given
    * */
    private static boolean readArgs(String[] args) {
        for(String arg : args){
            if(arg.startsWith("-apikey")){
                if(arg.contains("=")){
                    apikey = arg.substring(arg.indexOf("=") + 1);
                    if(!UUID.fromString(apikey).toString().equalsIgnoreCase(apikey)){
                        System.out.println("API key not valid, use -help to learn more.");
                        return false;
                    }
                }
                else{
                    System.out.println("API key not valid, use -help to learn more.");
                    return false;
                }
            }
            else if(arg.startsWith("-playerCount")){
                try{
                    playerCount = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse player count, use -help to learn more. Defaulting to: " + playerCount + ".");
                }
            }
            else if(arg.startsWith("-mojangDelay")){
                try{
                    mojangDelay = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse Mojang delay, use -help to learn more. Defaulting to: " + mojangDelay + ".");
                }
            }
            else if(arg.startsWith("-hypixelDelay")){
                try{
                    hypixelDelay = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse Hypixel delay, use -help to learn more. Defaulting to: " + hypixelDelay + ".");
                }
            }
            else if(arg.startsWith("-posPaths")){
                if(arg.contains("=")) {
                    posPaths = arg.substring(arg.indexOf("=") + 1).replaceAll(" ", "").split(",");
                    if (posPaths.length == 0) {
                        System.out.println("Positive paths cannot be empty, use -help to learn more. Using default values.");
                        posPaths = new String[]{"player.stats.Duels.bridge_duel_wins",
                                "player.stats.Duels.bridge_doubles_wins",
                                "player.stats.Duels.bridge_threes_wins",
                                "player.stats.Duels.bridge_four_wins",
                                "player.stats.Duels.bridge_2v2v2v2_wins",
                                "player.stats.Duels.bridge_3v3v3v3_wins",
                                "player.stats.Duels.capture_threes_wins"};
                    }
                }
            }
            else if(arg.startsWith("-negPaths")){
                if(arg.contains("=")){
                    negPaths = arg.substring(arg.indexOf("=") + 1).replaceAll(" ", "").split(",");
                }
            }
            else if(arg.startsWith("-multiplicative")){
                try{
                    multiplicative = Boolean.parseBoolean(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse multiplicative, use -help to learn more. Defaulting to: " + multiplicative + ".");
                }
            }
            else if(arg.startsWith("-decimals")){
                try{
                    decimals = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                    if(decimals < 0 || decimals > 10){
                        decimals = 1;
                        System.out.println("Invalid number of decimals, use -help to learn more. Defaulting to: " + decimals + ".");
                    }
                }
                catch(Exception e){
                    System.out.println("Could not parse decimals, use -help to learn more. Defaulting to: " + decimals + ".");
                }
            }
            else if(arg.startsWith("-reverse")){
                try{
                    reverse = Boolean.parseBoolean(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse reverse, use -help to learn more. Defaulting to: " + reverse + ".");
                }
            }
            else if(arg.startsWith("-showProgress")){
                try{
                    showProgress = Boolean.parseBoolean(arg.substring(arg.indexOf("=") + 1));
                }
                catch(Exception e){
                    System.out.println("Could not parse showProgress, use -help to learn more. Defaulting to: " + showProgress + ".");
                }
            }
            else if(arg.startsWith("-fileName")){
                if(arg.contains("=")){
                    fileName = arg.substring(arg.indexOf("=") + 1);
                }
            }
        }
        return true;
    }

    /*
     * Prints the help message.
     * */
    private static void printHelpMessage() {
        System.out.println("""
                MiniLeaderboardMachine by Daniel_H212 is a tool to create a new mini-leaderboard from the players on any
                leaderboard available on the Hypixel website, using user-specified sets of statistics from the Hypixel API.
                
                Version: 1.0
                
                Usage: mlm.jar -apikey=<your Hypixel api key> [options]
                    -help                           Prints this message.
                    -URL=ARG                        The full URL of the Hypixel leaderboard page of players whose stats
                                                    you want to check.
                                                        Defaults to bridge duels wins leaderboard.
                    -playerCount=#                  The number of players from the leaderboard to check.
                                                        Defaults to 100.
                    -hypixelDelay=#                 The delay between each query to the Hypixel API in milliseconds.
                                                        Defaults to 2000, min 1000.
                    -mojangDelay=#                  The delay between each query to the Mojang API in milliseconds.
                                                        Defaults to 2000, min 1000.
                    -posPaths=ARG                   Comma-separated list of paths of stats that you would like to be
                                                    positively included in the leaderboard you wish to create.
                                                        Defaults to bridge overall wins.
                                                        Example path: player.stats.Duels.bridge_duel_wins
                                                        (this example is bridge solo wins)
                    -negPaths=ARG                   Comma-separated list of paths of stats that you would like to be
                                                    negatively included in the leaderboard you wish to create.
                                                        Defaults to empty.
                    -multiplicative=true|false      If true, the stats from the positive paths will be multiplied
                                                    together, then divided by the stats from the negative paths.
                                                    If false, the stats from the positive paths will be added together,
                                                    then the stats from the negative paths will be subtracted.
                                                        Defaults to false.
                    -decimals=#                     The decimal precision of stats.
                                                        Defaults to 1, min 0, max 10.
                    -reversed                       If true, leaderboard will be ordered from least to most. If false,
                                                    leaderboard will be ordered from most to least.
                                                        Defaults to false.
                    -fileName=ARG                   The name of the output file.
                                                        Defaults to date-time.csv
                
                To find the paths to the stats you want, try manually entering this URL to your browser with the
                required fields (marked with <>) filled in:
                https://api.hypixel.net/player?name=<username>&key=<apikey>
                I suggest using a Json formatter and something like Notepad++ to view the Json output.
                
                For further support, join my discord: https://discord.gg/UTUawMz
                """);
    }


    /*
     * @return                  Hypixel leaderboard webpage being scraped
     * */
    private static Document getPage(){
        try {
            return Jsoup.connect(URL).userAgent("Mozilla").referrer("http://www.google.com").get();
        } catch (IOException e) {
            if(e instanceof HttpStatusException); //placeholder
            System.out.println("Unable to access requested URL.");
            e.printStackTrace();
            return null;
        }
    }

    /*
     * @param doc               The Document of the Hypixel leaderboard webpage being scraped
     * @return                  List of usernames on the page
     * */
    private static ArrayList<String> getPlayers(Document doc){
        ArrayList<String> playerList = new ArrayList<>();
        Element lbTable = doc.getElementsByClass("leaderboard").getFirst();
        Elements players = lbTable.getElementsByClass("player");
        try {
            for (int i = 0; i < playerCount && i < players.size(); i++) {
                Element player = players.get(i);
                System.out.println(player);
                Elements links = player.select("a");
                if (links.isEmpty()) {
                    playerCount++;
                    continue;
                }
                try {
                    playerList.add(links.first().text().replaceAll(" ", ""));
                } catch (NullPointerException e) {
                    playerCount++;
                    System.out.println("No player found for table element: " + player);
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            System.out.println("Failed to finish processing player list after " + playerList.size() + " players.");
        }
        return playerList;
    }

    /*
     * @param names             ArrayList of usernames
     * @return                  Name - UUID pairs
     * */
    private static HashMap<String, String> getUUIDList(ArrayList<String> names){
        HashMap<String, String> UUIDList = new HashMap<>();
        for(String name : names){
            if(showProgress) System.out.println("Getting UUID for " + name + ".");
            UUIDList.put(name, getUUID(name));
            try {
                Thread.sleep(mojangDelay);
            } catch (InterruptedException e) {
                System.out.println("Thread.sleep() fucked up somehow, idk.");
                throw new RuntimeException(e);
            }
        }
        return UUIDList;
    }

    /*
     * @param name              The username of a player
     * @return                  Player UUID according to Mojang
     * */
    private static String getUUID(String name){
        String api = "https://api.mojang.com/users/profiles/minecraft/" + name;
        try {
            URL url = new URL(api);
            URLConnection request = url.openConnection();
            request.connect();
            JsonParser jp = new JsonParser();
            JsonObject root = jp.parse((Reader)new InputStreamReader((InputStream)request.getContent())).getAsJsonObject();
            return root.get("id").getAsString();
        }
        catch (Exception e) {
            System.out.println("Failed to obtain UUID for username: " + name);
            e.printStackTrace();
            return null;
        }
    }

    /*
     * @param UUIDList          HashMap of name - UUID pairs
     * @return                  Name - raw stats pairs
     * */
    private static HashMap<String, JsonElement> getStatsList(HashMap<String, String> UUIDList) {
        HashMap<String, JsonElement> statsList = new HashMap<>();
        for(String name : UUIDList.keySet()){
            if(showProgress) System.out.println("Getting stats for " + name + ".");
            statsList.put(name, getStats(UUIDList.get(name)));
            try {
                Thread.sleep(hypixelDelay);
            } catch (InterruptedException e) {
                System.out.println("Thread.sleep() fucked up somehow, idk.");
                e.printStackTrace();
            }
        }
        return statsList;
    }

    /*
     * @param UUID              The UUID of the player being checked
     * @return                  Player raw stats according to Hypixel API
     * */
    private static JsonElement getStats(String UUID) {
        String baseUrl = "https://api.hypixel.net/v2/player";
        try{
            URIBuilder uriBuilder = new URIBuilder(baseUrl)
                    .addParameter("uuid", UUID);
            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(uriBuilder.build());
            request.addHeader("API-Key", apikey);
            HttpResponse response = client.execute(request);
            JsonParser parser = new JsonParser();
            Reader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
            JsonObject rootobj = parser.parse(reader).getAsJsonObject();
            return rootobj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * @param statsList         Hashmap of name - raw stats pairs
     * @return                  Name - processed stat pairs
     * */
    private static HashMap<String, Double> createLeaderboard(HashMap<String, JsonElement> statsList) {
        HashMap<String, Double> leaderboard = new HashMap<>();
        for(String name : statsList.keySet()){
            leaderboard.put(name, roundTo(processStats(name, statsList.get(name))));
        }
        return leaderboard;
    }

    /*
     * @param playerStats       JsonElement of player raw stats
     * @return                  Final processed stat of the player
     * */
    private static double processStats(String name, JsonElement playerStats) {
        double finalStat = multiplicative ? 1.0 : 0.0;
        for(int i = 0; i < posPaths.length; i++){
            finalStat = multiplicative ?
                    finalStat * findStat(name, playerStats, posPaths[i], 1.0) :
                    finalStat + findStat(name, playerStats, posPaths[i], 0.0);
        }
        for(int i = 0; i < negPaths.length; i++){
            finalStat = multiplicative ?
                    finalStat / findStat(name, playerStats, posPaths[i], 1.0) :
                    finalStat - findStat(name, playerStats, posPaths[i], 0.0);
        }
        return finalStat;
    }

    /*
     * @param playerStats       JsonElement of player raw stats
     * @param path              String of the path of the
     * @param defaultValue      Default value in case stat at specified path doesn't exist or is invalid
     * @return                  Stat specified by path
     * */
    private static double findStat(String name, JsonElement playerStats, String path, double defaultValue) {
        String[] fullPath = path.split("\\.");
        JsonElement stat = playerStats;
        double output;
        try {
            for (String piece : fullPath) {
                stat = stat.getAsJsonObject().get(piece);
            }
            if(stat.isJsonPrimitive() && stat.getAsJsonPrimitive().isNumber()){
                output = stat.getAsNumber().doubleValue();
            }
            else{
                output = defaultValue;
                System.out.println("Stat at: " + path + "\nwas not a number for player " + name + ", defaulting to " + output + ".");
            }
        }
        catch(Exception e){
            output = defaultValue;
            System.out.println("Stat at: " + path + "\nwas not found for player " + name + ", defaulting to " + output + ".");
        }
        return output;
    }

    /*
     * @param num               Double number to round
     * @return                  Rounded number
     * */
    private static double roundTo(double num){
        if(decimals == 0){
            return Math.round(num);
        }
        DecimalFormat df = new DecimalFormat("#." + "#".repeat(decimals));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(num));
    }

    /*
     * Selection sort because I am a lazy fuck
     * @param leaderboard       HashMap of name - processed stat pairs
     * @return                  List of stats with numbering
     * */
    private static ArrayList<String> sortLeaderboard(HashMap<String, Double> leaderboard) {
        ArrayList<String> sortedLeadereboard = new ArrayList<>();
        int i = 1;
        while(!leaderboard.isEmpty()){
            String best = null;
            for(String name : leaderboard.keySet()){
                if(best == null || (reverse ^ leaderboard.get(name) > leaderboard.get(best))){
                    best = name;
                }
            }
            sortedLeadereboard.add(i + "," + best + "," + leaderboard.get(best));
            leaderboard.remove(best);
            i++;
        }
        return sortedLeadereboard;
    }

    /*
     * Selection sort because I am a lazy fuck
     * @param leaderboard       HashMap of name - processed stat pairs
     * @return                  List of stats with numbering
     * */
    private static void checkFileName() {
        if(fileName == null){
            LocalDateTime time = LocalDateTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            fileName = timeFormatter.format(time) + ".csv";
            return;
        }
        if(!fileName.endsWith(".csv")) fileName += ".csv";
        File output = new File(fileName);
        while(output.exists() && !output.isDirectory()){
            fileName = fileName.substring(0, fileName.lastIndexOf(".csv")) + "-.csv";
            output = new File(fileName);
        }
    }

    /*
     * Selection sort because I am a lazy fuck
     * @param leaderboard       HashMap of name - processed stat pairs
     * @return                  List of stats with numbering
     * */
    private static void writeToFile(ArrayList<String> sortedLeaderboard) {
        File output = new File(fileName);

        try {
            output.createNewFile();
        }
        catch (Exception e) {
            System.out.println("Unable to create output file to write to. Printing results instead");
            printResults(sortedLeaderboard);
            return;
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(output);
        }
        catch (FileNotFoundException e) {
            System.out.println("Unable to write to output file. Printing results instead");
            printResults(sortedLeaderboard);
            return;
        }
        for (String entry : sortedLeaderboard) {
            out.println(entry);
        }
        out.close();
    }

    private static void printResults(ArrayList<String> sortedLeaderboard) {
        for(String entry : sortedLeaderboard) System.out.println(entry);
    }
}
