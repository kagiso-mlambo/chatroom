import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private static final Scanner sc = new Scanner(System.in);

    public static void main() throws IOException{
        Socket clientSocket = new Socket("localhost",8081);
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String request;

        System.out.println("Please enter your username: ");
        request = sc.nextLine();
        printWriter.println(request);

        while (true){
            request = sc.nextLine();
            printWriter.println(request);
        }
    }


}
