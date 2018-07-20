import com.trolltech.qt.core.QByteArray;
import com.trolltech.qt.core.QDataStream;
import com.trolltech.qt.core.QIODevice;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.*;
import com.trolltech.qt.network.QTcpSocket;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

public class Client extends  QWidget
{
    private QLineEdit nameLine;
    private QLineEdit addressLine;
    Signal2<String,String> enterSignal = new Signal2<>();

    public static void main(String args[])
    {
        QApplication.initialize(args);
        ChattingClient cClient = new ChattingClient();
        QApplication.execStatic();
    }

    Client()
    {
        QLabel addressLabel = new QLabel("IP-адресс сервера: ");
        QFont font = addressLabel.font();
        font.setPointSize(13);
        addressLabel.setFont(font);
        addressLine = new QLineEdit("localhost");
        addressLine.setFont(font);
        addressLine.returnPressed.connect(this, "enterSlot()");

        QLabel nameLabel = new QLabel("Имя пользователя: ");
        nameLabel.setFont(font);
        nameLine = new QLineEdit("human");
        nameLine.setFont(font);

        QPushButton enterButton = new QPushButton("Войти");
        enterButton.setFont(font);
        enterButton.clicked.connect(this, "enterSlot()");

        QVBoxLayout labelLayout = new QVBoxLayout();
        labelLayout.addWidget(addressLabel);
        labelLayout.addWidget(nameLabel);

        QVBoxLayout lineLayout = new QVBoxLayout();
        lineLayout.addWidget(addressLine);
        lineLayout.addWidget(nameLine);

        QHBoxLayout inputLayout = new QHBoxLayout();
        inputLayout.addLayout(labelLayout);
        inputLayout.addLayout(lineLayout);

        QVBoxLayout enterLayout = new QVBoxLayout();
        enterLayout.addLayout(inputLayout);
        enterLayout.addWidget(enterButton);

        setLayout(enterLayout);
        setWindowTitle("Клиент");
    }

    private void enterSlot()
    {
        if(nameLine.text().contains(" ") || nameLine.text().isEmpty())
        {
            QMessageBox msg = new QMessageBox();
            msg.setText("Имя не может быть пустым, или содержать пробелы");
            msg.exec();
            return;
        }
        enterSignal.emit(addressLine.text(), nameLine.text());
    }
}

class ChattingClient extends QWidget
{
    private QTcpSocket tcpSocket;
    private short nextBlockSize;
    private QLineEdit inputLine;
    private String username;
    private Client eClient;
    private QListWidget dialogsList;
    private QTextEdit dialogText;
    private QLabel nameLabel;
    private String recieverName;
    private Vector<Dialog> dialogs;

    ChattingClient()
    {
        eClient = new Client();
        eClient.show();
        eClient.enterSignal.connect(this,"enter(String,String)");
    }

    private void enter(String address, String user)
    {
        username = user;
        nextBlockSize = 0;

        tcpSocket = new QTcpSocket(this);
        tcpSocket.connectToHost(address, 2323);

        tcpSocket.error.connect(this,"errorSlot()");
        tcpSocket.connected.connect(this,"build()");
        tcpSocket.readyRead.connect(this, "readServer()");
    }

    private void errorSlot()
    {
        QMessageBox msg = new QMessageBox();
        msg.setText("Неверный IP-адресс");
        msg.exec();
        close();
    }

