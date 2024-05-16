package ProjectTwo;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * The CompressScene class extends Scene and is used to create a user interface
 * for compressing files using Huffman Coding. It integrates file I/O, Huffman
 * tree construction, and visual display of compression statistics and settings.
 */
public class CompressScene extends Scene {

	int[] freq = new int[256]; // Frequency array for each byte value (0-255)
	File file; // File object for the file to be compressed
	String fileName; // Name of the file without extension
	Heap heap; // Min-heap for building the Huffman tree
	byte numberOfLeafs; // Number of leaf nodes in the Huffman tree

	String fullHeader; // Complete header information as a binary string
	Node[] nodes = new Node[256]; // Array to store nodes corresponding to byte values

	Node rootNode; // Root node of the Huffman tree

	BorderPane bp = new BorderPane(); // Main layout pane for the scene

	long lengthBefore; // File size before compression
	long lengthAfter; // File size after compression

	byte extLength; // Length of the file extension
	String extString; // String of the file extension
	int headerLength; // Length of the header in bits
	String header; // Header data as a binary string

	Stage stage; // Stage on which the scene is set
	Scene scene; // Previous scene to return to
	
	String outFileName; // contains the resulted file name

	/**
	 * Constructor for CompressScene. Sets up the scene and initializes the
	 * compression process.
	 * 
	 * @param stage The primary stage of the application.
	 * @param scene The previous scene to allow returning to it.
	 * @param file  The file to be compressed.
	 */
	public CompressScene(Stage stage, Scene scene, File file) {
		super(new BorderPane(), 1200, 600);
		this.stage = stage;
		this.scene = scene;

		this.bp = ((BorderPane) this.getRoot());

		this.file = file;

		this.lengthBefore = this.file.length();

		getFreq();
		initializeHeap();
		rootNode = heap.remove();
		if (rootNode.getLeft() == null && rootNode.getRight() == null) {
			rootNode.setHuffCode("1");
			rootNode.setHuffLength((byte) 1);
			Byte charCode = rootNode.getCharCode();
			if (charCode < 0)
				nodes[rootNode.getCharCode() + 256] = rootNode;
			else
				nodes[rootNode.getCharCode()] = rootNode;

		} else
			generateHuffmanCodes(rootNode, "", (byte) 0);

		this.fullHeader = generateHeader();

		writeToFile();

		addFX();
	}

