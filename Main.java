import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;




public class Main {

	static ArrayList<ServerDetails> allServers =new ArrayList<>();
	static int[][] routingTableReadFromTopologyFile;
	static int updateInterval=1000,myServerId=1,countOfDisabledServers=0;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello");



		Scanner stdinScanner = new Scanner(System.in);
		while (true) {
			System.out.print(">>");

			String line = stdinScanner.nextLine();
			String[] splitLine = line.split(" ");

			if (splitLine.length < 1) {
				System.out.print("Incorrect Command ");
				continue;
			}

			switch (splitLine[0]) {
			case "server":

				try {
					updateInterval = Integer.parseInt(splitLine[4]);
				} catch (Exception e) {
					System.out.println("Server Command Incorrect");
					continue;
				}
				allServers = readTopologyFile(splitLine[2], allServers);
				//allServers = readTopologyFile("/home/krishna/workspaces/eclipse/DVP/src/topology.txt", allServers);

				allServers = createRoutingTable(allServers);
				
				//Main mainObj = new Main();
				
				//Timer time = new Timer(); // Instantiate Timer Object
        		//ScheduledTask st = mainObj.new ScheduledTask(); // Instantiate SheduledTask class
        		//time.schedule(st,60000, updateInterval); // Create Repetitively task for every 1 secs

				routingTableReadFromTopologyFile = new int[allServers.size()+countOfDisabledServers][allServers.size()+countOfDisabledServers];
				for(int i=0;i<allServers.size();i++) {
					if(allServers.get(i).id == myServerId) {
						for(int s=0;s<allServers.get(i).routingTable.length;s++) {
							for(int t=0;t<allServers.get(i).routingTable[s].length;t++) {
								routingTableReadFromTopologyFile[s][t] = allServers.get(i).routingTable[s][t];
							}
						}
						break;
					}
				}

				/*
				 * // Fire off the server listening thread ip = InetAddress.getLocalHost();
				 * Runnable server = new Server(localServerInfo.Port); new
				 * Thread(server).start();
				 */

				/*
				 * // Fire off the timer that sends a update every 10 seconds Timer timer = new
				 * Timer(true); UpdateTimerTask updateTimertask = new UpdateTimerTask();
				 * timer.scheduleAtFixedRate(updateTimertask, updateDelay, updateDelay);
				 */

				System.out.println(splitLine[0] + " SUCCESS");
				//sendRoutingTableToNeighbor("send", "192.168.0.44", 8200);
				break;
			case "help":
				System.out.println(line + " SUCCESS");
				System.out.println("\nList of Commands supported:" + "\n>> help"
						+ "\n>> update <server id 1> <server id 2> <link cost>" + "\n>> step" + "\n>> packets"
						+ "\n>> display" + "\n>> disable <server id>" + "\n>> crash\n");
				break;
			case "update":
				int linkServer1 = Integer.parseInt(splitLine[1]);
				int linkServer2 = Integer.parseInt(splitLine[2]);
				String newCostOfLink =  splitLine[3];
				if(linkServer1 == linkServer2)
				{
					System.out.println("Enter command correctly");
					break;
				}
				else if(linkServer2 == myServerId)
				{
					sendUpdateLinkCostToNeighbor(linkServer2,linkServer1,newCostOfLink);
					break;
				}
				else
				{
					sendUpdateLinkCostToNeighbor(linkServer1,linkServer2,newCostOfLink);
					break;
				}
			case "step":
				doStep(allServers);
				//sendRoutingTableToNeighbor("step","192.168.0.44", 6666);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "packets":
				displayPackets(allServers);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "display":
				displayRoutingTable(allServers);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "disable":
				//send this to all servers, not just neighbors
				if(Integer.parseInt(splitLine[1])==(myServerId))
				{
					System.out.println("Can not Disable yourself");
					break;
				}
				sendDisableToAllServers(Integer.parseInt(splitLine[1]));
				countOfDisabledServers++;
				break;
			case "crash":
				sendCrashToAllServers();
				System.out.println(splitLine[0] + " SUCCESS");
				System.exit(1);
				break;
			default:
				break;
			}

		}
	}


	private static void sendUpdateLinkCostToNeighbor(int linkServer1, int linkServer2, String newCostOfLink) {
		// TODO Auto-generated method stub


		if(newCostOfLink.equalsIgnoreCase("inf")) {
			routingTableReadFromTopologyFile[linkServer1-1][linkServer2-1] = 9999;
		}
		else {
			routingTableReadFromTopologyFile[linkServer1-1][linkServer2-1] = Integer.parseInt(newCostOfLink);
		}


		for(int x=0;x<allServers.size();x++) {
			if(allServers.get(x).id == myServerId) {

				for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
					for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
						allServers.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
					}
				}
				break;
			}
		}

		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "update");
			obj.put("update_server_id_1", linkServer1);
			obj.put("update_server_id_2", linkServer2);
			obj.put("cost", newCostOfLink);

		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<allServers.size();i++) {

				///System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(allServers.get(i).ipAddress);
				Socket socket = new Socket(ip, allServers.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
		
		doStep(allServers);
	}


	private static void sendCrashToAllServers() {
		// TODO Auto-generated method stub
		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "crash");
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<allServers.size();i++) {

				if(allServers.get(i).id == myServerId) {
					continue;
				}

				//System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(allServers.get(i).ipAddress);
				Socket socket = new Socket(ip, allServers.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
	}


	private static void sendDisableToAllServers(int dsid) {

		for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
			for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
				if(j == (dsid-1)) {
					continue;
				}
				routingTableReadFromTopologyFile[j][dsid-1] = 9999;
				routingTableReadFromTopologyFile[dsid-1][j] = 9999;
			}
		}
		for(int x=0;x<allServers.size();x++) {
			if(allServers.get(x).id == myServerId) {
				allServers.get(x).neighborsIdAndCost.remove(dsid);

				for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
					for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
						allServers.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
					}
				}
				break;
			}
		}


		// TODO Auto-generated method stub
		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "disable");
			obj.put("disable_server_id", dsid);
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<allServers.size();i++) {
				
				if(allServers.get(i).id == myServerId) {
					continue;
				}

				//System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(allServers.get(i).ipAddress);
				Socket socket = new Socket(ip, allServers.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
		allServers.remove(dsid-1);
		doStep(allServers);	
	}


	private static void sendRoutingTableToNeighbor(String ipAddressOfNeighbor, int portOfNeighbor) {
		// TODO Auto-generated method stub

		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "step");
			obj.put("id_of_sender", myServerId);
			for(int i=0;i<allServers.size();i++) {
				//System.out.println("server id ="+allServers.get(i).id);
				if(allServers.get(i).id == myServerId) {
					//System.out.print("matched = "+allServers.get(i).routingTable);
					obj.put("rt", allServers.get(i).routingTable);
					break;
				}
			}
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			//System.out.println("inside Try");
			InetAddress ip=InetAddress.getByName(ipAddressOfNeighbor);
			Socket socket = new Socket(ip, portOfNeighbor);
			//System.out.println("socket Created");
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			//System.out.println("inside Send, dataSteam Created");

			// transfer JSONObject as String to the server
			//dataOutputStream.writeUTF(stringJsonData);
			dataOutputStream.writeUTF(obj.toString());

			//System.out.println("inside Send, wrote Data to neighbor");
			socket.close();
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
	}



	private static void displayPackets(ArrayList<ServerDetails> allServers) {
		for (int i = 0; i < allServers.size(); i++) {
			if (allServers.get(i).id == myServerId) {
				System.out.println(allServers.get(i).noOfPacketsReceived);
			}
		}
	}

	private static void displayRoutingTable(ArrayList<ServerDetails> allServers) {
		System.out.println("Routing Table is");
		for (int i = 0; i < allServers.size(); i++) {
			
			if (allServers.get(i).id == myServerId) {
				for (int j = 0; j < allServers.get(i).routingTable.length; j++) {
					for (int k = 0; k < allServers.get(i).routingTable[j].length; k++) {
						System.out.print(allServers.get(i).routingTable[j][k] + "\t");
					}
					System.out.print("\n");
				}
				break;
			}
		}
	}

	private static ArrayList<ServerDetails> createRoutingTable(ArrayList<ServerDetails> allServers) {
		// fetch the server you need
		// assign routing table to the
		for (int i = 0; i < allServers.size(); i++) {
			allServers.get(i).routingTable = new int[allServers.size()+countOfDisabledServers][allServers.size()+countOfDisabledServers];
			if (allServers.get(i).id == myServerId) {

				for (int j = 0; j < allServers.get(i).routingTable.length; j++) {
					for (int k = 0; k < allServers.get(i).routingTable[j].length; k++) {
						if (j == k) {
							allServers.get(i).routingTable[j][k] = 0;
						} else {
							allServers.get(i).routingTable[j][k] = 9999;
						}
					}
				}
			} else {
				for (int j = 0; j < allServers.get(i).routingTable.length; j++) {
					for (int k = 0; k < allServers.get(i).routingTable[j].length; k++) {
						allServers.get(i).routingTable[j][k] = 9999;
					}
				}
			}
		}

		for (int i = 0; i < allServers.size(); i++) {
			if (allServers.get(i).id == myServerId) {
				for (int j = 0; j < allServers.get(i).routingTable.length; j++) {
					if (j + 1 == myServerId) {
						Iterator<Map.Entry<Integer, Integer>> itr = allServers.get(i).neighborsIdAndCost.entrySet()
								.iterator();
						while (itr.hasNext()) {
							Map.Entry<Integer, Integer> entry = itr.next();
							allServers.get(i).routingTable[j][entry.getKey() - 1] = entry.getValue();
						}
						break;
					}
				}
				break;
			}
		}
		/*for (int i = 0; i < allServers.size(); i++) {
			System.out.println("Routing Table is");
			if (allServers.get(i).id == myServerId) {
				for (int j = 0; j < allServers.get(i).routingTable.length; j++) {
					for (int k = 0; k < allServers.get(i).routingTable[j].length; k++) {
						System.out.print(allServers.get(i).routingTable[j][k] + " ");
					}
					System.out.print("\n");
				}
				break;
			}
		}*/

		return allServers;
	}

	private static ArrayList<ServerDetails> readTopologyFile(String fileName, ArrayList<ServerDetails> allServers) {

		//System.out.println(fileName);
		int totalServersCount = 0;
		int totalNeighborsCount = 0;

		HashMap<Integer, Integer> newNeighborIdAndCost = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line;
			if ((line = br.readLine()) != null) {
				totalServersCount = Integer.parseInt(line);
			} else {
				throw new Exception("Topology File Not Correctly Formatted!");
			}
			if ((line = br.readLine()) != null) {
				totalNeighborsCount = Integer.parseInt(line);
			} else {
				throw new Exception("Topology File Not Correctly Formatted!");
			}
			int i;
			String[] splitLine;
			for (i = 0; i < totalServersCount; i++) {
				if ((line = br.readLine()) != null) {
					splitLine = line.split(" ");
					if (splitLine.length != 3) {
						throw new Exception("Topology File Not Correctly Formatted!");
					} else {
						ServerDetails newServer = new ServerDetails();
						newServer.setId(Integer.parseInt(splitLine[0]));
						newServer.setIpAddress(splitLine[1]);
						newServer.setPort(Integer.parseInt(splitLine[2]));
						newServer.setNoOfPacketsReceived(0);
						allServers.add(newServer);
						if(newServer.id == myServerId) {  
							Main mainObj = new Main();
							Thread listenerThread = new Thread(mainObj.new Listener(newServer.port));
							listenerThread.start();
						}
					}
				} else {
					throw new Exception("Topology File Not Correctly Formatted!");
				}
			}

			for (i = 0; i < totalNeighborsCount; i++) {
				if ((line = br.readLine()) != null) {
					splitLine = line.split(" ");
					if (splitLine.length != 3) {
						throw new Exception("Topology File Not Correctly Formatted!");
					} else {
						myServerId = Integer.parseInt(splitLine[0]);
						newNeighborIdAndCost.put(Integer.parseInt(splitLine[1]), Integer.parseInt(splitLine[2]));
					}
				} else {
					throw new Exception("Topology File Not Correctly Formatted!");
				}
			}
			//System.out.println("in try" + newNeighborIdAndCost);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			// System.exit(1);
		}
		//System.out.println("aftre catch" + newNeighborIdAndCost);
		for (int i = 0; i < allServers.size(); i++) {
			if (allServers.get(i).getId() == myServerId) {
				allServers.get(i).neighborsIdAndCost = newNeighborIdAndCost;
			} else {
				HashMap<Integer, Integer> emptyHashMap = new HashMap<>();
				emptyHashMap.put(0, 0);
				allServers.get(i).neighborsIdAndCost = emptyHashMap;
			}
		}
		//System.out.println("out of readTopology File");

		return allServers;
	}

	private static void doStep(ArrayList<ServerDetails> allServers) {

		for (int i = 0; i < allServers.size(); i++) {
			if (allServers.get(i).id == myServerId) {
				//System.out.println("my servers id = "+allServers.get(i).id);

				Iterator<Map.Entry<Integer, Integer>> itr = allServers.get(i).neighborsIdAndCost.entrySet().iterator();
				while (itr.hasNext()) {
					String ipAddressOfNeighbor = "";
					int portOfNeighbor = 0;
					Map.Entry<Integer, Integer> entry = itr.next();

					//System.out.println("neighbor = "+entry.getKey());
					//System.out.println("value = "+entry.getValue());
					// find ip of neighbor and send routing table to that neighbor
					for (int k = 0; k < allServers.size(); k++) {
						if (allServers.get(k).id == entry.getKey()) {
							ipAddressOfNeighbor = allServers.get(k).ipAddress;
							portOfNeighbor = allServers.get(k).port;
							break;
						}
					}
					//System.out.println("ipaddress of neighbor = " + ipAddressOfNeighbor);
					//System.out.println("port of neighbor = " + portOfNeighbor);
					try {
						//System.out.println("send message");
						sendRoutingTableToNeighbor(ipAddressOfNeighbor, portOfNeighbor);
						//System.out.println("message sent");
					} catch (Exception e) {

					}
				}
				break;
			}
		}
	}

	private static ArrayList<ServerDetails> updateRoutingTable(ArrayList<ServerDetails> allServers, int[][] nrt) {

		/*displayRoutingTable(allServers);
		System.out.println("inside update Routing Table, Table = ");
		for (int s = 0; s < nrt.length; s++) {
			for (int t = 0; t < nrt[s].length; t++) {
				System.out.print(nrt[s][t]);
			}
			System.out.println();
		}*/


		int[][] myOriginalRoutingTable = new int[allServers.size()+countOfDisabledServers][allServers.size()+countOfDisabledServers];
		int[][] myNewRoutingTable = new int[allServers.size()+countOfDisabledServers][allServers.size()+countOfDisabledServers];

		/*System.out.println("2nd time = my Orginial Routing Table = ");
		System.out.println("my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/



		int i = 0;
		for (i = 0; i < allServers.size(); i++) {
			if (allServers.get(i).getId() == myServerId) {

				for(int a=0;a<allServers.get(i).routingTable.length;a++) {
					for(int b=0;b<allServers.get(i).routingTable[a].length;b++) {
						myOriginalRoutingTable[a][b]=allServers.get(i).routingTable[a][b];
						myNewRoutingTable[a][b]=allServers.get(i).routingTable[a][b];
					}
				}
				break;
			}
		}

		/*System.out.println("3rd time = my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/



		int[] neighbors = new int[allServers.get(i).neighborsIdAndCost.size()];
		Iterator<Map.Entry<Integer, Integer>> itr = allServers.get(i).neighborsIdAndCost.entrySet().iterator();
		int x = 0;
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			neighbors[x] = entry.getKey();
			x++;
		}
		//System.out.println(
		//		"No. of Neighbors of server " + myServerId + " = " + allServers.get(i).neighborsIdAndCost.size());
		for (int j = 0; j < myNewRoutingTable.length; j++) {
			if (j + 1 == myServerId) {

			} else {
				for (int k = 0; k < myNewRoutingTable[j].length; k++) {
					if (j == k) {

					} else {
						if (myNewRoutingTable[j][k] < nrt[j][k]) {

						} else {
							myNewRoutingTable[j][k] = nrt[j][k];
						}
					}
				}
			}
		}

		/*System.out.println("4th time = my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/

		for (int j = 0; j < myNewRoutingTable.length; j++) {
			if (j + 1 == Main.myServerId) {
				// update routing table
				for (int k = 0; k < myNewRoutingTable[j].length; k++) {
					if (j == k) {

					} else {
						// this array stores all the distance to a partiular server
						int newCosts[] = new int[allServers.get(i).neighborsIdAndCost.size()];
						// calculate all new costs to the server
						for (int a = 0; a < neighbors.length; a++) {

							newCosts[a] = myNewRoutingTable[j][neighbors[a] - 1]
									+ myNewRoutingTable[neighbors[a] - 1][k];
						}
						// find the minimum cost from the array
						int minCost = 9999;
						for (int a = 0; a < newCosts.length; a++) {
							if (minCost > newCosts[a]) {
								minCost = newCosts[a];
							}
						}
						myNewRoutingTable[j][k] = minCost;
					}
				}
			}
		}
		/*System.out.println("5th time, my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/
		// check if your routing table has changed
		Boolean didRoutingTableChange = false;
		for (int s = 0; s < allServers.get(i).routingTable.length; s++) {
			for (int t = 0; t < allServers.get(i).routingTable[s].length; t++) {
				if (myNewRoutingTable[s][t] != myOriginalRoutingTable[s][t]) {
					didRoutingTableChange = true;
					break;
				}
			}
		}
		//System.out.println("did Routing Table change = "+didRoutingTableChange);
		//System.out.println("New Routing Table = "+myNewRoutingTable);
		if (didRoutingTableChange) {
			allServers.get(i).routingTable = myNewRoutingTable;
			// send routing table to neighbors
			doStep(allServers);

		}
		//allServers.get(i).noOfPacketsReceived++;
		// displayRoutingTable(allServers);
		return allServers;
	}

	class Listener implements Runnable{
		// initialize socket and input stream
		private Socket socket = null;
		private ServerSocket server = null;
		private DataInputStream in = null;
		private int port = 0;

		Listener(int port){
			this.port=port;
		}

		public void run() {
			try {
				//System.out.println("port ="+port);
				server = new ServerSocket(port);
				//System.out.println("Server started");
				//System.out.println("Waiting for a client ...");
				while(true){
					socket = server.accept();
					//System.out.println("Client accepted");

					// takes input from the client socket
					in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

					String line = in.readUTF().toString();
					//System.out.println(line);
					JSONObject receivedJSON = new JSONObject(line);
					//JSONObject receivedJSON =  line.;
					switch(receivedJSON.get("operation").toString()) {
					case "step":
						System.out.println("Received a Message From Server "+receivedJSON.getInt("id_of_sender"));

						int[][] nrt =new int[allServers.size()+countOfDisabledServers][allServers.size()+countOfDisabledServers];
						//System.out.println("inside step");
						JSONArray jsonArray = receivedJSON.getJSONArray("rt"); 
						//System.out.println("inside step");
						//nrt = (int[][]) receivedJSON.get("rt");
						for(int a=0;a<jsonArray.length();a++) {
							JSONArray innerJsonArray = (JSONArray) jsonArray.get(a);
							//System.out.println(innerJsonArray);
							for(int b=0;b<innerJsonArray.length();b++) {
								nrt[a][b]=Integer.parseInt(innerJsonArray.get(b).toString());
							}
						}
						//System.exit(1);
						//System.out.println("inside step");
						//also update the no.of packets received by this server
						for(int i=0;i<allServers.size();i++) {
							if(allServers.get(i).id == myServerId) {
								//System.out.println(allServers.get(i).noOfPacketsReceived);
								allServers.get(i).noOfPacketsReceived++;
								//System.out.println(allServers.get(i).noOfPacketsReceived);
								break;
							}
						}
						allServers = updateRoutingTable(allServers, nrt);
						break;
					case "update":
						String newCost = receivedJSON.get("cost").toString();
						int server1 = Integer.parseInt(receivedJSON.get("update_server_id_1").toString());
						int server2 = Integer.parseInt(receivedJSON.get("update_server_id_2").toString());

						if(newCost.equalsIgnoreCase("inf")) {
							routingTableReadFromTopologyFile[server2-1][server1-1] = 9999;
						}
						else {
							routingTableReadFromTopologyFile[server2-1][server1-1] = Integer.parseInt(newCost);
						}
						for(int x=0;x<allServers.size();x++) {
							if(allServers.get(x).id == myServerId) {

								for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
									for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
										allServers.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
									}
								}
								break;
							}
						}

						break;
					case "disable":

						int disable_server_id = Integer.parseInt(receivedJSON.get("disable_server_id").toString());

						if(disable_server_id == myServerId) {
							System.exit(1);
						}

						for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
							for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
								if(j == (disable_server_id-1)) {
									continue;
								}
								routingTableReadFromTopologyFile[j][disable_server_id-1] = 9999;
								routingTableReadFromTopologyFile[disable_server_id-1][j] = 9999;
							}
						}
						for(int x=0;x<allServers.size();x++) {
							if(allServers.get(x).id == myServerId) {
								allServers.get(x).neighborsIdAndCost.remove(disable_server_id);

								for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
									for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
										allServers.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
									}
								}
								break;
							}
						}
						/*
							for(int i=0;i<allServers.size();i++) {
								if(allServers.get(i).id == myServerId) {
									allServers.get(i).neighborsIdAndCost.remove(disable_server_id);
									//Do I make changes to routing table?
									for(int j=0;j<allServers.get(i).routingTable.length;j++) {
										if(j == (disable_server_id-1)) {
											continue;
										}
										allServers.get(i).routingTable[j][disable_server_id-1] = 9999;
										allServers.get(i).routingTable[disable_server_id-1][j] = 9999;
									}
									break;
								}
							}*/

						allServers.remove(disable_server_id-1);
						countOfDisabledServers++;
						break;
					case "crash":
						System.out.println("CRASH SUCCESSFUL");
						System.exit(1);
						break;
					}
				}
			} catch (Exception i) {
				//System.out.println("inside outer catch");
				System.out.println(i);
			}
		}
	}
	class ScheduledTask extends TimerTask {

		//Date now; // to display current time

		// Add your task here
		public void run() {
			//now = new Date(); // initialize date
			//System.out.println("Time is :" + now); // Display current time
			doStep(allServers);
		}
	}
}

class ServerDetails {
	int id;
	String ipAddress;
	int port;
	int noOfPacketsReceived;
	HashMap<Integer, Integer> neighborsIdAndCost;
	int[][] routingTable;

	ServerDetails() {
		this.neighborsIdAndCost = new HashMap<>();
		this.noOfPacketsReceived = 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public HashMap<Integer, Integer> getNeighborsIdAndCost() {
		return neighborsIdAndCost;
	}

	public void setNeighborsIdAndCost(HashMap<Integer, Integer> neighborsIdAndCost) {
		this.neighborsIdAndCost = neighborsIdAndCost;
	}

	public int getNoOfPacketsReceived() {
		return noOfPacketsReceived;
	}

	public void setNoOfPacketsReceived(int noOfPacketsReceived) {
		this.noOfPacketsReceived = noOfPacketsReceived;
	}

	public int[][] getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(int[][] routingTable) {
		this.routingTable = routingTable;
	}

}

class Message{
	String operation;
	HashMap<Integer, Integer> link=new HashMap<>();
	int[][] rt;
}