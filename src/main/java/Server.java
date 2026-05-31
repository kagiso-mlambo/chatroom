import java.net.*;
import java.io.*;

public class Server {
    static void main(String[] args) throws IOException{
        ServerSocket severSocket = new ServerSocket(8081);
        Socket clientSocket = severSocket.accept();
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String request;

        while ((request = bufferedReader.readLine()) != null){
            System.out.println("Server received: " + request);
            printWriter.println("Server received: " + request);
        }


    }
}