	/**
	 * Reads the file and calculates the frequency of each byte.
	 */
	private void getFreq() {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[8]; // Buffer to read chunks of the file

			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				for (int i = 0; i < bytesRead; i++) {
					if (buffer[i] < 0)
						freq[buffer[i] + 256]++;
					else
						freq[buffer[i]]++;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the heap with nodes for each unique byte and builds the Huffman
	 * tree.
	 */
	private void initializeHeap() {
		heap = new Heap(256);
		for (int i = 0; i < freq.length; i++) {
			if (freq[i] != 0) {
				Node node = new Node((byte) i, freq[i]);
				heap.insert(node);
			}
		}

		numberOfLeafs = (byte) heap.getSize();

		if (numberOfLeafs == 1)
			return;

		while (heap.getSize() != 1) {
			Node node1 = heap.remove();
			Node node2 = heap.remove();

			Node node = new Node(node1.getFreq() + node2.getFreq());
			node.setLeft(node1);
			node.setRight(node2);

			heap.insert(node);
		}

	}

	/**
	 * Recursively generates Huffman codes for each node in the tree starting from
	 * the root.
	 * 
	 * @param node   Current node in the tree
	 * @param code   Accumulated Huffman code for the current node
	 * @param length Length of the Huffman code
	 */
	public void generateHuffmanCodes(Node node, String code, byte length) {
		if (node != null) {
			if (node.getLeft() == null && node.getRight() == null) {
				node.setHuffCode(code);
				node.setHuffLength(length);

				Byte charCode = node.getCharCode();
				if (charCode < 0)
					nodes[node.getCharCode() + 256] = node;
				else
					nodes[node.getCharCode()] = node;

			} else {
				generateHuffmanCodes(node.getLeft(), code + "0", (byte) (length + 1));
				generateHuffmanCodes(node.getRight(), code + "1", (byte) (length + 1));
			}
		}
	}

	/**
	 * Generates the header for the compressed file, containing metadata like file
	 * extension and tree structure.
	 * 
	 * @return A string representing the binary form of the header.
	 */
	private String generateHeader() {
		StringBuilder headInfo = new StringBuilder();

		String[] nameInfo = file.getName().split("\\.");

		String ext = nameInfo[1];
		this.extString = ext;
		byte extLength = (byte) ext.length();
		this.extLength = extLength;
		String fileName = nameInfo[0];
		this.fileName = fileName;

		// appending the length and the extension of the original file to the header
		headInfo.append(byteToBinaryString(extLength));
		for (int i = 0; i < extLength; i++)
			headInfo.append(byteToBinaryString((byte) ext.charAt(i)));

		StringBuilder headerBuilder = getHeader(rootNode);

		this.headerLength = headerBuilder.length();

		if (headerLength % 8 != 0)
			for (int i = 0; i < 8 - headerLength % 8; i++)
				headerBuilder.append("0");

		this.header = headerBuilder.toString();

		headInfo.append(byteToBinaryString((byte) (headerLength >> 24)));
		headInfo.append(byteToBinaryString((byte) (headerLength >> 16)));
		headInfo.append(byteToBinaryString((byte) (headerLength >> 8)));
		headInfo.append(byteToBinaryString((byte) (headerLength)));
		headInfo.append(this.header);

		return headInfo.toString();
	}

	public StringBuilder getHeader(Node root) { // post order
		StringBuilder builder = new StringBuilder();
		getHeader(root, builder);
		return builder;
	}

	private void getHeader(Node node, StringBuilder builder) {
		if (node == null)
			return;
		getHeader(node.getLeft(), builder);
		getHeader(node.getRight(), builder);

		if (node.getLeft() == null && node.getRight() == null) {
			byte charCode = node.getCharCode();
			builder.append("1" + byteToBinaryString(charCode));
		} else {
			builder.append("0");
		}
	}

	/**
	 * Converts a byte into a binary string of 8 bits.
	 * 
	 * @param b Byte to convert.
	 * @return Binary string representation of the byte.
	 */
	public static String byteToBinaryString(byte b) {
		StringBuilder binaryString = new StringBuilder();
		for (int i = 7; i >= 0; i--) {
			int bit = (b >> i) & 1;
			binaryString.append(bit);
		}
		return binaryString.toString();
	}

	/**
	 * Writes the compressed data, including the header and the Huffman-encoded
	 * content, to a file.
	 */
	private void writeToFile() {
	    // Append ".huff" extension to the original filename and ensure it is unique
	    StringBuilder outFileName = new StringBuilder(fileName + ".huff");
	    getUniquName(outFileName);
	    File outFile = new File(outFileName.toString());
	    
	    this.outFileName = outFileName.toString();

	    try (FileOutputStream out = new FileOutputStream(outFile)) {
	        byte[] bufferIn = new byte[8]; // Buffer to read the original file
	        byte[] bufferOut = new byte[8]; // Buffer to write compressed data
	        byte[] headerBytes = new byte[8]; // Buffer to store header information

	        int byteNumber = 0;

	        // Convert header string bits to bytes and write them to the output file
	        for (byteNumber = 0; byteNumber < fullHeader.length() / 8; byteNumber++) {
	            String byteString = fullHeader.substring(8 * byteNumber, 8 * byteNumber + 8);
	            headerBytes[byteNumber % 8] = (byte) Integer.parseInt(byteString, 2);

	            // Write every 8 bytes to the file
	            if ((byteNumber + 1) % 8 == 0)
	                out.write(headerBytes);
	        }

	        // Write any remaining bytes in the header buffer
	        if (byteNumber % 8 != 0)
	            out.write(headerBytes, 0, byteNumber % 8);

	        // Process the original file's data
	        try (FileInputStream inputStream = new FileInputStream(file)) {
	            int bytesRead;
	            StringBuilder builder = new StringBuilder();

	            // Read and compress the input file byte by byte
	            while ((bytesRead = inputStream.read(bufferIn)) != -1) {
	                for (int i = 0; i < bytesRead; i++) {
	                    byte currByte = bufferIn[i];
	                    // Retrieve and append Huffman codes for each byte
	                    if (currByte < 0)
	                        builder.append(nodes[currByte + 256].getHuffCode());
	                    else
	                        builder.append(nodes[currByte].getHuffCode());

	                    // Write 64 bits at a time to the output file
	                    if (builder.length() >= 64) {
	                        for (int j = 0; j < 8; j++) {
	                            bufferOut[j] = (byte) Integer.parseInt(builder.substring(0, 8), 2);
	                            builder.delete(0, 8);
	                        }
	                        out.write(bufferOut);
	                    }
	                }
	            }

	            // Write any remaining bits to the output file
	            byte length = (byte) builder.length();
	            for (int i = 0; i < length / 8; i++) {
	                bufferOut[i] = (byte) Integer.parseInt(builder.substring(0, 8), 2);
	                builder.delete(0, 8);
	            }
	            out.write(bufferOut, 0, length / 8);

	            // Handle the last few remaining bits by padding them with zeros
	            byte lengthOfRemainingBits = (byte) builder.length();
	            byte[] remain = new byte[2];
	            if (lengthOfRemainingBits != 0) {
	                for (int i = 0; i < 8 - lengthOfRemainingBits; i++)
	                    builder.append("0");

	            	try {
		                remain[0] = (byte) Integer.parseInt(builder.substring(0, 8), 2);
		                remain[1] = (byte) (8 - lengthOfRemainingBits);
	            	}
	            	catch (Exception e) {
						// TODO: handle exception
					}

	            } else {
	            	try {
		                remain[0] = (byte) Integer.parseInt(builder.substring(0, 8), 2);
		                remain[1] = (byte) 0;
	            	}
	            	catch (Exception e) {
						// TODO: handle exception
					}
	            }
	            out.write(remain, 0, 2);

	            // Close the output file stream
	            out.close();

	            // Store the final size of the compressed file
	            lengthAfter = outFile.length();

	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public void getUniquName(StringBuilder fileName) {
		// Create a File object based on the input file name.
		File file = new File(fileName.toString());
		// Initialize a counter and a flag for the while loop.
		int number = 1, flag = 0;
		// Loop to check if the file exists and modify the file name accordingly.
		while (file.exists()) {
			int lastDotIndex;
			if (flag == 0) {
				// Find the last dot (.) position to locate the extension.
				lastDotIndex = fileName.lastIndexOf(".");
				// Insert a number before the extension for the first time.
				fileName.insert(lastDotIndex, "(" + (number++) + ")");
			} else {
				// For subsequent iterations, remove the old number and add a new one.
				int startIndex = fileName.lastIndexOf("(");
				int endIndex = fileName.lastIndexOf(")") + 1;
				fileName.delete(startIndex, endIndex);
				lastDotIndex = fileName.lastIndexOf(".");
				fileName.insert(lastDotIndex, "(" + (number++) + ")");
			}
			// Update the file object with the new file name.
			file = new File(fileName.toString());
			// Set flag to 1 to indicate that the file name has been modified at least once.
			flag = 1;
		}
	}

	/**
	 * Creates and returns a TableView populated with Huffman coding data for each
	 * character in the file.
	 * 
	 * @return A fully initialized TableView with Huffman data.
	 */
	private TableView<NodeModel> getTable() {
	    TableView<NodeModel> table = new TableView<>();
	    ObservableList<NodeModel> data = FXCollections.observableArrayList();

	    // Populate the observable list with node data for display in the table
	    for (Node node : nodes) {
	        if (node != null) {
	            data.add(new NodeModel(node.getCharCode(), node.getFreq(), node.getHuffCode(), node.getHuffLength()));
	        }
	    }

	    // Set up table columns for character, frequency, Huffman code, code length, and ASCII value
	    TableColumn<NodeModel, String> charColumn = new TableColumn<>("Character");
	    charColumn.setCellValueFactory(new PropertyValueFactory<>("charDisplay"));
	    charColumn.setPrefWidth(120);

	    TableColumn<NodeModel, Number> freqColumn = new TableColumn<>("Frequency");
	    freqColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
	    freqColumn.setPrefWidth(120);

	    TableColumn<NodeModel, String> codeColumn = new TableColumn<>("Huffman Code");
	    codeColumn.setCellValueFactory(new PropertyValueFactory<>("huffCode"));
	    codeColumn.setPrefWidth(150);

	    TableColumn<NodeModel, Number> lengthColumn = new TableColumn<>("Code Length");
	    lengthColumn.setCellValueFactory(new PropertyValueFactory<>("huffLength"));
	    lengthColumn.setPrefWidth(120);

	    TableColumn<NodeModel, Number> asciiColumn = new TableColumn<>("ASCII Value");
	    asciiColumn.setCellValueFactory(new PropertyValueFactory<>("asciiValue"));
	    asciiColumn.setPrefWidth(120); // Set preferred width for ASCII column

	    // Style settings for table columns to enhance readability
	    charColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 14px;");
	    freqColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 14px;");
	    codeColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 14px;");
	    lengthColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 14px;");
	    asciiColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 14px;"); // Style for ASCII column

	    // Apply custom row factory to adjust row styling dynamically
	    table.setRowFactory(tv -> new TableRow<NodeModel>() {
	        @Override
	        protected void updateItem(NodeModel item, boolean empty) {
	            super.updateItem(item, empty);
	            setStyle(item == null || empty ? "" : "-fx-font-weight: bold; -fx-font-size: 14px;");
	        }
	    });

	    // Add all columns to the table
	    table.getColumns().add(charColumn);
	    table.getColumns().add(asciiColumn);
	    table.getColumns().add(freqColumn);
	    table.getColumns().add(codeColumn);
	    table.getColumns().add(lengthColumn);
	    table.setItems(data);
	    table.setStyle("-fx-border-color: black; -fx-border-radius: 10; -fx-background-radius: 10;");

	    return table;
	}


	/**
	 * Creates and returns a Pane displaying the compression percentage.
	 * 
	 * @return A Pane with visual representation of the compression ratio.
	 */
	private Pane getPercentagePane() {
		double percentage = ((double) lengthAfter / lengthBefore);
		String percentageText = String.format("%.4f%%", percentage * 100);;
		if (percentage > 1) {
			percentage = 1;
			percentageText = "More than " + String.format("%.4f%%", percentage * 100);
		}

		Text percentageDisplay = new Text(percentageText);
		percentageDisplay.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.REGULAR, 20));
		percentageDisplay.setFill(Color.BLACK);

		double paneWidth = 200;
		double paneHeight = 50;

		Rectangle outerRectangle = new Rectangle(0, 0, paneWidth, paneHeight);
		outerRectangle.setFill(Color.LIGHTGRAY);
		outerRectangle.setArcWidth(20);
		outerRectangle.setArcHeight(20);
		outerRectangle.setStroke(Color.web("#FFF5E0"));
		outerRectangle.setStrokeWidth(3);

		Rectangle filledRectangle = new Rectangle(0, 0, paneWidth * percentage, paneHeight);
		if (percentage >= 1)
			filledRectangle.setFill(Color.web("#C40C0C"));
		else
			filledRectangle.setFill(Color.web("#90D26D"));
		filledRectangle.setArcWidth(20);
		filledRectangle.setArcHeight(20);
		filledRectangle.setClip(new Rectangle(0, 0, paneWidth * percentage, paneHeight));

		percentageDisplay.setLayoutX((paneWidth - percentageDisplay.getBoundsInLocal().getWidth()) / 2);
		percentageDisplay.setLayoutY((paneHeight + percentageDisplay.getBoundsInLocal().getHeight()) / 2);

		Pane pane = new Pane();
		pane.setPrefSize(paneWidth, paneHeight);
		pane.getChildren().addAll(outerRectangle, filledRectangle, percentageDisplay);
		pane.setMinWidth(Region.USE_PREF_SIZE);
		pane.setMaxWidth(Region.USE_PREF_SIZE);

		return pane;
	}

	/**
	 * Opens a directory using the system's default file explorer.
	 * 
	 * @param dir The directory path to open.
	 */
	private void openDirectory(String dir) {
		try {
			Desktop.getDesktop().open(new File(dir));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Constructs and returns a detailed scene containing header information.
	 * 
	 * @return A Scene displaying detailed header data.
	 */
	private Scene getHeaderScene() {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(20);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 10, 10, 10));

		Label extensionLengthLabel = new Label("Extension Length");
		extensionLengthLabel.setStyle("-fx-text-fill: #FFF5E0; -fx-padding: 2; -fx-font-size: 22px;");
		Label fileExtensionLabel = new Label("File Extension");
		fileExtensionLabel.setStyle("-fx-text-fill: #FFF5E0; -fx-padding: 2; -fx-font-size: 22px;");
		Label headerLengthLabel = new Label("Header Length");
		headerLengthLabel.setStyle("-fx-text-fill: #FFF5E0; -fx-padding: 2; -fx-font-size: 22px;");
		Label headerLabel = new Label("Header");
		headerLabel.setStyle("-fx-text-fill: #FFF5E0; -fx-padding: 2; -fx-font-size: 22px;");

		Label extensionLengthValue = new Label(extLength + " Byte");
		extensionLengthValue.setStyle(
				"-fx-text-fill: #141E46; -fx-background-color: white; -fx-padding: 10; -fx-border-color: #41B06E; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 16px;");
		extensionLengthValue.setMinWidth(200);

		Label fileExtensionValue = new Label("." + extString);
		fileExtensionValue.setStyle(
				"-fx-text-fill: #141E46; -fx-background-color: white; -fx-padding: 10; -fx-border-color: #41B06E; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 16px;");
		fileExtensionValue.setMinWidth(200);

		Label headerLengthValue = new Label(this.headerLength + " Bits");
		headerLengthValue.setStyle(
				"-fx-text-fill: #141E46; -fx-background-color: white; -fx-padding: 10; -fx-border-color: #41B06E; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 16px;");
		headerLengthValue.setMinWidth(200);

		TextArea headerTextArea = new TextArea(this.header);
		headerTextArea.setWrapText(true);
		headerTextArea.setEditable(false);
		headerTextArea.setStyle(
				"-fx-text-fill: #141E46; -fx-background-color: white; -fx-padding: 10; -fx-border-color: #41B06E; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 16px;");

		grid.add(extensionLengthLabel, 0, 0);
		grid.add(extensionLengthValue, 1, 0);
		grid.add(fileExtensionLabel, 0, 1);
		grid.add(fileExtensionValue, 1, 1);
		grid.add(headerLengthLabel, 0, 2);
		grid.add(headerLengthValue, 1, 2);
		grid.add(headerLabel, 0, 3);
		grid.add(headerTextArea, 1, 3);

		return new Scene(grid, 850, 450);
	}

	/**
	 * Adds interactive and visual elements to the compression scene, including
	 * statistics and controls.
	 */
	private void addFX() {
		Label huffTableLabel = new Label("Huffman Table");
		huffTableLabel.setStyle("-fx-text-fill: #FFF5E0; " + "-fx-padding: 2; " + "-fx-font-size: 22px;");

		Label fileName = new Label("File Name : " + outFileName);
		fileName.setStyle("-fx-text-fill: #FFF5E0; " + "-fx-padding: 2; " + "-fx-font-size: 22px;");

		
		VBox tableBox = new VBox(10, huffTableLabel, getTable(), fileName);
		tableBox.setAlignment(Pos.CENTER);
		bp.setPadding(new Insets(15));

		BorderPane.setMargin(tableBox, new Insets(0, 0, 0, 100));

		bp.setLeft(tableBox);

		// Dynamic content resizing and style application
		double maxWidth = Math.max(new Text("Size Before : " + lengthBefore + " Byte").getLayoutBounds().getWidth(),
				new Text("Size After : " + lengthAfter + " Byte").getLayoutBounds().getWidth());
		maxWidth += 100; // Additional padding for aesthetics

		Label beforeLabel = new Label("Size Before : " + lengthBefore + " Byte");
		beforeLabel.setStyle("-fx-text-fill: #141E46; " + "-fx-background-color: white; " + "-fx-padding: 10; "
				+ "-fx-border-color: #41B06E; " + "-fx-border-radius: 5; " + "-fx-background-radius: 5; "
				+ "-fx-font-size: 16px;");
		beforeLabel.setMinWidth(maxWidth);

		Label afterLabel = new Label("Size After : " + lengthAfter + " Byte");
		afterLabel.setStyle("-fx-text-fill: #141E46; " + "-fx-background-color: white; " + "-fx-padding: 10; "
				+ "-fx-border-color: #41B06E; " + "-fx-border-radius: 5; " + "-fx-background-radius: 5; "
				+ "-fx-font-size: 16px;");
		afterLabel.setMinWidth(maxWidth);

		beforeLabel.setAlignment(Pos.CENTER);
		afterLabel.setAlignment(Pos.CENTER);

		Button openDirectoryButton = new Button("Open File Directory");
		openDirectoryButton.setOnAction(e -> openDirectory(System.getProperty("user.dir")));
		openDirectoryButton.setMaxWidth(Double.MAX_VALUE);

		Button headerButton = new Button("Header Information");
		headerButton.setOnAction(e -> {
			Stage headerStage = new Stage();
			Scene headerScene = getHeaderScene();
			headerScene.getStylesheets().add("LightMode.css");
			headerStage.setScene(headerScene);
			headerStage.setTitle("Header Information");

			headerStage.show();

		});
		headerButton.setMaxWidth(Double.MAX_VALUE);

		Button backButton = new Button("Back");
		backButton.setOnAction(e -> {
			stage.setScene(scene);
		});
		backButton.setMaxWidth(Double.MAX_VALUE);

		Label compRateLabel = new Label("Compression Rate");
		compRateLabel.setStyle("-fx-text-fill: #FFF5E0; " + "-fx-padding: 2; " + "-fx-font-size: 22px;");
		compRateLabel.setAlignment(Pos.CENTER);

		VBox percentageBox = new VBox(10, compRateLabel, getPercentagePane(), beforeLabel, afterLabel);
		percentageBox.setAlignment(Pos.CENTER);

		VBox rightBox = new VBox(20, percentageBox, openDirectoryButton, headerButton, backButton);
		rightBox.setAlignment(Pos.CENTER);
		BorderPane.setMargin(rightBox, new Insets(0, 150, 0, 0));

		bp.setRight(rightBox);

	}

}
