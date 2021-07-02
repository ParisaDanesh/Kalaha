package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 *
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    // Files details
    String FILENAME = "./BestMoves.txt";
    FileWriter writer ;
    FileReader reader ;

    String result[];
    HashMap<String, Integer> hmap = new HashMap<String, Integer>();

    private int player;
    private JTextArea text;

    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;

    /**
     * Creates a new client.
     */
    public AIClient()
    {
        player = -1;
        connected = false;

        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();

        try
        {
            File file = new File(FILENAME);
            if(file.exists()) {
                reader = new FileReader(FILENAME);
                writer = new FileWriter(FILENAME, true);
                BufferedReader br = new BufferedReader(reader);
                StringBuffer sb = new StringBuffer();
                String line;
                while((line=br.readLine())!=null){
                    sb.append(line + "\n");
                }
                reader.close();

                result = sb.toString().split("\n");
                //Key-Value
                for (String item : result){
                    String[] parts = item.split(";");
                    hmap.put(parts[0], Integer.parseInt(parts[1]));
                }

            }
            else {
                writer = new FileWriter(FILENAME);
//                System.out.println("No");
            }

//            System.exit(0);


            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }

    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }

    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());

        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));

        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

    /**
     * Adds a text string to the GUI textarea.
     *
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }

    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;

        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);

                    addText("I am player " + player);
                }

                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }

                    // Close File. I added it here
                    writer.close();

                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = 0;

                            // we added the code here
                            if(!IsNullOrEmpty(result)){
                                if(hmap.containsKey(Arrays.toString(currentBoard.getBoard()))){
                                    cMove = hmap.get(
                                            Arrays.toString(currentBoard.getBoard())
                                    );
                                    System.out.println(Arrays.toString(currentBoard.getBoard()));
                                    System.out.println("--------------------------------------\n");
                                }else{
                                    cMove = getMove(currentBoard);
                                    System.out.println(Arrays.toString(currentBoard.getBoard()));
                                    System.out.println("--------------------------------------\n");
                                    try {
                                        writer.write(Arrays.toString(currentBoard.getBoard()) + ";" + cMove + "\n");

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }else{
                                cMove = getMove(currentBoard);
                                try {
                                    writer.write(Arrays.toString(currentBoard.getBoard()) + ";" + cMove + "\n");

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;

                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }

                        }
                    }
                }

                //Wait
                Thread.sleep(100);
            }
        }
        catch (Exception ex)
        {
            running = false;
        }

        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }

    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     *
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard) {

        System.out.println("Iterative deepening...");

        int myMove = 0;
        myMove = IterativeDeepeningSearch(currentBoard);

        return myMove;
    }

    int IterativeDeepeningSearch(GameState currentBoard)
    {
        int bestMove = 0;
        int bestState = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int tmp = 0;

        int iter_depth = 1;

        for(int i=1; i < 7 ; i++)
        {
            System.out.println("our depth now is: " + iter_depth);
            //we have 6 ambos, so we should devide 5000msec for to calculate time for each ambos
            long limit = System.currentTimeMillis() + 5000/6;

            while (true) {
                long currentTime = System.currentTimeMillis();
//                System.out.println(limit - now);
                if (limit - currentTime <= 0)
                    break;

                if (currentBoard.moveIsPossible(i))
                {
                    GameState new_board = currentBoard.clone();
                    new_board.makeMove(i);

                    tmp = minimaxSearch(new_board, iter_depth,player, alpha, beta);

                    if(tmp > bestState)
                    {
                        bestState = tmp;
                        bestMove = i;
                    }
                }
            }
            iter_depth++;
        }

        return bestMove;
    }

    public int minimaxSearch(GameState currentBoard, int depth,int which_player, int alpha, int beta)
    {
        int bestState = 0;
        int tmp;

        if(depth<=0) {
            if (player == 1)
                return (currentBoard.getScore(1) - currentBoard.getScore(2));
            else
                return (currentBoard.getScore(2) - currentBoard.getScore(1));
        }

        for(int i = 1;i<7;i++)
        {
            if(currentBoard.moveIsPossible(i)){
                GameState new_board = currentBoard.clone();
                new_board.makeMove(i);

                if(which_player != new_board.getNextPlayer()){
                    bestState = Integer.MAX_VALUE;
                    tmp = minimaxSearch(new_board, --depth, player, alpha, beta);
                    bestState = Math.min(bestState, tmp);

                    //beta-prunning for min
                    beta = Math.min(beta, bestState);
                    if (beta <= alpha)
                        break;

                }else{
                    bestState = Integer.MIN_VALUE;
                    tmp = minimaxSearch(new_board,--depth,player, alpha, beta);
                    bestState = Math.max(bestState, tmp);

                    //alpha-prunning for max
                    alpha = Math.max(alpha, bestState);
                    if(beta <= alpha)
                        break;
                }
            }
        }

        return bestState;
    }

    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     *
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }

    static boolean IsNullOrEmpty(String[] myStringArray) {
        return myStringArray == null || myStringArray.length < 1;
    }
}