    public void build()
    {
        eClient.close();
        sendToServer(SAXHandler.getMessage("userJoin", username,"",""));
        dialogs = new Vector<>();
        recieverName = "";

        dialogText = new QTextEdit();
        dialogText.setReadOnly(true);
        QFont font = dialogText.font();
        font.setPointSize(13);
        dialogText.setFont(font);

        inputLine = new QLineEdit();
        inputLine.setFont(font);
        inputLine.returnPressed.connect(this,"sendButtonClicked()");

        QPushButton sendButton = new QPushButton("Отправить");
        sendButton.setFont(font);
        sendButton.clicked.connect(this, "sendButtonClicked()");

        nameLabel = new QLabel(username);
        nameLabel.setAlignment(Qt.AlignmentFlag.AlignCenter);
        nameLabel.setFrameStyle(1);
        font.setPointSize(15);
        nameLabel.setFont(font);
        font.setPointSize(13);

        QLabel dialogsLabel = new QLabel("Диалоги");
        dialogsLabel.setFont(font);

        dialogsList = new QListWidget();
        dialogsList.setFont(font);
        dialogsList.itemClicked.connect(this,"setDialog()");

        QHBoxLayout inputLayout = new QHBoxLayout();
        inputLayout.addWidget(inputLine);
        inputLayout.addWidget(sendButton);

        QVBoxLayout dialogLayout = new QVBoxLayout();
        dialogLayout.addWidget(dialogText);
        dialogLayout.addLayout(inputLayout);

        QVBoxLayout dialogsLayout = new QVBoxLayout();
        dialogsLayout.addWidget(nameLabel,2);
        dialogsLayout.addWidget(dialogsLabel,1);
        dialogsLayout.addWidget(dialogsList,7);

        QHBoxLayout base = new QHBoxLayout();
        base.addLayout(dialogLayout,5);
        base.addLayout(dialogsLayout,2);

        setLayout(base);
        setWindowTitle("Клиент");
        show();
    }

    private void sendToServer(String message)
    {
        QByteArray block = new QByteArray();
        QDataStream out = new QDataStream(block, QIODevice.OpenModeFlag.WriteOnly);
        out.setVersion(QDataStream.Version.Qt_4_0.value());

        out.writeShort((short)0);
        out.writeString(message);

        out.device().seek(0);
        out.writeShort((short)(block.size() - 2));

        tcpSocket.write(block);
    }

    private void readServer()
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

            String message = in.readString();
            defineMessage(message);
        }
    }

    private  void setDialog()
    {
        recieverName = dialogsList.currentItem().text();
        for(int i = 0; i < dialogs.size(); i++)
            if(dialogs.get(i).members(username, recieverName))
                dialogText.setText(dialogs.get(i).getConverstaion());
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
                    dialogsList.clear();
                    for(String user : saxHandler.getContent().split(" "))
                    {
                        if(!username.equals(user))
                        {
                            dialogsList.addItem(user);
                            boolean found = false;
                            for(int i = 0; i < dialogs.size(); i++)
                                if(dialogs.get(i).members(username, user))
                                    found = true;
                            if(!found)
                            {
                                Dialog dialog = new Dialog(username, user);
                                dialogs.add(dialog);
                            }
                        }
                    }
                    break;
                case "changeName":
                    username = saxHandler.getContent();
                    nameLabel.setText(username);
                    break;
                case "messageSend":
                    updateDialogs(saxHandler.getSender(),saxHandler.getContent());
                    break;
                default:
                    System.out.println("wrong type: " + saxHandler.getType());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void sendButtonClicked()
    {
        if(!recieverName.isEmpty() && !inputLine.text().isEmpty())
        {
            sendToServer(SAXHandler.getMessage("messageSend", username, recieverName, inputLine.text()));

            for(int i = 0; i < dialogs.size(); i++)
                if(dialogs.get(i).members(username,recieverName))
                {
                    dialogs.get(i).write(username, inputLine.text());
                    dialogText.setText(dialogs.get(i).getConverstaion());
                }
        }
        inputLine.clear();
    }

    private void updateDialogs(String sender, String message)
    {
        System.out.println(sender + ": " + message);
        for(int i = 0; i < dialogs.size(); i++)
            if(dialogs.get(i).members(username, sender))
                dialogs.get(i).write(sender, message);
        if(recieverName.equals(sender))
            setDialog();
    }
}

/*switch(command)
        {
            case ConnectedUser.userJoin:
                dialogsList.clear();
                for(String user : message.split(" "))
                {
                    if(!username.equals(user))
                        dialogsList.addItem(user);
                }
                break;
            case ConnectedUser.changeName:
                username = message;
                nameLabel.setText(message);
                break;
            case ConnectedUser.messageSend:
                updateDialogs(message);

            default:
                System.out.println("wrong command");
        }*/