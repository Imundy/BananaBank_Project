package bananabank.server;

/* Author :Ian Mundy
 * ian.m.mundy@vadnerbilt.edu
 * 2/27/2014
 * CS283
 * Some useful code taken from benchmarkclient
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class Client {

	// class variables. Useful for accounts.
	static final String SERVER_ADDRESS = "localhost";
	public static final int PORT = 8889;
	public static final int TRANSACTIONS_NUM = 100;
	private static final int THREADS = 4;
	public static final int ACCOUNT_NUMBERS[] = new int[] { 11111, 22222,
			33333, 44444, 55555, 66666, 77777, 88888 };

	public static void main(String[] args) {

		ArrayList<ClientThread> workers = new ArrayList<ClientThread>();

		for (int i = 0; i < THREADS; i++) {
			//create several threads
			ClientThread thread = new ClientThread();
			thread.start();

		}

		//call shutdown
		ShutDownThread thread = new ShutDownThread();
		thread.start();

		//make sure all threads join(or server won't ever be able to shutdown)
		for (ClientThread t : workers) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Client Thread Makes a specified number of requests for transfers to the
	 * Bank
	 * 
	 * @author Ian Mundy
	 * 
	 */
	private static class ClientThread extends Thread {

		@Override
		public void run() {
			try {
				// connect to the server
				Socket socket = new Socket(SERVER_ADDRESS, PORT);
				System.out.println("Client worker thread (thread id="
						+ Thread.currentThread().getId()
						+ ") connected to server");

				// set up input and output streams
				PrintStream ps = new PrintStream(socket.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				System.out.println("Client worker thread (thread id="
						+ Thread.currentThread().getId() + ") requesting "
						+ TRANSACTIONS_NUM + " transactions");

				// request TRANSACTIONS_NUM transactions from the server
				for (int i = 0; i < TRANSACTIONS_NUM; i++) {
					// generate random source and destination account numbers
					Random rand = new Random();
					int srcAccountNumber = 0;
					int dstAccountNumber = 0;
					if (i % 10 != 0) {
						srcAccountNumber = ACCOUNT_NUMBERS[rand
								.nextInt(ACCOUNT_NUMBERS.length)];
						dstAccountNumber = ACCOUNT_NUMBERS[rand
								.nextInt(ACCOUNT_NUMBERS.length)];
					}
					// ask the server to transfer $1 from source to destination
					// account
					String line = "1 " + srcAccountNumber + " "
							+ dstAccountNumber;
					ps.println(line);
					System.out.println("SENT: " + line);

					// read back the server's response and print it
					line = br.readLine();
					System.out.println("RECEIVED: " + line);
				}

				// close the print stream (and the socket, implicitly)
				ps.close();
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Client worker thread (thread id="
					+ Thread.currentThread().getId() + ") finished");
		}
	}

	/**
	 * Shutdown Tells the bank server to shut itself down gracefully.
	 * 
	 * @author Ian Mundy
	 * 
	 */
	private static class ShutDownThread extends Thread {
		public void run() {
			try {
				Socket socket = new Socket(SERVER_ADDRESS, PORT);
				System.out.println("Shutdown worker thread (thread id="
						+ Thread.currentThread().getId()
						+ ") connected to server");

				PrintStream ps = new PrintStream(socket.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				ps.println("SHUTDOWN");
				String line;
				line = br.readLine();

				int total = Integer.parseInt(line);

				System.out.println("TOTAL IS: " + total);

				ps.close();
				socket.close();

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
