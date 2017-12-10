import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
	
	static double LEARNING_RATE = 0.0001;
	static int MIN_DAYS = 30; // min 30 days of inputs before giving a prediction
	
	static String fileDir = "C:\\Users\\stee2\\Desktop\\Self programmed A.I\\Bitcoin predictor\\";
	
	static String[] coins = {};
	//array of how much percent the price has changed (compared to the previous day)
	static double[][] data;
	
	static boolean UPDATE_DATA_FILE = false; //download all the coin states (takes about 3 mins)
	static Data dataManager;
	
	static NeuralNetwork NN;
	
    static public NeuralNetwork testRNN = null;
	static String helpMenu = "";
    
    
    //for every coin keep 10 days to test 
    //(don't train the last 10 days do it can be tested if the  network can predict it successfully without it seeing it before)
    static int keepDataForTest = 10;
    
	//static String coinDataFileName = "coinData_test.txt";
	static String coinDataFileName = "coinData.txt";
	public static void main(String[] arg) throws InterruptedException{
		dataManager = new Data();
		
		try {
			coins = dataManager.getCoins("coin_list.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//get data in 2 dimentional double array
		try {
			if(UPDATE_DATA_FILE)
				dataManager.updateAndWriteData(coins, coinDataFileName);
			data = dataManager.readData(coinDataFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		// input, hidden layers and output of the network
		int[] layers = { 1,  10, 40, 80, 180, 250, 180, 80, 40, 10,  1 };
		
		initializeNetwork(layers);
		
		
	       testRNN = NN.copy();
	       
	       Thread one = new Thread() {
	        public void run() {
	            try {
					executeNetworkWithInput();
				} catch (Exception e) {
					e.printStackTrace();
				}
	           }
	       };
	       one.start();

		for(int X = 0; X < 10000; X++){
			double totalError = 0;
			int totalErrorTimes = 0;
			
			for(int i = 0; i < data.length; i++){
				NN.resetNetwork();
				
				double error = 0;
				int errorTimes = 0;
				
				//each value (day) of the coin
				for(int j = 0; j < data[i].length - (1 + keepDataForTest) ; j++){
					double prefValue = 0;
					double thisValue = data[i][j];
					double nextValue = data[i][j + 1];
					
					if(j != 0)
						prefValue = data[i][j - 1];
					
					//set inputs
					double percentFromPrevious = 0;
					if(j != 0)
						percentFromPrevious = percent(prefValue, thisValue);
					
					double expected = percent(thisValue, nextValue);
					double[] networkOut = NN.FeedForward(new double[]{percentFromPrevious});
					

					//only train if it has already 'seen' 30 days in the past
					if(j > MIN_DAYS){
						double thisError = NN.BackProp(new double[]{expected}, true);
						error += thisError;
						errorTimes++;
					}

					
				}// </day>
				
				totalError += error/errorTimes;
				totalErrorTimes++;
				
			}// </coin>
			LEARNING_RATE *= 1 + LEARNING_RATE;
			testRNN = NN.copy();
			println("-----FINISHED "+X+" itteration. average itteration error: "+toRead(totalError/totalErrorTimes)+" (LEARNING_RATE) "+normalNotation(LEARNING_RATE));
		}// </loop>
	}
	
	
	 static void executeNetworkWithInput() throws Exception{
         Scanner keyboard = new Scanner(System.in);
         helpMenu = "You can input a coin to see the predicted results ex. 'LTC': 'bittrex_LTC_BTC'\n";
         helpMenu += "The commands:\n";
         helpMenu += "test [coin] (test a coin)\n";
         helpMenu += "set [variable] [value]\n";
         	helpMenu += "\tlearning_rate, current: "+normalNotation(LEARNING_RATE)+" (means the learning rate of the network)\n";
         	helpMenu += "\tmin_days, current: "+MIN_DAYS+" (the number of days it needs bevore giving a prediction)\n";
         	helpMenu += "\tkeepDataForTest, current: "+keepDataForTest+" (dont train the last X days to the network for testing)\n";
         helpMenu += "help (this menu)\n";
         
         print(helpMenu);
         
	     while(true){
	         System.out.println("Command, or coin name to predict(not all coins are available) ");
	         String command = keyboard.nextLine();
	         
	         try {
	        	 passCommand(command);
	         } catch(Exception e) {
	        	 e.printStackTrace();
	         }
	         
	     }
  }
	 
	 static void passCommand(String command) throws Exception{
		 boolean didEnterCommand = false;
		 boolean test = false;
		 String coin = command;
		 command = command.toLowerCase();
		 
		 if(command.indexOf("test ") == 0){
			 coin = coin.substring("test ".length());
			 println("COI NGJNKDSGN "+coin);
			 test = true;
		 }
		 
		 if(command.indexOf("set ") == 0){
			 String variableType = command.replace("set ", "");
			 
			 if(variableType.indexOf("learning_rate ") == 0){
				 String value = variableType.replace("learning_rate ", "");
				 double new_learning_rate = Double.parseDouble(value);
				 println("New learning rate: "+normalNotation(new_learning_rate)+" Old: "+normalNotation(LEARNING_RATE));
				 LEARNING_RATE = new_learning_rate;
			 }
			 
			 if(variableType.indexOf("keepDataForTest ") == 0){
				 String value = variableType.replace("keepDataForTest ", "");
				 int new_min_days = Integer.parseInt(value);
				 println("New days to keep data: "+new_min_days+" Old: "+MIN_DAYS);
				 MIN_DAYS = new_min_days;
			 }
			 
			 
			 didEnterCommand = true;
		 }
		 
		 if(command.equals("help")){
			 print(helpMenu);
		 }
		 
		 if(!didEnterCommand){
			 
			double[] data = dataManager.getCoinData(coin);
	         
	         testRNN.resetNetwork();
	         //each value (day) of the coin
	         double networkOutput = 0;
	         int endIndex = data.length;
	         if(test)
	        	 endIndex -= 5;
	         
	         for(int j = 0; j < endIndex; j++){
	        	 double prefValue = 0;
	        	 double thisValue = data[j];
				
	        	 if(j != 0)
	        		 prefValue = data[j - 1];
				
	        	 //set inputs
	        	 double percentFromPrevious = 0;
	        	 if(j != 0)
	        		 percentFromPrevious = percent(prefValue, thisValue);
	
	        	 networkOutput = testRNN.FeedForward(new double[]{percentFromPrevious})[0];
	         }// </day>
	         
	         println("Predictions network (01:00):");
	         for(int i = 0; i < 5; i++){
	        	 if(test){
	        		 double actualRate = percent(data[data.length - 1 - (4 - i)] , data[data.length - 1- (4 - i) - 1]);
	        		 println("+"+(i + 1)+" day: "+toRead(toRead(networkOutput) * 100)+"% But actual rate: "+toRead(toRead(actualRate) * 100)+"% (index:"+(data.length - 1- (4 - i) - 1)+", data.length "+data.length+") coin rate "+data[data.length - 1 - (4 - i)]+" previous rate "+(data[data.length - 1 - (4 - i) - 1]));
	        	 } else
	        		 println("+"+(i + 1)+" day: "+toRead(toRead(networkOutput) * 100)+"%");
	        	 networkOutput = testRNN.FeedForward(new double[]{networkOutput})[0];
	         }
	         
		 }
		 
	 }
	 
	 static String normalNotation(Double number){
		NumberFormat formatter = new DecimalFormat("###.#####");
		
		return formatter.format(number);
	 }
	
	static double toRead(double x){return Math.round(x * 1000) / 1000.0;}
	
	static double percent(double oldValue, double newValue){
		if(oldValue == 0)
			oldValue = 1E-7;
		if(newValue == 0)
			newValue = 1E-7;
		
		return (newValue - oldValue) / oldValue;
	}
	
	static void initializeNetwork(int[] layers){
		//network inputs and output:
		// 1: percent rise or fall	||	0.1 = 10%	-0.5 = -50%    1 = 100%
		NN = new NeuralNetwork(layers);
	}

	static void println(Object o){
		System.out.println(o);
	}
	static void print(Object o){
		System.out.print(o);
	}
}
