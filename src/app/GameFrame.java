/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 *
 * @author gaish
 */
public class GameFrame extends javax.swing.JFrame {

    /**
     * Creates new form GameFrame
     */
    Locale locale;
    Locale russianLocale = new Locale("ru");
    int drawX = 0, drawY = 0; //drew point

    String[][] points;
    ArrayList<ArrayList<Point>> contours = new ArrayList<ArrayList<Point>>();
    ArrayList<ArrayList<Point>> unclosedContoursM = new ArrayList<ArrayList<Point>>();
    ArrayList<ArrayList<Point>> unclosedContoursO = new ArrayList<ArrayList<Point>>();
    boolean game = false, red = true;
    public String ip = "";
    public int port = 0;
    String password = "";
    Selector selector;
    ServerSocketChannel serverSocket;
    ByteBuffer buffer = ByteBuffer.allocate(256);
    SocketChannel socket = null;
    ClientTask t;
    boolean client = false, yourStep = false;
    public boolean choose;
    int myScore = 0, opponentScore = 0;
    int minutes = 3, seconds = 0;
    boolean endGame = false;
    javax.swing.Timer timer;

    public GameFrame() {
        initComponents();
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        locale = Locale.getDefault();
    }

    byte[] ObjectToBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.flush();
            yourBytes = bos.toByteArray();

        } catch (IOException ex) {
            Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return yourBytes;
    }

    Object BytesToObject(byte[] arrayBytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(arrayBytes);
        ObjectInput in = null;
        Object resp = null;
        try {
            in = new ObjectInputStream(bis);
            resp = in.readObject();

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return resp;
    }

    class ServerTask extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            while (true) {
                while (true) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectedKeys.iterator();
                    while (iter.hasNext()) {

                        SelectionKey key = iter.next();

                        if (key.isAcceptable()) {
                            socket = serverSocket.accept();
                            if (!game) {
                                game = true;
                                socket.configureBlocking(false);
                                socket.register(selector, SelectionKey.OP_READ);
                                yourStep = false;
                                drawPoint(18, 15);
                                drawPoint(19, 14);
                                yourStep = true;
                                drawPoint(19, 15);
                                drawPoint(18, 14);
                                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                                    jLabel1.setText("Ваш ход");
                                } else {
                                    jLabel1.setText("Your move");
                                }
                                jButton4.setEnabled(true);
                                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                                    jButton4.setText("Стоп");
                                } else {
                                    jButton4.setText("Stop");
                                }

                                jLabel6.setVisible(false);
                                jLabel7.setVisible(false);
                                minutes = 3;
                                seconds = 0;
                                endGame = false;
                                myScore = 0;
                                opponentScore = 0;
                                jLabel4.setText("0");
                                jLabel5.setText("0");
                                jButton2.setEnabled(false);
                            } else {
                                ArrayList<String> resp = new ArrayList<String>();
                                resp.add("9");
                                ByteBuffer msgBuf = ByteBuffer.wrap(ObjectToBytes(resp));
                                SocketChannel sch = (SocketChannel) key.channel();
                                sch.write(msgBuf);
                                msgBuf.rewind();
                                key.cancel();
                            }
                        }
                        if (key.isReadable()) {
                            handleRead(key);
                        }
                    }
                    iter.remove();
                }
            }
        }

        private void handleRead(SelectionKey key) throws IOException, SQLException {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            byte[] a = new byte[0];
            int read = 0;
            while ((read = channel.read(buffer)) > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                byte[] newArray = new byte[a.length + bytes.length];
                System.arraycopy(a, 0, newArray, 0, a.length);
                System.arraycopy(bytes, 0, newArray, a.length, bytes.length);
                a = newArray;
                buffer.clear();
            }
            if (a.length == 0) {
                key.cancel();
                return;
            }
            ArrayList<String> msg = (ArrayList<String>) BytesToObject(a);
            ArrayList<String> resp = new ArrayList<String>();
            if (msg.get(0).trim().equals("0")) {
                if (msg.get(1).equals(password)) {
                    resp.add("7");

                } else {
                    resp.add("8");
                    ByteBuffer msgBuf = ByteBuffer.wrap(ObjectToBytes(resp));
                    SocketChannel sch = (SocketChannel) key.channel();
                    sch.write(msgBuf);
                    msgBuf.rewind();
                    key.cancel();
                    game = false;
                }
                ByteBuffer msgBuf = ByteBuffer.wrap(ObjectToBytes(resp));
                SocketChannel sch = (SocketChannel) key.channel();
                sch.write(msgBuf);
                msgBuf.rewind();
            }
            if (msg.get(0).trim().equals("1")) {
                Point p = new Point(Integer.valueOf(msg.get(1)), Integer.valueOf(msg.get(2)));
                points[p.x][p.y] = "20";
                drawPoint(p.x, p.y);

                int c = countNewContours(p.x, p.y);
                if (c >= 1) {
                    for (int i = 1; i <= c; i++) {
                        drawPolygon(contours.get(contours.size() - i));
                    }
                };
                if (!endGame) {
                    yourStep = true;
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jLabel1.setText("Ваш ход");
                    } else {
                        jLabel1.setText("Your move");
                    }

                }

                checkUnclosedContour();
            }
            if (msg.get(0).trim().equals("2")) {
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    jLabel1.setText("Противник нажал кнопку Стоп");
                } else {
                    jLabel1.setText("Opponent pressed \"Stop\" button");
                }

                jLabel7.setText("03:00");
                jLabel6.setVisible(true);
                jLabel7.setVisible(true);
                minutes = 3;
                seconds = 0;
                endGame = true;
                yourStep = true;
                timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        if (seconds == 0) {
                            seconds = 59;
                            minutes--;
                            if (minutes < 0) {
                                EndGame(key);
                            }
                        } else {
                            seconds--;
                        }
                        jLabel7.setText("0" + String.valueOf(minutes) + ":" + String.format("%02d", seconds));

                    }
                });
                timer.start();
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    jButton4.setText("Завершить игру");
                } else {
                    jButton4.setText("Complete the game");
                }
            }

            if (msg.get(0).trim().equals("3")) {
                yourStep = false;
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    jLabel1.setText("Конец игры!");
                } else {
                    jLabel1.setText("Game over!");
                }
                if (myScore > opponentScore) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Игра закончена! Вы выиграли, окружив " + String.valueOf(myScore) + " точек противника. Противник окружил лишь " + String.valueOf(opponentScore) + "точек.");

                    } else {
                        JOptionPane.showMessageDialog(null, "Game over! You won having surrounded " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded only " + String.valueOf(opponentScore) + "dots.");
                    }
                }
                if (myScore < opponentScore) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Игра закончена! Вы проиграли, окружив лишь " + String.valueOf(myScore) + " точек противника. Противник окружил " + String.valueOf(opponentScore) + "точек.");
                    } else {
                        JOptionPane.showMessageDialog(null, "Game over! You lose having surrounded only " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded " + String.valueOf(opponentScore) + "dots.");
                    }
                }
                if (myScore == opponentScore) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Игра закончена! Ничья. Вы окружили по " + String.valueOf(myScore) + " точек.");
                    } else {
                        JOptionPane.showMessageDialog(null, "Game over! Tie. You each surrounded " + String.valueOf(myScore) + " dots.");
                    }
                }
            }

        }

    }

    void EndGame(SelectionKey key) {
        timer.stop();
        ArrayList<String> resp = new ArrayList<String>();
        resp.add("3");
        ByteBuffer msgBuf = ByteBuffer.wrap(ObjectToBytes(resp));
        SocketChannel sch = (SocketChannel) key.channel();
        try {
            sch.write(msgBuf);
        } catch (IOException ex) {
            Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        msgBuf.rewind();
        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
            jButton4.setText("Стоп");
        } else {
            jButton4.setText("Stop");
        }
        jButton1.setEnabled(true);
        jButton2.setEnabled(true);
    }

    void EndGame(SocketChannel key) {
        timer.stop();
        ArrayList<String> resp = new ArrayList<String>();
        resp.add("3");
        ByteBuffer msgBuf = ByteBuffer.wrap(ObjectToBytes(resp));
        SocketChannel sch = (SocketChannel) key;
        try {
            sch.write(msgBuf);
        } catch (IOException ex) {
            Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        msgBuf.rewind();
        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
            jButton4.setText("Стоп");
        } else {
            jButton4.setText("Stop");
        }
        jButton1.setEnabled(true);
        jButton2.setEnabled(true);
    }

    class ClientTask extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            while (true) {
                ByteBuffer buf = ByteBuffer.allocate(2048);
                socket.configureBlocking(false);
                byte[] a = new byte[0];
                int read = 0;
                try {
                    while ((read = socket.read(buf)) > 0) {
                        socket.configureBlocking(false);
                        buf.flip();
                        byte[] bytes = new byte[buf.limit()];
                        buf.get(bytes);
                        byte[] newArray = new byte[a.length + bytes.length];
                        System.arraycopy(a, 0, newArray, 0, a.length);
                        System.arraycopy(bytes, 0, newArray, a.length, bytes.length);
                        a = newArray;
                        buf.clear();
                    }
                } catch (Exception e) {
                    Logger.getLogger(GameFrame.class.getName()).log(Level.SEVERE, null, e);
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Сервер был отключен! Игра окончена", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Server is disabled! Game is ended", "Error!", JOptionPane.ERROR_MESSAGE);
                    }
                    //EndGame();
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jLabel1.setText("Сервер отключен!");
                    } else {
                        jLabel1.setText("Server is disabled!");
                    }
                    return null;
                }
                if (a.length == 0) {
                    continue;
                }
                ArrayList<String> resp = (ArrayList<String>) BytesToObject(a);
                if (resp.get(0).equals("1")) {
                    Point p = new Point(Integer.valueOf(resp.get(1)), Integer.valueOf(resp.get(2)));
                    points[p.x][p.y] = "20";
                    drawPoint(p.x, p.y);

                    int c = countNewContours(p.x, p.y);
                    if (c >= 1) {
                        for (int i = 1; i <= c; i++) {
                            drawPolygon(contours.get(contours.size() - i));
                        }
                    };
                    if (!endGame) {
                        yourStep = true;
                        jLabel1.setText("Ваш ход");
                    }

                    checkUnclosedContour();
                }
                if (resp.get(0).trim().equals("2")) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jLabel1.setText("Противник нажал кнопку Стоп");
                    } else {
                        jLabel1.setText("Opponent pressed \"Stop\" button");
                    }
                    jLabel7.setText("03:00");
                    jLabel6.setVisible(true);
                    jLabel7.setVisible(true);
                    minutes = 3;
                    seconds = 0;
                    endGame = true;
                    yourStep = true;
                    timer = new Timer(1000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            if (seconds == 0) {
                                seconds = 59;
                                minutes--;
                                if (minutes < 0) {
                                    EndGame(socket);
                                }
                            } else {
                                seconds--;
                            }
                            jLabel7.setText("0" + String.valueOf(minutes) + ":" + String.format("%02d", seconds));

                        }
                    });
                    timer.start();
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jButton4.setText("Завершить игру");
                    } else {
                        jButton4.setText("Complete the game");
                    }
                }
                if (resp.get(0).trim().equals("3")) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jLabel1.setText("Конец игры!");
                    } else {
                        jLabel1.setText("Game over!");
                    }
                    if (myScore > opponentScore) {
                        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                            JOptionPane.showMessageDialog(null, "Игра закончена! Вы выиграли, окружив " + String.valueOf(myScore) + " точек противника. Противник окружил лишь " + String.valueOf(opponentScore) + "точек.");

                        } else {
                            JOptionPane.showMessageDialog(null, "Game over! You won having surrounded " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded only " + String.valueOf(opponentScore) + "dots.");
                        }
                    }
                    if (myScore < opponentScore) {
                        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                            JOptionPane.showMessageDialog(null, "Игра закончена! Вы проиграли, окружив лишь " + String.valueOf(myScore) + " точек противника. Противник окружил " + String.valueOf(opponentScore) + "точек.");
                        } else {
                            JOptionPane.showMessageDialog(null, "Game over! You lose having surrounded only " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded " + String.valueOf(opponentScore) + "dots.");
                        }
                    }
                    if (myScore == opponentScore) {
                        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                            JOptionPane.showMessageDialog(null, "Игра закончена! Ничья. Вы окружили по " + String.valueOf(myScore) + " точек.");
                        } else {
                            JOptionPane.showMessageDialog(null, "Game over! Tie. You each surrounded " + String.valueOf(myScore) + " dots.");
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("app/Bundle"); // NOI18N
        setTitle(bundle.getString("GameFrame.title")); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel1.setMaximumSize(new java.awt.Dimension(780, 640));
        jPanel1.setMinimumSize(new java.awt.Dimension(780, 640));
        jPanel1.setPreferredSize(new java.awt.Dimension(780, 640));
        jPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel1MouseMoved(evt);
            }
        });
        jPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel1MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 778, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 638, Short.MAX_VALUE)
        );

        jButton1.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jButton1.setText(bundle.getString("GameFrame.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Consolas", 1, 12)); // NOI18N
        jLabel1.setText(bundle.getString("GameFrame.jLabel1.text")); // NOI18N

        jButton2.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jButton2.setText(bundle.getString("GameFrame.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jButton3.setText(bundle.getString("GameFrame.jButton3.text")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jButton4.setText(bundle.getString("GameFrame.jButton4.text")); // NOI18N
        jButton4.setEnabled(false);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jLabel2.setText(bundle.getString("GameFrame.jLabel2.text")); // NOI18N

        jLabel3.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        jLabel3.setText(bundle.getString("GameFrame.jLabel3.text")); // NOI18N

        jLabel4.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        jLabel4.setText("0");

        jLabel5.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        jLabel5.setText("0");

        jLabel6.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        jLabel6.setText(bundle.getString("GameFrame.jLabel6.text")); // NOI18N

        jLabel7.setFont(new java.awt.Font("Consolas", 1, 18)); // NOI18N
        jLabel7.setText("03:00");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel1)
                            .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel7)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel2)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(54, 54, 54)
                        .addComponent(jLabel6)))
                .addContainerGap(34, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel1)
                        .addGap(31, 31, 31)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel5))
                        .addGap(51, 51, 51))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        if (serverSocket != null) {
            if (serverSocket.isOpen()) {
                try {
                    //serverSocket.socket().close();
                    selector.close();
                    
                    serverSocket.close();
                    
                    
                } catch (Exception ex) {
                    Logger.getLogger(GameFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        ip = "";
        port = 0;
        ConnectionFrame form = new ConnectionFrame(this, true, true);
        form.setVisible(true);
        if (!choose) {
            return;
        }
        if (locale.getLanguage().equals(russianLocale.getLanguage())) {
            jLabel1.setText("Ожидание второго игрока");
        } else {
            jLabel1.setText("Waiting of your opponent");
        }
        if (choose) {
            try {
                selector = Selector.open();
                serverSocket = ServerSocketChannel.open();

                serverSocket.socket().setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("localhost", port));
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);
                buffer = ByteBuffer.allocate(2048);
                ServerTask t = new GameFrame.ServerTask();
                //jButton2.setEnabled(false);

                t.execute();

                client = false;
                yourStep = false;
                drawField();
                newMatrix();

            } catch (Exception ex) {
                ex.printStackTrace();
                Logger
                        .getLogger(GameFrame.class
                                .getName()).log(Level.SEVERE, null, ex);
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    JOptionPane.showMessageDialog(null, "Сервер не создан! " + ex.getMessage(), "Подключение", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Server has not created! " + ex.getMessage(), "Connecting", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
        }


    }//GEN-LAST:event_jButton1ActionPerformed
    void checkUnclosedContour() {
        boolean stop = false;
        if (yourStep) {
            for (int i = 0; i < unclosedContoursM.size(); i++) {
                for (int j = 0; j < unclosedContoursM.get(i).size(); j++) {
                    Point p = unclosedContoursM.get(i).get(j);

                    if (points[p.x][p.y] != "10") {
                        unclosedContoursM.remove(i);
                        stop = true;
                        break;
                    }

                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                return;
            }
            for (int x = 0; x < 39; x++) {
                for (int y = 0; y < 32; y++) {
                    for (int i = 0; i < unclosedContoursM.size(); i++) {
                        if (((pnpoly(unclosedContoursM.get(i), x, y))) && (points[x][y] == "20")) {
                            points[x][y] = "32";
                            myScore++;
                            drawCapturedPoint(x, y);
                            drawPolygon(unclosedContoursM.get(i));
                            contours.add(unclosedContoursM.get(i));
                        }
                    }
                }
            }
            for (int i = 0; i < unclosedContoursM.size(); i++) {
                if (contours.contains(unclosedContoursM.get(i))) {
                    unclosedContoursM.remove(i);
                }
            }
        } else {
            for (int i = 0; i < unclosedContoursO.size(); i++) {
                for (int j = 0; j < unclosedContoursO.get(i).size(); j++) {
                    Point p = unclosedContoursO.get(i).get(j);
                    if (points[p.x][p.y] != "20") {
                        unclosedContoursO.remove(i);
                        stop = true;
                        break;
                    }
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                return;
            }
            for (int x = 0; x < 39; x++) {
                for (int y = 0; y < 32; y++) {
                    for (int i = 0; i < unclosedContoursO.size(); i++) {
                        if (((pnpoly(unclosedContoursO.get(i), x, y))) && (points[x][y] == "10")) {
                            points[x][y] = "31";
                            opponentScore++;
                            drawCapturedPoint(x, y);
                            drawPolygon(unclosedContoursO.get(i));
                            contours.add(unclosedContoursO.get(i));
                        }
                    }
                }
            }
            for (int i = 0; i < unclosedContoursM.size(); i++) {
                if (contours.contains(unclosedContoursM.get(i))) {
                    unclosedContoursM.remove(i);
                }
            }
        }
    }

    boolean pnpoly(ArrayList<Point> polygon, int x, int y) {
        boolean c = false;
        int npol = polygon.size();
        for (int i = 0, j = npol - 1; i < npol; j = i++) {
            if ((((polygon.get(i).y <= y) && (y < polygon.get(j).y)) || ((polygon.get(j).y <= y) && (y < polygon.get(i).y)))
                    && (x > (polygon.get(j).x - polygon.get(i).x) * (y - polygon.get(i).y) / (polygon.get(j).y - polygon.get(i).y) + polygon.get(i).x)) {
                c = !c;
            }
        }
        return c;
    }

    void drawField() {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(1, 1, jPanel1.getWidth() - 2, jPanel1.getHeight() - 2);
        g.setColor(Color.GRAY);
        for (int i = 0; i < 32; i++) {
            g.drawLine(1, 10 + 20 * i, jPanel1.getWidth() - 2, 10 + 20 * i);
        }
        for (int i = 0; i < 39; i++) {
            g.drawLine(10 + 20 * i, 1, 10 + 20 * i, jPanel1.getHeight() - 2);
        }
    }

    int countNewContours(int x, int y) {
        int[][] matrix = new int[39][32];
        for (int i = 0; i < 39; i++) {
            for (int j = 0; j < 32; j++) {
                if (yourStep) {
                    if (points[i][j] == "10") {
                        matrix[i][j] = -1;
                    } else {
                        matrix[i][j] = 0;
                    }
                } else {
                    if (points[i][j] == "20") {
                        matrix[i][j] = -1;
                    } else {
                        matrix[i][j] = 0;
                    }
                }
            }
        }
        int count = waveAlgorythm(matrix, x, y);
        return count;
    }

    int waveAlgorythm(int[][] matrix, int x, int y) {

        int count = 0;
        int step;

        while (true) {
            boolean exit = false;
            if (x == 0) {
                if (y == 0) {
                    boolean check = true;
                    if (matrix[x][y + 1] == 0) {
                        matrix[x][y + 1] = -2;
                        check = false;
                    }
                    if (matrix[x + 1][y] == 0) {
                        matrix[x + 1][y] = -2;
                        check = false;
                    }
                    if (matrix[x + 1][y + 1] == 0) {
                        matrix[x + 1][y + 1] = -2;
                        check = false;
                    }
                    if (check) {
                        break;
                    }
                } else {
                    if (y == 31) {
                        boolean check = true;
                        if (matrix[x][y - 1] == 0) {
                            matrix[x][y - 1] = -2;
                            check = false;
                        }
                        if (matrix[x + 1][y] == 0) {
                            matrix[x + 1][y] = -2;
                            check = false;
                        }
                        if (matrix[x + 1][y - 1] == 0) {
                            matrix[x + 1][y - 1] = -2;
                            check = false;
                        }
                        if (check) {
                            break;
                        }
                    } else {
                        boolean check = true;
                        if (matrix[x][y + 1] == 0) {
                            matrix[x][y + 1] = -2;
                            check = false;
                        }
                        if (matrix[x][y - 1] == 0) {
                            matrix[x][y - 1] = -2;
                            check = false;
                        }
                        if (matrix[x + 1][y + 1] == 0) {
                            matrix[x + 1][y + 1] = 1;
                            exit = true;
                            check = false;
                        }
                        if ((matrix[x + 1][y - 1] == 0) && (!exit)) {
                            matrix[x + 1][y - 1] = 1;
                            exit = true;
                            check = false;
                        }
                        if ((matrix[x + 1][y] == 0) && (!exit)) {
                            matrix[x + 1][y] = 1;
                            exit = true;
                            check = false;
                        }
                        if (check) {
                            break;
                        }
                    }
                }
            } else {
                if (x == 38) {
                    if (y == 0) {
                        boolean check = true;
                        if (matrix[x - 1][y] == 0) {
                            matrix[x - 1][y] = -2;
                            check = false;
                        }
                        if (matrix[x - 1][y + 1] == 0) {
                            matrix[x - 1][y + 1] = -2;
                            check = false;
                        }
                        if (matrix[x][y + 1] == 0) {
                            matrix[x][y + 1] = -2;
                            check = false;
                        }
                        if (check) {
                            break;
                        }
                    } else {
                        if (y == 31) {
                            boolean check = true;
                            if (matrix[x - 1][y] == 0) {
                                matrix[x - 1][y] = -2;
                                check = false;
                            }
                            if (matrix[x - 1][y - 1] == 0) {
                                matrix[x - 1][y - 1] = -2;
                                check = false;
                            }
                            if (matrix[x][y - 1] == 0) {
                                matrix[x][y - 1] = -2;
                                check = false;
                            }
                            if (check) {
                                break;
                            }
                        } else {
                            boolean check = true;
                            if (matrix[x][y + 1] == 0) {
                                matrix[x][y + 1] = -2;
                                check = false;
                            }
                            if (matrix[x][y - 1] == 0) {
                                matrix[x][y - 1] = -2;
                                check = false;
                            }
                            if (matrix[x - 1][y + 1] == 0) {
                                matrix[x - 1][y + 1] = 1;
                                exit = true;
                                check = false;
                            }
                            if ((matrix[x - 1][y - 1] == 0) && (!exit)) {
                                matrix[x - 1][y - 1] = 1;
                                exit = true;
                                check = false;
                            }
                            if ((matrix[x - 1][y] == 0) && (!exit)) {
                                matrix[x - 1][y] = 1;
                                exit = true;
                                check = false;
                            }
                            if (check) {
                                break;
                            }
                        }
                    }
                }
            }
            if ((y == 0) && (x > 0) && (x < 38)) {
                boolean check = true;
                if (matrix[x + 1][y] == 0) {
                    matrix[x + 1][y] = -2;
                    check = false;
                }
                if (matrix[x - 1][y] == 0) {
                    matrix[x - 1][y] = -2;
                    check = false;
                }
                if (matrix[x - 1][y + 1] == 0) {
                    matrix[x - 1][y + 1] = 1;
                    exit = true;
                    check = false;
                }
                if ((matrix[x + 1][y + 1] == 0) && (!exit)) {
                    matrix[x + 1][y + 1] = 1;
                    exit = true;
                    check = false;
                }
                if ((matrix[x][y + 1] == 0) && (!exit)) {
                    matrix[x][y + 1] = 1;
                    exit = true;
                    check = false;
                }
                if (check) {
                    break;
                }
            }
            if ((y == 31) && (x > 0) && (x < 38)) {
                boolean check = true;
                if (matrix[x + 1][y] == 0) {
                    matrix[x + 1][y] = -2;
                    check = false;
                }
                if (matrix[x - 1][y] == 0) {
                    matrix[x - 1][y] = -2;
                    check = false;
                }
                if (matrix[x - 1][y - 1] == 0) {
                    matrix[x - 1][y - 1] = 1;
                    exit = true;
                    check = false;
                }
                if ((matrix[x + 1][y - 1] == 0) && (!exit)) {
                    matrix[x + 1][y - 1] = 1;
                    exit = true;
                    check = false;
                }
                if ((matrix[x][y - 1] == 0) && (!exit)) {
                    matrix[x][y - 1] = 1;
                    exit = true;
                    check = false;
                }
                if (check) {
                    break;
                }
            }
            if ((y == 30) && (x > 0) && (x < 38)) {
                if (matrix[x + 1][y + 1] == 0) {
                    matrix[x + 1][y + 1] = -2;
                }
                if (matrix[x][y + 1] == 0) {
                    matrix[x][y + 1] = -2;
                }
                if (matrix[x - 1][y + 1] == 0) {
                    matrix[x - 1][y + 1] = -2;
                }

            }
            if ((y == 1) && (x > 0) && (x < 38)) {
                if (matrix[x + 1][y - 1] == 0) {
                    matrix[x + 1][y - 1] = -2;
                }
                if (matrix[x][y - 1] == 0) {
                    matrix[x][y - 1] = -2;
                }
                if (matrix[x - 1][y - 1] == 0) {
                    matrix[x - 1][y - 1] = -2;
                }

            }
            if ((x == 1) && (y > 0) && (y < 31)) {
                if (matrix[x - 1][y] == 0) {
                    matrix[x - 1][y] = -2;
                }
                if (matrix[x - 1][y - 1] == 0) {
                    matrix[x - 1][y - 1] = -2;
                }
                if (matrix[x - 1][y + 1] == 0) {
                    matrix[x - 1][y + 1] = -2;
                }

            }
            if ((x == 37) && (y > 0) && (y < 31)) {
                if (matrix[x + 1][y] == 0) {
                    matrix[x + 1][y] = -2;
                }
                if (matrix[x + 1][y - 1] == 0) {
                    matrix[x + 1][y - 1] = -2;
                }
                if (matrix[x + 1][y + 1] == 0) {
                    matrix[x + 1][y + 1] = -2;
                }

            }

            if ((x >= 1) && (x <= 37) && (y >= 1) && (y <= 30)) {
                if (matrix[x - 1][y] == 0) {
                    matrix[x - 1][y] = 1;
                } else {
                    if (matrix[x][y - 1] == 0) {
                        matrix[x][y - 1] = 1;
                    } else {
                        if (matrix[x + 1][y] == 0) {
                            matrix[x + 1][y] = 1;
                        } else {
                            if (matrix[x][y + 1] == 0) {
                                matrix[x][y + 1] = 1;
                            } else if (matrix[x - 1][y - 1] == 0) {
                                matrix[x - 1][y - 1] = 1;
                            } else {
                                if (matrix[x - 1][y + 1] == 0) {
                                    matrix[x - 1][y + 1] = 1;
                                } else {
                                    if (matrix[x + 1][y - 1] == 0) {
                                        matrix[x + 1][y - 1] = 1;
                                    } else {
                                        if (matrix[x + 1][y + 1] == 0) {
                                            matrix[x + 1][y + 1] = 1;
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }

            step = 0;
            boolean stop = false, contour = true;
            while (!stop) {
                step++;
                contour = true;
                for (int i = 1; i < 39; i++) {
                    for (int j = 1; j < 32; j++) {
                        if (matrix[i][j] == step) {
                            contour = false;
                            if (i - 1 <= 0) {
                                if (i == 0) {
                                    stop = true;
                                    break;
                                }
                                if (matrix[i - 1][j] == 0) {
                                    stop = true;
                                    break;
                                }
                            } else if (matrix[i - 1][j] == 0) {
                                matrix[i - 1][j] = step + 1;
                            }
                            if (i + 1 > 37) {
                                if (i == 38) {
                                    stop = true;
                                    break;
                                }
                                if (matrix[i + 1][j] == 0) {
                                    stop = true;
                                    break;
                                }
                            } else if (matrix[i + 1][j] == 0) {
                                matrix[i + 1][j] = step + 1;
                            }
                            if (j - 1 <= 0) {
                                if (j == 0) {
                                    stop = true;
                                    break;
                                }
                                if (matrix[i][j - 1] == 0) {
                                    stop = true;
                                    break;
                                }
                            } else if (matrix[i][j - 1] == 0) {
                                matrix[i][j - 1] = step + 1;
                            }
                            if (j + 1 > 30) {
                                if (j == 31) {
                                    stop = true;
                                    break;
                                }
                                if (matrix[i][j + 1] == 0) {
                                    stop = true;
                                    break;
                                }
                            } else if (matrix[i][j + 1] == 0) {
                                matrix[i][j + 1] = step + 1;
                            }
                        }
                    }
                    if (stop) {

                        break;
                    }
                }

                if (contour) {
                    stop = true;
                }
            }
            if (contour) {
                ArrayList<Point> p = findContour(matrix);
                if (p != null) {
                    if (!contours.contains(p)) {
                        boolean dots = false;
                        for (int i = 0; i < 39; i++) {
                            for (int j = 0; j < 32; j++) {
                                if (matrix[i][j] > 0) {
                                    if (yourStep) {
                                        if (points[i][j] == "20") {
                                            dots = true;
                                        }
                                    } else {
                                        if (points[i][j] == "10") {
                                            dots = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (dots) {
                            for (int i = 0; i < 39; i++) {
                                for (int j = 0; j < 32; j++) {
                                    if (matrix[i][j] > 0) {
                                        if (yourStep) {
                                            if (points[i][j] == "20") {
                                                points[i][j] = "32";
                                                myScore++;
                                                jLabel4.setText(String.valueOf(myScore));
                                                drawCapturedPoint(i, j);
                                            }
                                            if (points[i][j] == "00") {
                                                points[i][j] = "30";
                                            }
                                        } else {
                                            if (points[i][j] == "10") {
                                                points[i][j] = "31";
                                                opponentScore++;
                                                jLabel5.setText(String.valueOf(opponentScore));
                                                drawCapturedPoint(i, j);
                                            }
                                            if (points[i][j] == "00") {
                                                points[i][j] = "30";
                                            }
                                        }
                                    }

                                }
                            }
                            contours.add(p);
                            count++;
                        } else {
                            if (yourStep) {
                                unclosedContoursM.add(p);
                            } else {
                                unclosedContoursO.add(p);
                            }
                        }
                    }

                }
            }
            matrix = resetMatrix(matrix, x, y);
        }
        return count;
    }

    ArrayList<Point> findContour(int[][] matrix) {
        ArrayList<Point> list = new ArrayList<Point>();
        for (int i = 0; i < 39; i++) {
            for (int j = 0; j < 32; j++) {
                if (i == 0) {
                    if (j == 0) {
                        if ((matrix[i][j] == -1) && ((matrix[i + 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                            list.add(new Point(i, j));
                        }
                    } else {
                        if (j == 31) {
                            if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i + 1][j] > 0))) {
                                list.add(new Point(i, j));
                            }
                        } else {
                            if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i + 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                list.add(new Point(i, j));
                            }
                        }
                    }
                } else {
                    if (i == 38) {
                        if (j == 0) {
                            if ((matrix[i][j] == -1) && ((matrix[i - 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                list.add(new Point(i, j));
                            }
                        } else {
                            if (j == 31) {
                                if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i - 1][j] > 0))) {
                                    list.add(new Point(i, j));
                                }
                            } else {
                                if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i - 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                    list.add(new Point(i, j));
                                }
                            }
                        }
                    } else {
                        if (j == 0) {
                            if (i == 0) {
                                if ((matrix[i][j] == -1) && ((matrix[i + 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                    list.add(new Point(i, j));
                                }
                            } else {
                                if (i == 38) {
                                    if ((matrix[i][j] == -1) && ((matrix[i - 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                        list.add(new Point(i, j));
                                    }
                                } else {
                                    if ((matrix[i][j] == -1) && ((matrix[i - 1][j] > 0) || (matrix[i + 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                        list.add(new Point(i, j));
                                    }
                                }
                            }
                        } else {
                            if (j == 31) {
                                if (i == 0) {
                                    if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i + 1][j] > 0))) {
                                        list.add(new Point(i, j));
                                    }
                                } else {
                                    if (i == 38) {
                                        if ((matrix[i][j] == -1) && ((matrix[i][j - 1] > 0) || (matrix[i - 1][j] > 0))) {
                                            list.add(new Point(i, j));
                                        }
                                    } else {
                                        if ((matrix[i][j] == -1) && ((matrix[i - 1][j] > 0) || (matrix[i + 1][j] > 0) || (matrix[i][j - 1] > 0))) {
                                            list.add(new Point(i, j));
                                        }
                                    }
                                }
                            } else {
                                if ((matrix[i][j] == -1) && ((matrix[i - 1][j] > 0) || (matrix[i][j - 1] > 0) || (matrix[i + 1][j] > 0) || (matrix[i][j + 1] > 0))) {
                                    list.add(new Point(i, j));
                                }
                            }
                        }
                    }
                }

            }
        }
        ArrayList<Point> sortedList = new ArrayList<Point>();
        sortedList.add(list.get(0));
        Point movedPoint = list.get(0);
        list.remove(0);
        boolean stop = true;
        while (stop) {
            //while (!list.isEmpty()) {
            stop = false;
            for (int i = 0; i < list.size(); i++) {
                int index = 0;
                if (list.indexOf(new Point(movedPoint.x - 1, movedPoint.y)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x - 1, movedPoint.y));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x - 1, movedPoint.y - 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x - 1, movedPoint.y - 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x, movedPoint.y - 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x, movedPoint.y - 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x + 1, movedPoint.y - 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x + 1, movedPoint.y - 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x + 1, movedPoint.y)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x + 1, movedPoint.y));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x + 1, movedPoint.y + 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x + 1, movedPoint.y + 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x, movedPoint.y + 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x, movedPoint.y + 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
                if (list.indexOf(new Point(movedPoint.x - 1, movedPoint.y + 1)) != -1) {
                    index = list.indexOf(new Point(movedPoint.x - 1, movedPoint.y + 1));
                    sortedList.add(list.get(index));
                    movedPoint = list.get(index);
                    list.remove(index);
                    stop = true;
                    break;
                }
            }
        }
        for (int i = 0; i < sortedList.size(); i++) {
            if (i == sortedList.size() - 1) {
                if (!(((sortedList.get(i).x >= sortedList.get(0).x - 1) && (sortedList.get(i).x <= sortedList.get(0).x + 1)) && ((sortedList.get(i).y >= sortedList.get(0).y - 1) && (sortedList.get(i).y <= sortedList.get(0).y + 1)))) {
                    return null;
                }
            } else if (!(((sortedList.get(i).x >= sortedList.get(i + 1).x - 1) && (sortedList.get(i).x <= sortedList.get(i + 1).x + 1)) || ((sortedList.get(i).y >= sortedList.get(i + 1).y - 1) && (sortedList.get(i).y <= sortedList.get(i + 1).y + 1)))) {
                return null;
            }
        }
        return sortedList;
    }

    int[][] resetMatrix(int[][] matrix, int x, int y
    ) {
        for (int i = 0; i < 39; i++) {
            for (int j = 0; j < 32; j++) {
                if (matrix[i][j] > 0) {
                    if (((i == x - 1) || (i == x + 1) || (i == x)) && ((j == y - 1) || (j == y + 1) || (j == y))) {
                        matrix[i][j] = -2;
                    } else {
                        matrix[i][j] = 0;
                    }
                }
            }
        }
        return matrix;
    }

    void drawPolygon(ArrayList<Point> polygon
    ) {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        Color color;
        if (client) {
            if (yourStep) {
                color = new Color(0, 0, 255, 63);
            } else {
                color = new Color(255, 0, 0, 63);
            }
        } else {
            if (yourStep) {
                color = new Color(255, 0, 0, 63);
            } else {
                color = new Color(0, 0, 255, 63);
            }
        }
        g.setColor(color);
        int[] x = new int[polygon.size()];
        int[] y = new int[polygon.size()];
        for (int i = 0; i < polygon.size(); i++) {
            x[i] = polygon.get(i).x * 20 + 10;
            y[i] = polygon.get(i).y * 20 + 10;
        }
        g.fillPolygon(x, y, polygon.size());
    }

    Point getPosition(int x, int y
    ) {
        return new Point((x - 5) / 20, (y - 5) / 20);
    }

    Point getPosition(Point p
    ) {
        return new Point((p.x - 5) / 20, (p.y - 5) / 20);
    }

    void newMatrix() {
        points = new String[39][32];
        for (int i = 0; i < 39; i++) {
            for (int j = 0; j < 32; j++) {
                points[i][j] = "00";
            }
        }
        if (!client) {
            points[18][14] = "10";
            points[18][15] = "20";
            points[19][14] = "20";
            points[19][15] = "10";
        } else {
            points[18][15] = "10";
            points[18][14] = "20";
            points[19][15] = "20";
            points[19][14] = "10";
        }

    }

    boolean canDrawPoint(int x, int y
    ) {
        return (points[x][y].equals("00"));
    }

    void drawTranslucentPoint(int x, int y
    ) {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        Color color;
        if (client) {
            if (yourStep) {
                color = new Color(0, 0, 255, 63);
            } else {
                color = new Color(255, 0, 0, 63);
            }
        } else {
            if (yourStep) {
                color = new Color(255, 0, 0, 63);
            } else {
                color = new Color(0, 0, 255, 63);
            }
        }
        g.setColor(color);
        g.fillOval(x * 20 + 5, y * 20 + 5, 10, 10);

    }

    void drawPoint(int x, int y
    ) {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        Color color;
        if (client) {
            if (yourStep) {
                color = new Color(0, 0, 255, 255);
            } else {
                color = new Color(255, 0, 0, 255);
            }
        } else {
            if (yourStep) {
                color = new Color(255, 0, 0, 255);
            } else {
                color = new Color(0, 0, 255, 255);
            }
        }
        g.setColor(color);
        g.fillOval(x * 20 + 5, y * 20 + 5, 10, 10);

    }

    void drawCapturedPoint(int x, int y
    ) {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        Color color;
        color = Color.YELLOW;
        g.setColor(color);
        g.fillOval(x * 20 + 5, y * 20 + 5, 10, 10);
        if (client) {
            if (!yourStep) {
                color = new Color(0, 0, 255, 255);
            } else {
                color = new Color(255, 0, 0, 255);
            }
        } else {
            if (!yourStep) {
                color = new Color(255, 0, 0, 255);
            } else {
                color = new Color(0, 0, 255, 255);
            }
        }
        g.setColor(color);
        g.fillOval(x * 20 + 6, y * 20 + 6, 8, 8);

    }

    void clearPoint(int x, int y
    ) {
        Graphics2D g = (Graphics2D) jPanel1.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(x * 20 + 5, y * 20 + 5, 10, 10);
        g.setColor(Color.GRAY);
        g.drawLine(x * 20 + 5, y * 20 + 10, x * 20 + 15, y * 20 + 10);
        g.drawLine(x * 20 + 10, y * 20 + 5, x * 20 + 10, y * 20 + 15);
    }

    private void jPanel1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MouseMoved
        // TODO add your handling code here:
        if (!yourStep) {
            return;
        }
        Point p = getPosition(evt.getPoint());
        if (points[drawX][drawY] == "00") {
            clearPoint(drawX, drawY);
        }
        if (canDrawPoint(p.x, p.y)) {
            drawTranslucentPoint(p.x, p.y);
            drawX = p.x;
            drawY = p.y;
        }

    }//GEN-LAST:event_jPanel1MouseMoved
    void repaintField() {
        drawField();
        boolean oldYourStep = yourStep;
        for (int i = 0; i < 39; i++) {
            for (int j = 0; j < 32; j++) {
                if (points[i][j] == "10") {
                    yourStep = true;
                    drawPoint(i, j);
                }
                if (points[i][j] == "20") {
                    yourStep = false;
                    drawPoint(i, j);
                }
                if (points[i][j] == "31") {
                    yourStep = false;
                    drawCapturedPoint(i, j);
                }
                if (points[i][j] == "32") {
                    yourStep = true;
                    drawCapturedPoint(i, j);
                }
            }
        }
        for (int i = 0; i < contours.size(); i++) {
            yourStep = points[contours.get(i).get(0).x][contours.get(i).get(0).y] == "10";
            drawPolygon(contours.get(i));
        }
        yourStep = oldYourStep;
    }
    private void jPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MouseClicked
        // TODO add your handling code here:

        Point p = getPosition(evt.getPoint());
        if ((yourStep) && (canDrawPoint(p.x, p.y))) {
            drawPoint(p.x, p.y);
            ArrayList<String> message = new ArrayList<String>();
            message.add("1");
            message.add(String.valueOf(p.x));
            message.add(String.valueOf(p.y));
            ByteBuffer buf = ByteBuffer.wrap(ObjectToBytes(message));
            buf.compact();
            buf.flip();
            try {
                socket.write(buf);

            } catch (IOException ex) {
                Logger.getLogger(GameFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            buf.flip();

            points[p.x][p.y] = "10";
            int c = countNewContours(p.x, p.y);
            if (c >= 1) {
                for (int i = 1; i <= c; i++) {
                    drawPolygon(contours.get(contours.size() - i));
                }
            };
            if (!endGame) {
                yourStep = false;
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    jLabel1.setText("Ожидание хода противника");
                } else {
                    jLabel1.setText("Waiting opponent's move");
                }
            }
            checkUnclosedContour();

        }
    }//GEN-LAST:event_jPanel1MouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        if (serverSocket != null) {
            try {
                serverSocket.socket().close();
selector.selectNow();
            } catch (IOException ex) {
                Logger.getLogger(GameFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        choose = false;
        ip = "";
        port = 0;
        ConnectionFrame form = new ConnectionFrame(this, true, false);
        form.setVisible(true);
        client = true;
        if (choose) {
            try {
                socket = SocketChannel.open(new InetSocketAddress(ip, port));
                socket.configureBlocking(false);
                ArrayList<String> message = new ArrayList<String>();
                message.add("0");
                message.add(password);
                ByteBuffer buf = ByteBuffer.wrap(ObjectToBytes(message));
                buf.compact();
                buf.flip();
                socket.write(buf);
                buf.flip();
                socket.configureBlocking(true);
                byte[] a = new byte[0];
                int read = 0;
                while ((read = socket.read(buf)) > 0) {
                    socket.configureBlocking(false);
                    buf.flip();
                    byte[] bytes = new byte[buf.limit()];
                    buf.get(bytes);
                    byte[] newArray = new byte[a.length + bytes.length];
                    System.arraycopy(a, 0, newArray, 0, a.length);
                    System.arraycopy(bytes, 0, newArray, a.length, bytes.length);
                    a = newArray;
                    buf.clear();
                }

                ArrayList<String> resp = (ArrayList<String>) BytesToObject(a);
                if (resp.get(0).equals("7")) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Подключение прошло успешно!", "Подключение", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Connection was successfully!", "Connection", JOptionPane.INFORMATION_MESSAGE);
                    }
                    client = true;
                    yourStep = true;
                    drawField();
                    newMatrix();
                    drawPoint(18, 15);
                    drawPoint(19, 14);
                    yourStep = false;
                    drawPoint(19, 15);
                    drawPoint(18, 14);
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        jLabel1.setText("Ожидание хода противника");
                    } else {
                        jLabel1.setText("Waiting opponent's move");

                    }
                    t = new GameFrame.ClientTask();
                    t.execute();
                    game = true;
                    jButton4.setEnabled(true);
                    jButton4.setText("Стоп");
                    jLabel6.setVisible(false);
                    jLabel7.setVisible(false);
                    minutes = 3;
                    seconds = 0;
                    endGame = false;
                    jButton1.setEnabled(false);
                    jButton2.setEnabled(false);
                    myScore = 0;
                    opponentScore = 0;
                    jLabel4.setText("0");
                    jLabel5.setText("0");
                }
                if (resp.get(0).equals("8")) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Неверный пароль!", "Подключение", JOptionPane.ERROR_MESSAGE);
                        //EndGame();

                        jLabel1.setText("Неверный пароль!");
                    } else {
                        JOptionPane.showMessageDialog(null, "Wrong password!", "Connection", JOptionPane.ERROR_MESSAGE);
                        //EndGame();

                        jLabel1.setText("Wrong password!");
                    }
                }
                if (resp.get(0).equals("9")) {
                    if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                        JOptionPane.showMessageDialog(null, "Сервер занят!", "Подключение", JOptionPane.ERROR_MESSAGE);
                        //EndGame();
                        jLabel1.setText("Сервер занят!");
                    } else {
                        JOptionPane.showMessageDialog(null, "Server is busy!", "Connection", JOptionPane.ERROR_MESSAGE);
                        //EndGame();
                        jLabel1.setText("Server is busy!");
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(GameFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    JOptionPane.showMessageDialog(null, "Ошибка подключения к серверу!", "Подключение", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Error connecting to server!", "Connection", JOptionPane.ERROR_MESSAGE);
                }
                //EndGame();

            }
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        // TODO add your handling code here:
        if (game) {
            repaintField();
        }
    }//GEN-LAST:event_formWindowActivated

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        if ((jButton4.getText().equals("Стоп")) || (jButton4.getText().equals("Stop"))) {
            jLabel7.setText("03:00");
            jLabel6.setVisible(true);
            jLabel7.setVisible(true);
            jButton4.setEnabled(false);
            yourStep = false;
            endGame = true;
            if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                jLabel1.setText("Заключ. действия противника");
            } else {
                jLabel1.setText("Closing moves of opponent");
            }
            ArrayList<String> message = new ArrayList<String>();
            message.add("2");
            ByteBuffer buf = ByteBuffer.wrap(ObjectToBytes(message));
            buf.compact();
            buf.flip();
            try {
                socket.write(buf);
            } catch (Exception ex) {
                Logger.getLogger(GameFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            buf.flip();

            jLabel7.setText("03:00");
            jLabel6.setVisible(true);
            jLabel7.setVisible(true);
            minutes = 3;
            seconds = 0;
            endGame = true;
            yourStep = true;
            timer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (seconds == 0) {
                        seconds = 59;
                        minutes--;
                        if (minutes < 0) {
                            seconds = 0;
                        }
                    } else {
                        seconds--;
                    }
                    jLabel7.setText("0" + String.valueOf(minutes) + ":" + String.format("%02d", seconds));

                }
            });
            timer.start();
        } else {
            EndGame(socket.socket().getChannel());
            if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                jLabel1.setText("Конец игры!");
            } else {
                jLabel1.setText("Game over!");
            }
            yourStep = false;
            if (myScore > opponentScore) {
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    JOptionPane.showMessageDialog(null, "Игра закончена! Вы выиграли, окружив " + String.valueOf(myScore) + " точек противника. Противник окружил лишь " + String.valueOf(opponentScore) + "точек.");

                } else {
                    JOptionPane.showMessageDialog(null, "Game over! You won having surrounded " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded only " + String.valueOf(opponentScore) + "dots.");
                }
            }
            if (myScore < opponentScore) {
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    JOptionPane.showMessageDialog(null, "Игра закончена! Вы проиграли, окружив лишь " + String.valueOf(myScore) + " точек противника. Противник окружил " + String.valueOf(opponentScore) + "точек.");
                } else {
                    JOptionPane.showMessageDialog(null, "Game over! You lose having surrounded only " + String.valueOf(myScore) + " opponent's dots. Your opponent surrounded " + String.valueOf(opponentScore) + "dots.");
                }
            }
            if (myScore == opponentScore) {
                if (locale.getLanguage().equals(russianLocale.getLanguage())) {
                    JOptionPane.showMessageDialog(null, "Игра закончена! Ничья. Вы окружили по " + String.valueOf(myScore) + " точек.");
                } else {
                    JOptionPane.showMessageDialog(null, "Game over! Tie. You each surrounded " + String.valueOf(myScore) + " dots.");
                }
            }
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        (new RulesFrame()).setVisible(true);
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GameFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GameFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GameFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GameFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GameFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
