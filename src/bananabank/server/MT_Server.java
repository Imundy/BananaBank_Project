package bananabank.server;
/* Author :Ian Mundy
 * ian.m.mundy@vadnerbilt.edu
 * 2/27/2014
 * CS283
 * Some code taken from CS283 in class examples and refitted for assignment
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;


public class MT_Server {

	//Class variables
	private static final int PORT = 8889;
	private static ArrayList<ServerThread> threads_;
	private static BananaBank BANK;
	private static boolean cancelled_;
	private static ServerSocket ss_;

	public static void main(String[] args) throws IOException {
		
		BANK = new BananaBank("accounts.txt");
		System.out.println("BananaBank ServerSocket created");
		System.out.println("Waiting for client connection on port " + PORT);
		ss_ = new ServerSocket(PORT);
		cancelled_ = false;	
		threads_= new ArrayList<ServerThread>();
		try{
			while (!cancelled_) {
			
				Socket cs = ss_.accept();
				ServerThread s = new ServerThread(cs);
				threads_.add(s);
				
				s.start();
			}
		}catch(SocketException e){
			System.out.println("While exited safely");
		}

		
	}

	
	/**
	 * Run method handles server calls.
	 * Must be given a socket to be initialized to.
	 * 
	 * @author Ian Mundy
	 *
	 */
	private static class ServerThread extends Thread {
		
		Socket socket_;
		
		ServerThread(Socket socket){
			socket_ = socket;
		}
		
		public void run() {
			try{
				BufferedReader r = new BufferedReader(new InputStreamReader(
						socket_.getInputStream()));
				String line = "" ;
				PrintStream out = new PrintStream(socket_.getOutputStream());
				while(socket_.isBound() && (line = r.readLine()) != null ){
					String address = socket_.getInetAddress().toString();
					if(line.equals("SHUTDOWN") &&  address.equals("/127.0.0.1")){
						cancelled_ = true;
						shutdown();
					}else{
						
						//get info on account
						Scanner lineScanner = new Scanner(line);
						int  amt = lineScanner.nextInt();
						int src = lineScanner.nextInt();
						int dst = lineScanner.nextInt();
						Account srcAccount = BANK.getAccount(src);
						Account dstAccount = BANK.getAccount(dst);
						
						//check if accounts are valid
						if(srcAccount == null){
							out.println("ERR: Source Account not found");
						} else if( dstAccount == null){
							out.println("ERR: Destination Account not found");
						} else {
							
							//Always acquire lock for lower act # first
							if(src>dst){
								synchronized(dstAccount){
									synchronized(srcAccount){
										srcAccount.transferTo(amt, dstAccount);
									}
								}		
								out.println("Transferred " + amt + " from " +
								src + " to " + dst);
							}else if(dst>src){
								synchronized(srcAccount){
									synchronized(dstAccount){
										srcAccount.transferTo(amt, dstAccount);
									}
								}	
								out.println("Transferred " + amt + " from " +
								src + " to " + dst);
							}else{
								//same so we tell the client that
								out.println("ERR: Trying to transfer to same Account");
							}
						}
					}
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		/**
		 * Initiates the graceful shutdown of a server
		 * Waits for all threads to join other than this thread, which received the command
		 * then calls the countTotal method to return the money in the bank
		 */
		private  void shutdown(){
			for(ServerThread s : threads_){
				try{
					if(!s.equals(this))
							s.join();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
			
			
			System.out.println("SERVER SAVING...");
			try {
				PrintStream out = new PrintStream(socket_.getOutputStream());
				BANK.save("SAVED.txt");
				countTotal(out);
				ss_.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
		
		/**
		 * Takes a printstream object to which it will print the total sum of money
		 * in all accounts at the end.
		 * @param out
		 */
		private void countTotal(PrintStream out){
			Collection<Account> allAccounts = BANK.getAllAccounts();
			int total = 0;
			for(Account a : allAccounts){
				total += a.getBalance();
			}
			out.println(total);
			System.out.println("SERVER SHUTDOWN COMPLETED");
		}
	}
}
	

