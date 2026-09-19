package server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static common.ANSICodes.*;

public class CommandProcessor {
    private Server server;
    private ClientHandler client;

    public CommandProcessor(Server server, ClientHandler client){
        this.server = server;
        this.client = client;
    }

    public void handleCommand(String command) {
        try {
            String[] args = command.split(" ");
            switch (args[0]) {
                case "/quit": {
                    client.closeClient();
                    break;
                }

                case "/users": {
                    client.sendMessage(UNDERLINE.code() + BOLD.code() + "Friends:" + RESET.code());
                    ArrayList<String> clients = server.getAllClients();
                    for (String user : clients) {
                        if (!client.username().equals(user)) client.sendMessage(user);
                    }
                    client.sendMessage("\n");
                    break;
                }

                case "/create": {
                    server.addGroupChannel(args[1], client.username());
                    client.sendMessage(BOLD.code() + "You've created the channel \"" + args[1] +
                            "\" use /join [channel name] to enter" + RESET.code());
                    break;
                }

                case "/join": {
                    server.joinNewChannel(client.username(), args[1]);
                    break;
                }

                case "/delete": {
                    server.deleteChannel(args[1], client.username());
                    break;
                }

                case "/groups": {
                    client.sendMessage(UNDERLINE.code() + BOLD.code() + "Groups:" + RESET.code());
                    ArrayList<String> channels = server.getAllGroupChannels(client.username());
                    for (String channel : channels) {
                        client.sendMessage(channel);
                    }
                    client.sendMessage("\n");
                    break;
                }

                default: {
                    command = command.replace("/", "");
                    server.privateMessage(command, client.username());
                }

            }
        } catch (SQLException e) {
            System.out.println("CommandProcessor - Method handleCommand: ");
            System.out.println(e);
        }
    }
}
