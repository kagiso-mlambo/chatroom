package server;

import java.util.ArrayList;

public class Channel {
    private String name;
    private String creator;
    private ArrayList<ClientHandler> members;


    public Channel(String name, String creator){
        this.name = name;
        this.creator = creator;
        members = new ArrayList<>();
    }


    public String name() {return name; }


    public String creator() {return creator; }


    public void addMembers(ClientHandler client){ members.add(client); }


    public void removeMembers(ClientHandler client){
        for (int i = 0; i < members.size(); i++){
            if (members.get(i).username().equals(client.username())) { members.remove(i); }
        }
    }


    public ArrayList<ClientHandler> members() { return members; }
}
