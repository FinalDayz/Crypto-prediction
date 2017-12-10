import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Data {
	
	
	double[][] readData(String fileName) throws IOException{
		String[] dataLines = new String(Files.readAllBytes(Paths.get(Main.fileDir + fileName))).split("\r\n");
		double[][] data = new double[dataLines.length][];
		
		for(int i = 0; i < dataLines.length; i++){
			String[] values = dataLines[i].split(",");
			data[i] = new double[values.length];
			
			for(int j = 0; j < data[i].length; j++){
				data[i][j] = Double.parseDouble(values[j]);
			}
		}
		
		return data;
	}
	
	
		/* 
		Original format text file:
			bittrex_8BIT_BTC : b/8BIT_BTC,
			bittrex_ABY_BTC : b/ABY_BTC,
			bittrex_ADC_BTC : b/ADC_BTC,
		we want:
			bittrex_8BIT_BTC
			bittrex_ABY_BTC
			bittrex_ADC_BTC
		 */
	
	String[] getCoins(String fileName) throws IOException{
		String[] coinLines = new String(Files.readAllBytes(Paths.get(Main.fileDir + fileName))).split("\r\n");
		for(int i = 0; i < coinLines.length; i++){
			String thisLine = coinLines[i];
			coinLines[i] = thisLine.substring(0, thisLine.indexOf(":")).replace(" ", "");
		}
		return coinLines;
	}
	
	//send for every coin a request to a website to get the history of that coin
	//you are getting a lot more data in the post request that we need, so it will get filtered out
	void updateAndWriteData(String[] coins, String fileName) throws Exception{

		String beginResponsArray = "google.visualization.arrayToDataTable(";
		String endResponsArray = "]);varoptions={";
		double[][] coinData = new double[coins.length][];
		boolean[] disqualified = new boolean[coinData.length];
		
		for(int i = 0; i < coins.length; i++){
			String coinLabel = coins[i];
			
			print(" ("+i+"/"+coins.length+") Requesting "+coinLabel+"... ");
			
			String respons = sendPost("http://www.alt19.com/19/alt1.php", "source='bittrex&label=" + coinLabel + "&period=1d&presence=chart&submit=OK");
			respons = respons.replace(" ", "").replace("\t", "");
			
			respons = respons.substring(respons.lastIndexOf(beginResponsArray) + beginResponsArray.length(), respons.lastIndexOf(endResponsArray));
			respons = respons.replace("\n", "").replace("\r\n", "").replace("\r", "").replace("[['Period',right_title_name],[", "");
			respons = respons.substring(0, respons.length() - 2);

			String[] dayValues = respons.split("\\],\\[");

			//fill in coinData
			int zeroValues = 0;
			coinData[i] = new double[dayValues.length];
			
			if(dayValues.length < 30)
				disqualified[i] = true;
			
			for(int j = 0; j < dayValues.length; j++){
				coinData[i][j] = Double.parseDouble(dayValues[j].split(",")[1]);
				//we don't want zero's (zèro's if you know what i mean :) (Z))
				if(coinData[i][j] == 0){
					zeroValues++;
					if(j == 0){
						double notZero = 0;
						for(int k = 0; k < dayValues.length; k++)
							if(Double.parseDouble(dayValues[k].split(",")[1]) != 0)
								notZero = Double.parseDouble(dayValues[k].split(",")[1]);
						
						coinData[i][j] = notZero;
					} else
						coinData[i][j] = coinData[i][j - 1];
				}
				//don't add this coin, because it is probably useless
				if(zeroValues > 5){
					disqualified[i] = true;
				}
				if(disqualified[i])
					break;
			}
			
			if(!disqualified[i])
				println("done");
			else
				if(dayValues.length < 30)
					println("done, but disqualified because the coin has got too few values (days that is is 'living')");
				else
					println("done, but disqualified because it is probably bankrupt");
			
		}
		/*write data to a file
		format:
		0.1, 0.12, 0.14, 0.09
		0.6, 0.7, 0.72, 0.8
		*/
		println("Collected all data, writing file...");
		   Writer writer = null;			   					
		   writer = new BufferedWriter(new OutputStreamWriter(
		     						
		     new FileOutputStream(Main.fileDir + fileName), "utf-8"));

		  		for(int i = 0; i < coinData.length; i++){
		  			if(!disqualified[i]){
			  			if(i != 0)
			  				((BufferedWriter) writer).newLine();
			  			
			  			for(int j = 0; j < coinData[i].length; j++){
			  				if(j != 0)
			  					writer.write(",");
			  				writer.write(Double.toString(coinData[i][j]));
				  		}
		  			}
		  		}
		     
		  writer.close();
		  println("Done writine file "+fileName+" to dir "+Main.fileDir);
	}
	
	
	double[] getCoinData(String coinName) throws Exception{
		double[] coinData;
		
		String beginResponsArray = "google.visualization.arrayToDataTable(";
		String endResponsArray = "]);varoptions={";
		
		String respons = sendPost("http://www.alt19.com/19/alt1.php", "source='bittrex&label=" + coinName + "&period=1d&presence=chart&submit=OK");
		respons = respons.replace(" ", "").replace("\t", "");
		
		respons = respons.substring(respons.lastIndexOf(beginResponsArray) + beginResponsArray.length(), respons.lastIndexOf(endResponsArray));
		respons = respons.replace("\n", "").replace("\r\n", "").replace("\r", "").replace("[['Period',right_title_name],[", "");
		respons = respons.substring(0, respons.length() - 2);

		String[] dayValues = respons.split("\\],\\[");

		//fill in coinData
		int zeroValues = 0;
		coinData = new double[dayValues.length];
		
		
		for(int j = 0; j < dayValues.length; j++){
			coinData[j] = Double.parseDouble(dayValues[j].split(",")[1]);
			//we don't want zero's (zèro's if you know what i mean :) (Z))
			if(coinData[j] == 0){
				zeroValues++;
				if(j == 0){
					double notZero = 0;
					for(int k = 0; k < dayValues.length; k++)
						if(Double.parseDouble(dayValues[k].split(",")[1]) != 0)
							notZero = Double.parseDouble(dayValues[k].split(",")[1]);
					
					coinData[j] = notZero;
				} else
					coinData[j] = coinData[j - 1];
			}
		}
		
		return coinData;
	}
	
	String sendPost(String url, String parameters) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(parameters);
		wr.flush();
		wr.close();

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();

	}
	
	void println(Object o){
		System.out.println(o);
	}
	
	void print(Object o){
		System.out.print(o);
	}
}