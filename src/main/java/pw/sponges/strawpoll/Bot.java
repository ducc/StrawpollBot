package pw.sponges.strawpoll;

import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Bot {

    private Map<String, Integer> proxies;
    private int votes = 0;
    private int tries = 0;

    public Bot() {
        this.proxies = new HashMap<>();
        this.loadFile("proxies.txt");
    }

    private void bot(int poll, int option) {
        for (String ip : proxies.keySet()) {
            vote(poll, option, ip, proxies.get(ip));
        }

        System.out.println("Voted " + votes + " times!");
    }

    private void vote(int poll, int option, String ip, int port) {
        tries++;
        System.out.println("Try #" + tries);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL("http://strawpoll.me/api/v2/votes").openConnection(proxy);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (con == null) return;

        try {
            con.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        con.addRequestProperty("Content-Type", "application/json");
        con.addRequestProperty("User-Agent", "Mozilla/5.0");
        con.setDoOutput(true);
        con.setConnectTimeout(500);
        con.setReadTimeout(500);

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
        } catch (IOException ignored) {}

        JSONObject json = new JSONObject();
        json.put("pollId", poll);
        json.put("votes", new Integer[] { option });

        try {
            if (out != null) {
                out.write(json.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (con.getResponseCode() == 201) {
                votes++;
                System.out.println("Good proxy\n " + ip + ":" + port);
            }
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        try {
            new Bot().bot(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Usage: java -jar StrawpollBot.jar <poll id> <option>");
        }
    }

    private void loadFile(String name) {
        File file = new File(name);

        if (!file.exists()) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file));
                writer.write("");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Please enter the proxies into the " + name + " file in the same directory as the jar!");
            System.exit(-1);
            return;
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String input;

        try {
            while ((input = in.readLine()) != null) {
                try {
                    String[] split = input.split(":");
                    String ip = split[0];

                    int port;
                    try {
                        port = Integer.parseInt(split[1].split(" ")[0]);
                    } catch (Exception e) {
                        continue;
                    }

                    proxies.put(ip, port);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
