package pw.sponges.strawpoll;

import lombok.Cleanup;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class StrawPollBot {
    
    private Map<String, Integer> proxies;
    private int votes = 0;
    
    public StrawPollBot() throws IOException {
        proxies = new HashMap<>();
        loadFile("proxies.txt");
    }
    
    private void bot(int pollId, int option) {
        for (String ip : proxies.keySet()) {
            try {
                vote(pollId, option, ip, proxies.get(ip));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Voted " + votes + " times!");
    }
    
    private void vote(int poll, int option, String ip, int port) throws IOException {
        
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        System.out.println("Proxy: " + ip + ":" + port);
        HttpURLConnection con = (HttpURLConnection) new URL("http://strawpoll.me/api/v2/votes").openConnection(proxy);
        
        con.setRequestMethod("POST");
        con.addRequestProperty("Content-Type", "application/json");
        con.addRequestProperty("User-Agent", "Mozilla/5.0");
        con.setDoOutput(true);
        con.setConnectTimeout(1000);
        con.setReadTimeout(1000);
    
    
        JSONObject json = new JSONObject().put("pollId", poll).put("votes", new int[] {option});
        con.addRequestProperty("Content-Length", Integer.toString(json.toString().length()));
    
        @Cleanup BufferedWriter out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
        
        
        
        out.write(json.toString());
        
        System.out.println(con.getResponseCode());
        
        if (con.getResponseCode() == 201) {
            votes++;
            System.out.println("Good proxy\n " + ip + ":" + port);
        }
        con.disconnect();
    }
    
    public static void main(String[] args) throws IOException {
        new StrawPollBot().bot(5939768, 2);
    }
    
    private void loadFile(String name) throws IOException {
        File file = new File(name);
        
        if (!file.exists()) {
            file.createNewFile();
            System.out.println("Please enter the proxies into the " + name + " file in the same directory as the jar!");
            System.exit(-1);
        }
        
        @Cleanup Scanner scanner = new Scanner(file);
        
        String line;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.contains(":")) {
                String ip = line.substring(0, line.indexOf(":"));
                int port = Integer.parseInt(line.substring(line.indexOf(":") + 1, line.length()));
                proxies.put(ip, port);
            }
        }
    }
}
