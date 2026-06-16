package database;

import java.sql.Connection;

public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }
    public String logIn(String[] userCredentials){
        return "Logged in successfully";
    }

    public String signUp(String[] userCredentials){
        return "Logged in successfully";
    }
}
