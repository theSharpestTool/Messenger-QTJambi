import com.trolltech.qt.core.QDataStream;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.network.QTcpSocket;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

class ConnectedUser extends QObject {

    QTcpSocket socket;
    private TcpServer server;
    private short nextBlockSize;
    private String name;

    ConnectedUser(int descriptor, TcpServer server)
    {
        this.server = server;
        System.out.println("Connected user");
        nextBlockSize = 0;
        socket = new QTcpSocket(this);
        socket.setSocketDescriptor(descriptor);

        socket.connected.connect(this, "connectedSlot()");
        socket.error.connect(this,"errorSlot()");
        socket.disconnected.connect(this,"disconnectedSlot()");
        socket.readyRead.connect(this,"readSlot()");
    }

    private void connectedSlot()
    {
        System.out.println("connected");
    }
    private void errorSlot()
    {
        System.out.println("error");
    }
    private void disconnectedSlot()
    {
        server.removeSocket(name);
    }

    private void readSlot()
    {
        QTcpSocket socket = (QTcpSocket) signalSender();
        QDataStream in = new QDataStream(socket);
        in.setVersion(QDataStream.Version.Qt_4_0.value());
        for(;;)
        {
            if(nextBlockSize == 0)
            {
                if(socket.bytesAvailable() < 2)
                    break;
                nextBlockSize = in.readShort();
            }
            if(socket.bytesAvailable() < nextBlockSize)
                break;
            nextBlockSize = 0;

            String string = in.readString();
            defineMessage(string);
        }
    }

    private void defineMessage(String message)
    {
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            SAXHandler saxHandler = new SAXHandler();
            InputStream is = new ByteArrayInputStream(message.getBytes());

            parser.parse(is, saxHandler);
            switch(saxHandler.getType())
            {
                case "userJoin":
                    name = saxHandler.getSender();
                    server.checkForRepeat();
                    server.sendUsersList();
                    break;
                case "messageSend":
                    server.transferMessage(saxHandler.getSender(), saxHandler.getReceiver(), saxHandler.getContent());
                    break;
                default:
                    System.out.println("wrong type");
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }
}

/*switch(command)
        {
            case userJoin:
                name = string;
                server.checkForRepeat();
                server.sendUsersList();
                break;
            case messageSend:
                server.transferMessage(string);
                break;
            default:
                System.out.println("wrong command");
                break;
        }*/
/*public final static byte userJoin = 1;
    public final static byte messageSend = 2;
    public final static byte changeName = 3;*/