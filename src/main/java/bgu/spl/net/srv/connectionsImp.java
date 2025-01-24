package bgu.spl.net.srv;

import java.nio.channels.Channel;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class connectionsImp<T> implements Connections<T> {



    ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionHandlers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LinkedList<Integer>> channels = new ConcurrentHashMap();//from name to subscriptionsID
    ConcurrentHashMap<String, Client<T>> Clients = new ConcurrentHashMap<>();
    boolean isConnected = false;

    public connectionsImp() {
        //do nothing
    }


    @Override
    public boolean subscribe(int connectionId, int subscribeID, String channel) {
        ConcurrentHashMap<Integer, String> userChannels = connectionHandlers.get(connectionId).getClient().getChannels();
        userChannels.putIfAbsent(subscribeID, channel);

        if(channels.containsKey(channel)){
            channels.get(channel).add(subscribeID);
        }
        else{
            LinkedList<Integer> newChannel = new LinkedList<>();
            newChannel.add(subscribeID);
            channels.put(channel, newChannel);
        }

        return true;
    }

    @Override
    public boolean unsubscribe(int connectionId, int subscribeID, String channel) {
        ConcurrentHashMap<Integer, String> userChannels = connectionHandlers.get(connectionId).getClient().getChannels();
        userChannels.remove(subscribeID);
        return true;
    }

    @Override
    public boolean isSubscribe(int connectionId, String channel) {
        if(isChannel(channel)){
            return channels.get(channel).contains(connectionId);
        }
        return false;
    }

    @Override
    public boolean isChannel(String channel) {
        return channels.containsKey(channel);
    }

    @Override
    public boolean isUserInfoValid(String username, String password) {
        return !Clients.containsKey(username)||
                Clients.get(username).getPassword().equals(password));
    }

    @Override
    public boolean isUserLoggedIn(String username) {
        return !Clients.contains(username) ||
                Clients.get(username).isConnected();
    }



    @Override
    public boolean send(int connectionId, T msg) {
        connectionHandlers.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void login(int connectionId, String username, String password) {
        ConnectionHandler<T> handler = this.connectionHandlers.get(connectionId);
        if(Clients.contains(username)){
            Client client = Clients.get(username);
            client.connect();
            client.setConnectionHandler(handler);
            client.setConnectionId(connectionId);

        }
        else{
            Client<T> newClient = new Client<>(username, password, handler, connectionId);
            Clients.put(username, newClient);
        }

    }

    @Override
    public void send(String channel, T msg) {
        LinkedList<Integer> subscribers = channels.get(channel);
        for (int i = 0; i < subscribers.size(); i++) {
            connectionHandlers.get(subscribers.get(i)).send(msg);
        }
    }


    @Override
    public void disconnect(int connectionId) {
        Client client = connectionHandlers.get(connectionId).getClient();
        if(client != null){

            for(Object channel : client.getChannels().values()){
                channels.get((String) channel).remove(connectionId);
            }

            client.disconnect();
            client.setConnectionHandler(null);
            client.setConnectionId(-1);
        }
    }
}