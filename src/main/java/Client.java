import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    public static void main() throws IOException{
        Socket clientSocket = new Socket("localhost",8081);
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        while (true){
            Scanner sc = new Scanner(System.in);
            String request = sc.nextLine();
            printWriter.println(request);
        }
    }


}
