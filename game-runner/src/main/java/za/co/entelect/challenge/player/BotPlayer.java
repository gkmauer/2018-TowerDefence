package za.co.entelect.challenge.player;

import za.co.entelect.challenge.botrunners.BotRunner;
import za.co.entelect.challenge.core.renderers.TowerDefenseConsoleMapRenderer;
import za.co.entelect.challenge.core.renderers.TowerDefenseJsonGameMapRenderer;
import za.co.entelect.challenge.core.renderers.TowerDefenseTextMapRenderer;
import za.co.entelect.challenge.game.contracts.command.RawCommand;
import za.co.entelect.challenge.game.contracts.map.GameMap;
import za.co.entelect.challenge.game.contracts.player.Player;
import za.co.entelect.challenge.game.contracts.renderer.GameMapRenderer;
import za.co.entelect.challenge.utils.FileUtils;

import java.io.*;
import java.util.Scanner;

public class BotPlayer extends Player {
    private static final String BOT_COMMAND = "command.txt";
    private static final String BOT_STATE = "state.json";
    private GameMapRenderer jsonRenderer;
    private GameMapRenderer textRenderer;
    private GameMapRenderer consoleRenderer;
    private Scanner scanner;
    private BotRunner botRunner;
    private String saveStateLocation;

    public BotPlayer(String name, BotRunner botRunner, String saveStateLocation) {
        super(name);

        scanner = new Scanner(System.in);
        jsonRenderer = new TowerDefenseJsonGameMapRenderer();
        textRenderer = new TowerDefenseTextMapRenderer();
        consoleRenderer = new TowerDefenseConsoleMapRenderer();

        this.botRunner = botRunner;
        this.saveStateLocation = saveStateLocation;
    }

    @Override
    public void startGame(GameMap gameMap) {
        newRoundStarted(gameMap);
    }

    @Override
    public void newRoundStarted(GameMap gameMap) {
        String playerSpecificJsonState = jsonRenderer.render(gameMap, getGamePlayer());
        String playerSpecificTextState = textRenderer.render(gameMap, getGamePlayer());
        String playerSpecificConsoleState = consoleRenderer.render(gameMap, getGamePlayer());
        try {
            runBot(playerSpecificJsonState);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //receive response from bot
        String botInput = "";
        File botCommandFile = new File(String.format("%s/%s", botRunner.getBotDirectory(), BOT_COMMAND));
        Scanner scanner = null;
        try {
            scanner = new Scanner(botCommandFile);
            if (scanner.hasNext()) {
                botInput = scanner.nextLine();
            }else{
                botInput = "No Command";
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(String.format("File %s not found", botRunner.getBotDirectory() + "/" + BOT_COMMAND));
        }
        try{
            writeRoundStateData(playerSpecificJsonState, playerSpecificTextState,playerSpecificConsoleState, botInput, gameMap.getCurrentRound());
        }catch (IOException e){
            e.printStackTrace();
        }

        RawCommand rawCommand = new RawCommand(botInput);
        publishCommand(rawCommand);
    }

    private void writeRoundStateData(String playerSpecificJsonState, String playerSpecificTextState, String playerSpecificConsoleState ,String command, int round) throws IOException {
        String mainDirectory = String.format("%s/%s", saveStateLocation, FileUtils.getRoundDirectory(round));
        File fMain = new File(mainDirectory);
        if (!fMain.exists()){
            fMain.mkdirs();
        }

        File f = new File(String.format("%s/%s", mainDirectory, getName()));
        if (!f.exists()){
            f.mkdirs();
        }

        File fConsole = new File(String.format("%s/%s/%s", mainDirectory, getName(),"Console") );
        if (!fConsole.exists()){
            fConsole.mkdirs();
        }

        FileUtils.writeToFile(String.format("%s/%s/%s",mainDirectory, getName(), "JsonMap.json"), playerSpecificJsonState);
        FileUtils.writeToFile(String.format("%s/%s/%s",mainDirectory, getName(), "TextMap.txt" ), playerSpecificTextState);
        FileUtils.writeToFile(String.format("%s/%s/%s",mainDirectory, getName(), "PlayerCommand.txt"), command);
        FileUtils.writeToFile(String.format("%s/%s/%s/%s",mainDirectory, getName(), "Console", "Console.txt"), playerSpecificConsoleState);
    }

    private void runBot(String state) throws IOException {
        File existingCommandFile = new File(String.format("%s/%s",  botRunner.getBotDirectory(), BOT_COMMAND));
        if (existingCommandFile.exists()){
            existingCommandFile.delete();
        }
        FileUtils.writeToFile(String.format("%s/%s", botRunner.getBotDirectory(),  BOT_STATE), state);

        try {
            botRunner.run();
        }catch (IOException e){
            System.out.println("Bot execution failed: " + e.getLocalizedMessage());
        }
        System.out.println("BotRunner Started.");
    }

    @Override
    public void gameEnded(GameMap gameMap) {

    }

    @Override
    public void playerKilled(GameMap gameMap) {
        System.out.println(String.format("Player %s has been killed", getName()));
    }

    @Override
    public void playerCommandFailed(GameMap gameMap, String reason) {
        System.out.println(String.format("Could not process player command: %s", reason));
    }

    @Override
    public void firstRoundFailed(GameMap gameMap, String reason) {
        System.out.println(reason);
        System.out.println("The first round has failed.");
        System.out.println("The round will now restart and both players will have to try again");
        System.out.println("Press any key to continue");

        scanner.nextLine();
    }
}
