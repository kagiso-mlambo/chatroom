package server;

import java.io.IOException;

import static common.ANSICodes.*;

public class CommandProcessor {
    private Server server;
    private ClientHandler client;

    public CommandProcessor(Server server, ClientHandler client){
        this.server = server;
        this.client = client;
    }

    public void handleCommand(String command) throws IOException {
        String[] args = command.split(" ");
        switch (args[0]){
            case "/quit": { client.closeClient(); break; }

            case "/users":{
                client.sendMessage(UNDERLINE.code() + BOLD.code() + "Online Users:" + RESET.code());
                for (ClientHandler c: server.getAllClients()){
                    if ( !client.username().equals(c.username()) ) client.sendMessage(c.username());
                }
                client.sendMessage("\n");
                break;
            }

            case "/create":{
                server.addChannel(args[1], client.username());
                client.sendMessage(BOLD.code() + "You've created the channel \"" + args[1] + "\" use /join to enter" + RESET.code());
                break;
            }

            case "/join":{
                server.joinNewChannel(client.username(), args[1]);
                break;
            }

            case "/delete":{
                server.deleteChannel(args[1], client.username());
                break;
            }

            case "/rooms": {
                client.sendMessage(UNDERLINE.code() + BOLD.code() + "Available Rooms:" + RESET.code());
                for (Channel channel: server.getAllChannels()){ client.sendMessage(channel.name()); }
                client.sendMessage("\n");
                break;
            }

            default:
            {
                command = command.replace("/", "");
                server.privateMessage(command, client.username());
            }

        }
    }
}
