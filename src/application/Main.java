package application;
	
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
/**
* Contains the start method for a JavaFX application.
* As well as the main method.
* The application takes in a URL or file and displays
* statistics about that html file. Such as word count
* and percentage frequency.
* 
* @author Douglas Moore
* 
*/
public class Main extends Application {
	/**
	* Setup Listview which will be modified with error messages
	* as well as word statistics
	* @param listView a ListView of Strings which displays errors or results
	* @param items a ObserableList of Strings to insert into listView
	* 
	*/
	static ListView<String> listView = new ListView<>();
	static ObservableList<String> items = FXCollections.observableArrayList();
	
	/**
	* The start method for the application which sets the stage
	* a fileBtn and submitBtn allow application control.
	* 
	*/
	@Override
	public void start(Stage stage) throws IOException {
		TextField textPath = new TextField();
		textPath.setEditable(false);
		textPath.setMinWidth(380);
		TextField textURL = new TextField();
		textURL.setMinWidth(380);
		Label label = new Label("OR |  Enter URL");
		
		listView.setPrefHeight(400);
		listView.setPrefWidth(400);
		
		Button fileBtn = new Button("Choose a File");
		Button submit = new Button("Submit");
		/**
		* Uses a filechooser to allow the user to select a html file on their system
		* @param path establishes the full path to the html file and replaces \ with /
		* 
		*/
		fileBtn.setOnAction(e -> { // allows a html file to be chosen from drive
			FileChooser fc = new FileChooser();
			File selectedFile = fc.showOpenDialog(null);
			if (selectedFile != null) {
				String path = selectedFile.getAbsolutePath().replace('\\', '/');
				textPath.setText(path);
			}
		});
		
		submit.setOnAction(e -> {
			Document doc = null;
			if (textURL.getText().length() > 0) { //Load from URL
				try {
					doc = Jsoup.connect(textURL.getText()).maxBodySize(0).timeout(0).get();
//					beginList(doc);
					submitResults(doc);
					printDatabase();
				} catch (Exception econnect) {
					econnect.printStackTrace();
				} 
			} else if (textPath.getText().length() > 0) { //Load from .HTML file 
				File input = new File(textPath.getText()); 
				try {
					doc = Jsoup.parse(input, "UTF-8");
//					beginList(doc);
					submitResults(doc);
					printDatabase();
				} catch (Exception edocCreation) {
					edocCreation.printStackTrace();
				} 
			} else { //display an error to the use in window
				items.add("Select a .HTML file or enter a valid URL. Then hit submit.");
				listView.setItems(items);
			}
		});
		
		FlowPane root = new FlowPane(Orientation.HORIZONTAL, 5, 5);
        root.setPadding(new Insets(5));
        root.getChildren().addAll(fileBtn, textPath, label, textURL, submit, listView);
		Scene scene = new Scene(root, 500, 500);
		stage.setTitle("Text Reader");
		stage.setScene(scene);
		stage.show();
	}
	public static void main(String[] args) {
		launch(args);
	}
	
	/**
	* Makes a connection to the mysql database
	* loads a statement using a String query
	* using a while loop retrieves the data using Resultset
	* Then closes the connection
	*/
	public static void printDatabase() throws Exception {
		String url = "jdbc:mysql://localhost:3306/word_occurrences";  
        String uname = "root";  
        String pass = "";  
        String query = "SELECT * FROM word ORDER BY count desc;";
        
		Class.forName("com.mysql.cj.jdbc.Driver");
		Connection con = DriverManager.getConnection(url, uname, pass);
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);
		
		items.add("Word\t\tCount\t\tPercentage");
		while(rs.next()) {
        	items.add(rs.getString(1) + "\t\t\t" + rs.getInt(2) + "\t\t\t" + rs.getDouble(3) + " %");
		}
		listView.setItems(items);
		st.close();
		con.close();
	}
	/**
	* Makes a connection to the mysql database
	* loads a statement using a String query
	* creates a TreeMap and enters the info through a mysql connection
	* Makes use of the PreparedStatement to easily enter values
	* 
	* (  ?,  ?,	 ? )
	* (word, occurrences, frequency)
	* 
	* Then closes the connection
	*/
	public static void submitResults(Document doc) throws Exception {
		String url = "jdbc:mysql://localhost:3306/word_occurrences";  
        String uname = "root";  
        String pass = "";
        String query = "INSERT INTO word VALUES (?, ?, ?)";
        
		Class.forName("com.mysql.cj.jdbc.Driver");
		Connection con = DriverManager.getConnection(url, uname, pass);
		PreparedStatement st = con.prepareStatement(query);
		
		int totalWords = 0;
		int loopSize = 20;
		int rowAffected = 0;
		TreeMap<String, Integer> freqMap = generateFrequencyList(doc);
		for (String key : freqMap.keySet()) { //loop to find how many words were in the file (used for the percentage calculation)
			totalWords += freqMap.get(key);
		}
	    if (freqMap.size() < 20) 
	    	loopSize = freqMap.size();
	    for (int i = 0; i < loopSize; i++) {  //loop to print the top 20 words in a file
			int maxValueInMap = Collections.max(freqMap.values()); //finds the max frequency in the Treemap
			String keyToRemove = ""; //declare a String for key removal after the row has been displayed
			for (Entry<String, Integer> entry : freqMap.entrySet()) {  // Iterates to display a row
		        if (entry.getValue() == maxValueInMap) { //if the current max value has been found 
		            st.setString(1, entry.getKey());
		    		st.setInt(2, maxValueInMap);
		    		st.setDouble(3, (double)freqMap.get(entry.getKey())*100.0/(double)totalWords);
		    		int count = st.executeUpdate(); //executes the insert and returns an int for rows affected
		    		keyToRemove = entry.getKey(); //sets the String for removal outside for loop
		    		rowAffected += count; //tally the row affected to be printed later
		        }
		    }
			freqMap.remove(keyToRemove); //removes the key so that Collections.max() can find the 2nd max key 
		}
	    System.out.println(rowAffected + " row/s affected");
		st.close();
		con.close();
	}
    /**
	* Splits the html into tokens which can be iterated through
	* The punctuation and spaces are removed as well as every word is lowercased
	* Then a key value is given of 1 if none are found, otherwise added to current number of occurances.
	* 
	* @param freqMap Map storing the word and its current tally of occurances
	* @param tokens The html split up into string tokens from the Jsoup Document 
	* 
	*/
    public static TreeMap<String, Integer> generateFrequencyList(Document doc) throws IOException {
	    TreeMap<String, Integer> freqMap = new TreeMap<String, Integer>(); //A Treemap for word storage: ("the_word , frequency")
		String [] tokens = doc.text().split("\\s+"); //split entire .HTML text into tokens in a String array based on spaces
		
		for (String token : tokens) { //iterates through the entire array of words
			token = token.replaceAll("[^a-zA-Z]", ""); //removes all punctuation
			token = token.toLowerCase(); //lowercase all tokens
			if (!freqMap.containsKey(token)) {
				freqMap.put(token, 1); //if a word is occurring for the first time it will be given a 1 frequency
			} else {
				int count = freqMap.get(token); //gets the current frequency
				freqMap.put(token, count + 1); //adds one to the current frequency
			}
		}
		return freqMap;
	  }
}
