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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class StrawPollBot {
    
    private Map<String, Integer> proxies;
    private AtomicInteger votes;
    private AtomicInteger completed;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        new StrawPollBot().bot(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }
    
    public StrawPollBot() throws IOException {
        proxies = new HashMap<>();
        loadFile("proxies.txt");
        votes = new AtomicInteger(0);
        completed = new AtomicInteger(0);
    }
    
    private void bot(int pollId, int option) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2000);
        for (String ip : proxies.keySet()) {
            executorService.submit(() -> {
                vote(pollId, option, ip, proxies.get(ip));
            });
        }
        executorService.shutdown();
        final int proxyCount = proxies.size();
        while (!executorService.isTerminated()) {
            System.out.println(((int) ((completed.get() / (double) proxyCount) * 100)) + "% Votes: " + votes.get() + " Completed: " +
                    completed.get() + " Success Rate: " + ((int) ((votes.get() / (double) completed.get()) * 100)) + "%");
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {
            }
        }
        System.out.println("Done");
    }
    
    private void vote(int poll, int option, String ip, int port) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        
        for (int i = 0; i < 3; i++) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL("http://strawpoll.me/api/v2/votes").openConnection(proxy);
                
                con.setRequestMethod("POST");
                con.addRequestProperty("Content-Type", "application/json");
                con.addRequestProperty("User-Agent", "Mozilla/5.0");
                con.setDoOutput(true);
                con.setConnectTimeout(20000);
                con.setReadTimeout(20000);
                
                
                JSONObject json = new JSONObject().put("pollId", poll).put("votes", new Integer[] {option});
                con.addRequestProperty("Content-Length", Integer.toString(json.toString().length()));
                @Cleanup BufferedWriter out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
                out.write(json.toString());
                out.close();
                
                int responseCode = con.getResponseCode();
                
                if (responseCode == 201) {
                    votes.incrementAndGet();
                    break;
                }
                con.disconnect();
            } catch (IOException ignored) {
            }
        }
        completed.incrementAndGet();
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
                int port;
                try {
                    port = Integer.parseInt(line.substring(line.indexOf(":") + 1, line.length()));
                } catch (NumberFormatException e) {
                    continue;
                }
                proxies.put(ip, port);
            }
        }
    }
}
