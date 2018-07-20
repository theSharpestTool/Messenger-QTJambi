import com.trolltech.qt.core.QByteArray;
import com.trolltech.qt.core.QDataStream;
import com.trolltech.qt.core.QIODevice;
import com.trolltech.qt.network.QHostAddress;
import com.trolltech.qt.network.QTcpServer;

import java.util.*;

public class TcpServer extends QTcpServer {

    private Vector<ConnectedUser> users;
    private Vector<Dialog> dialogs;

    TcpServer()
    {
        users = new Vector<>();
        dialogs = new Vector<>();
    }

    @Override
    protected void incomingConnection(int handle)
    {
        super.incomingConnection(handle);
        ConnectedUser user = new ConnectedUser(handle, this);
        System.out.println("connected");
        users.add(user);
    }

    void runServer(int port)
    {
        QHostAddress address = new QHostAddress(QHostAddress.SpecialAddress.LocalHost);
        boolean serverWorks = listen(address, port);
        if(!serverWorks)
            System.out.println("Server Error");
    }

    private QByteArray createBlock(String message)
    {
        QByteArray block = new QByteArray();
        QDataStream out = new QDataStream(block, QIODevice.OpenModeFlag.WriteOnly);
        out.setVersion(QDataStream.Version.Qt_4_0.value());

        out.writeShort((short)0);
        out.writeString(message);

        out.device().seek(0);
        out.writeShort((short)(block.size() - 2));

        return block;
    }

    void sendUsersList()
    {
        String usersList = "";

        for(int i = 0; i < users.size(); i++)
            usersList += users.get(i).getName() + " ";

        if(!usersList.equals(""))
        {
            usersList = usersList.substring(0, usersList.length() - 1);
            QByteArray block = createBlock(SAXHandler.getMessage("userJoin", "", "", usersList));
            for (int i = 0; i < users.size(); i++)
                users.get(i).socket.write(block);
        }

        System.out.println("usersList = " + usersList);
    }


    void checkForRepeat()
    {
        boolean repeat;
        String curName =  users.lastElement().getName();
        System.out.println("curName = " + curName);
        repeat = false;
        for(int i = 0; i < users.size() - 1; i++)
            if(users.get(i).getName().equals(curName))
                repeat = true;
        if(repeat)
        {
            System.out.println("2");
            users.lastElement().setName(users.lastElement().getName() + "2");
            checkForRepeat();
            QByteArray block = createBlock(SAXHandler.getMessage("changeName","","", users.get(users.size() - 1).getName()));
            users.lastElement().socket.write(block);
        }
    }

    void removeSocket(String name)
    {
        removeDialogs(name);
        messageSignal.emit(dialogs);

        for(int i = 0; i < users.size(); i++)
            if(users.get(i).getName().equals(name))
                users.remove(i);
        sendUsersList();
    }

    private void removeDialogs(String name)
    {
        for(int i = 0; i < dialogs.size(); i++)
            if(dialogs.get(i).member(name))
            {
                dialogs.remove(i);
                removeDialogs(name);
            }
    }

    Signal1<Vector<Dialog>> messageSignal = new Signal1<>();

    void transferMessage(String sender, String reciever, String message)
    {
        System.out.println(sender  + "->" + reciever + "=" + message);
        boolean found = false;
        for(int i = 0; i < dialogs.size(); i++)
            if(dialogs.get(i).members(sender,reciever))
            {
                dialogs.get(i).write(sender, message);
                found = true;
            }
        if(!found)
        {
            Dialog newDialog = new Dialog(sender, reciever);
            dialogs.add(newDialog);
            dialogs.lastElement().write(sender, message);
        }
        messageSignal.emit(dialogs);

        for(int i = 0; i < users.size(); i++)
            if(users.get(i).getName().equals(reciever))
                users.get(i).socket.write(createBlock(SAXHandler.getMessage("messageSend", sender, reciever, message)));
    }
}
