package daniel.mlm;

public class Main {

    public static void main(String[] args) {
        try {
            MiniLeaderboardMachine.runMachine(args);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}