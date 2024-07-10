# MiniLeaderboardMachine

A tool to create a new mini-leaderboard from the players on any leaderboard available on the Hypixel website, using user-specified sets of statistics from the Hypixel API.

Requires jdk-21. Not for any good reason, just used some newer features auto-suggested by IntelliJ and now CBA to switch them back. Compile with shadowJar if you might not have the dependencies in your classpath.

Put the jar in a directory, open up Powershell or something, do `java -jar <name of .jar file> -help` to get help, or replace `-help` with necessary arguments to actually run the dang thing.

Example usage: `java -jar mlm.jar -apikey=9a080be7-157c-408e-a127-8f6f4425383f -playerCount=100 -fileName=output -posPaths=player~stats~Duels~bridge_duel_blocks_placed*player~stats~Duels~bridge_doubles_blocks_placed*player~stats~Duels~bridge_threes_blocks_placed*player~stats~Duels~bridge_four_blocks_placed*player~stats~Duels~bridge_2v2v2v2_blocks_placed*player~stats~Duels~bridge_3v3v3v3_blocks_placed*player~stats~Duels~capture_threes_blocks_placed -hypixelDelay=5000 -debug=true -hypixelDirect=true`

Outputs are in the form of a csv file in the same directory as the jar.

For further support, join my discord: https://discord.gg/UTUawMz
