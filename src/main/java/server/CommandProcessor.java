package server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import static common.ANSICodes.*;


/**
 * Parses and delegates every "/command" a client sends.
 *
 * Anything that doesn't match a known command (like /users or /create) is
 * treated as a private message request, with the leading "/" stripped and
 * the remaining text passed through as the recipient's username.
 */
public class CommandProcessor {
    private Server server;
    private ClientHandler client;
    private static final Logger LOGGER = Logger.getLogger(CommandProcessor.class.getName());


    /**
     * Creates a command processor for one client's session.
     *
     * @param server the server to delegate command actions to
     * @param client the client this processor is handling commands for
     */
    public CommandProcessor(Server server, ClientHandler client){
        this.server = server;
        this.client = client;
    }


    /**
     * Parses a single line of input and carries out the matching command.
     *
     * Supported commands are /quit, /users, /create, /join, /delete, and
     * /groups. Anything else is treated as /&lt;username&gt; and routed as a
     * private message request to that user.
     *
     * @param command the raw input line, starting with "/"
     */
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
           LOGGER.warning("CommandProcessor - Method handleCommand: " + e.getMessage());
        }
    }
}
