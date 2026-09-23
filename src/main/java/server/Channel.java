package server;

import java.util.ArrayList;


/**
 * Represents an in-memory view of a channel: its name, who created it, and
 * the currently connected clients that belong to it.
 *
 * This tracks live {@link ClientHandler} references for clients currently
 * online, which is separate from the persisted membership records kept in
 * ChannelMembersRepository.
 */
public class Channel {
    private String name;
    private String creator;
    private ArrayList<ClientHandler> members;


    /**
     * Creates a channel with no members yet.
     *
     * @param name the channel's name
     * @param creator the username of the user who created the channel
     */
    public Channel(String name, String creator){
        this.name = name;
        this.creator = creator;
        members = new ArrayList<>();
    }


    /**
     * @return the channel's name
     */
    public String name() {return name; }


    /**
     * @return the username of the user who created the channel
     */
    public String creator() {return creator; }


    /**
     * Adds a connected client to this channel's member list.
     *
     * @param client the client to add
     */
    public void addMembers(ClientHandler client){ members.add(client); }


    /**
     * Removes a client from this channel's member list, matched by username.
     *
     * @param client the client to remove
     */
    public void removeMembers(ClientHandler client){
        for (int i = 0; i < members.size(); i++){
            if (members.get(i).username().equals(client.username())) { members.remove(i); }
        }
    }


    /**
     * @return the clients currently in this channel
     */
    public ArrayList<ClientHandler> members() { return members; }
}
