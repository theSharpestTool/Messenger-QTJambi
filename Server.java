import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.*;

import java.util.Vector;

public class Server extends QWidget
{
    private TcpServer tcpServer;
    private QTextEdit dialogLabel;
    private QListWidget dialogsList;
    private Vector<Dialog> dialogs;
    private String curDialog;

    public static void main(String args[])
    {
        QApplication.initialize(args);

        Server server = new Server();
        server.show();

        QApplication.execStatic();
    }

    private Server()
    {
        runTcpServer(2323);

        dialogLabel = new QTextEdit();
        QFont font = dialogLabel.font();
        font.setPointSize(11);
        dialogLabel.setFont(font);
        dialogLabel.setReadOnly(true);

        QLabel dialogsLabel = new QLabel("Выберите диалог");
        font.setPointSize(15);
        dialogsLabel.setFont(font);

        dialogsList = new QListWidget();
        font.setPointSize(11);
        dialogsList.setFont(font);

        QVBoxLayout dialogsLayout = new QVBoxLayout();
        dialogsLayout.addWidget(dialogsLabel);
        dialogsLayout.addWidget(dialogsList);

        QHBoxLayout base = new QHBoxLayout();
        base.addWidget(dialogLabel,3);
        base.addLayout(dialogsLayout, 1);

        setLayout(base);
        setWindowTitle("Сервер");
    }

    private void runTcpServer(int port)
    {
        tcpServer = new TcpServer();
        tcpServer.messageSignal.connect(this,"updateDialogs(Vector)");
        tcpServer.runServer(port);
    }

    private void updateDialogs(Vector<Dialog> dialogss)
    {
        dialogs = dialogss;
        dialogsList.clear();
        for(int i = 0; i < dialogs.size(); i++)
        {
            dialogsList.addItem(dialogs.get(i).getMembers());
            dialogsList.itemClicked.connect(this, "setCurDialog()");
        }
        if(dialogsList.isEnabled())
            dialogSelected();
    }

    private void setCurDialog()
    {
        curDialog = dialogsList.currentItem().text();
        dialogSelected();
    }

    private void dialogSelected()
    {
        for(int i = 0; i < dialogs.size(); i++)
            if(dialogs.get(i).getMembers().equals(curDialog))
                dialogLabel.setText(dialogs.get(i).getConverstaion());
    }

}