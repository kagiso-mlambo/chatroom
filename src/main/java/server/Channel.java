package server;

import java.util.ArrayList;

public class Channel {
    private String name;
    private ArrayList<ClientHandler> members;


    public Channel(String name){
        this.name = name;
        members = new ArrayList<>();
    }


    public String name() {return name; }


    public void addMembers(ClientHandler client){ members.add(client); }


    public void removeMembers(ClientHandler client){
        for (int i = 0; i < members.size(); i++){
            if (members.get(i).username().equals(client.username())) { members.remove(i); }
        }
    }


    public ArrayList<ClientHandler> members() { return members; }
}
