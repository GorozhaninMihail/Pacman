/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server_;

import Enums.ClientCommand;
import Enums.MoveType;
import Enums.ServerCommand;
import MapModule.IChangeable;
import MapModule.Labyrinth;
import ServerModule.Player;
import ServerModule.Session;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 * @author August
 */
public class BaseUser {
    protected boolean _isPlayer;
    protected UUID _id;
    protected Socket _socket;
    protected ObjectOutputStream _outStream;
    protected ObjectInputStream _inStream;
    protected ArrayList<Session> _sessions;
    protected Session _currentSession;
    
    public UUID GetId(){
        return _id;
    }
    
    public boolean GetIsPlayer(){
        return _isPlayer;
    }
    
    public BaseUser(Socket socket){
        try {
            _socket = socket;
            _id = UUID.randomUUID();
            _inStream = new ObjectInputStream(_socket.getInputStream());
            _outStream = new ObjectOutputStream(_socket.getOutputStream());
            System.out.println("New user created.");
        } catch (IOException e) {
        }
    }
    
    public void SendLabyrinth(Labyrinth labyrinth){
        try {
            Pair parameter = new Pair(ClientCommand.Labyrinth, labyrinth);
            
            _outStream.writeObject(parameter);
            _outStream.flush();
            _outStream.reset();
        } catch (IOException ex) {
            Logger.getLogger(BaseUser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void SendActiveObjects(ArrayList<IChangeable> activeObjects){
        try {
            Pair parameter = new Pair(ClientCommand.ActiveObjects, activeObjects);
            
            _outStream.writeObject(parameter);
            _outStream.flush();
            _outStream.reset();
        } catch (IOException ex) {
            Logger.getLogger(BaseUser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void SendPlayers(ArrayList<Player> players){
        try {
            Pair parameter = new Pair(ClientCommand.Players ,players);
            
            _outStream.writeObject(parameter);
            _outStream.flush();
            _outStream.reset();
        } catch (IOException ex) {
            Logger.getLogger(BaseUser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void SetSessions(ArrayList<Session> sessions){
        _sessions = sessions; 
        Listen();
    }
    
    private class UpdateThread implements Runnable {
        private BaseUser _user;
        public UpdateThread(BaseUser user) {
            _user = user;
        }
        
        @Override
        public void run() {
             try {
                while(!_socket.isClosed()){
                    Pair com = (Pair)_inStream.readObject();
                    ServerCommand command = (ServerCommand)com.getKey();
                    switch (command){
                        case ConnectToSession:
                            ArrayList<Object> parametres = (ArrayList<Object>)com.getValue();
                            String sessionName = (String)parametres.get(0);
                            String playerName = (String)parametres.get(1);
                            boolean play = (boolean)parametres.get(2);
                            
                            for(Session session : _sessions){
                                if(session.GetName() == null ? sessionName == null : session.GetName().equals(sessionName)){
                                    _isPlayer = play;
                                    session.AddPlayer(_user);
                                    _currentSession = session;
                                    
                                    String res = "";
                                    res = "USER connected to session: "+session.GetName()+" as ";
                                    if(play)
                                        res += "player.";
                                    else
                                        res += "observer.";
                                    System.out.println(res);

                                    break;
                                }
                            }
                            Pair parameter = new Pair(ClientCommand.Id,_id);
                            _outStream.writeObject(parameter);
                        break;
                        case Move:
                            MoveType move = (MoveType)com.getValue();
                            _currentSession.GetPlayers().forEach(player -> {
                                if (player.GetPacman().GetId()==_id){
                                    player.GetPacman().SetNextDir(move);
                                }
                            });
                            break;
                        case Disconnect:
                            _outStream.close();
                            _inStream.close();
                            _socket.close();
                            _currentSession.RemovePlayer(_user);
                            break;
                    }
                }
                } catch (IOException ex) {
                        Logger.getLogger(BaseUser.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                        Logger.getLogger(BaseUser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    
    private void Listen(){
        new UpdateThread(this).run();
    }
}
