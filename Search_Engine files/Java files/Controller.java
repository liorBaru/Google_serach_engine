
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.*;
import sample.*;

import java.io.*;
import java.util.*;

public class Controller {

    //<editor-fold desc="GUI Objects">
        //<editor-fold desc="part_A objects">
        public TextField text_Field_corpus_stop_word_path;
        public TextField text_Field_Dictionary_Posting_Path;
        public Button button_Browse_Corpus_Stop_Word;
        public Button button_Browse_Dictionary_Posting;
        public Button button_Load_Dictionary;
        public Button button_Clear_Index;
        public Button button_Show_Dictionary;
        public Button button_Start;
        public ScrollPane scrollPane_Dictionary_Display;
        public CheckBox checkBox_stemmer;
        public GridPane grid;
        //</editor-fold>
        //<editor-fold desc="part_B objects">
        public Button button_run;
        public Button button_save_results;
        public Button browse_button_query_file;
        public TextField text_field_query_file;
        public TextField text_field_query;
        public CheckBox check_box_entity_search;
        public CheckBox check_box_semantic_search;
        public TextField text_area_save_results;
        public TextArea textArea;
        //</editor-fold>
        public Button browse_results_button = new Button();
    //</editor-fold>
    //<editor-fold desc="Retrieval Engine Objects">
    ReadFile readfile;
    Parse parser;
    Stemmer stemmer;
    Indexer indexer;
    Searcher searcher;
    //</editor-fold>

    public Controller() {
        //<editor-fold desc="init GUI Objects">
            //<editor-fold desc="part_A">
        text_Field_corpus_stop_word_path = new TextField();
        text_Field_Dictionary_Posting_Path= new TextField();
        button_Browse_Corpus_Stop_Word = new Button();
        button_Browse_Dictionary_Posting = new Button();
        button_Load_Dictionary = new Button();
        button_Clear_Index = new Button();
        button_Show_Dictionary = new Button();
        button_Start = new Button();
        scrollPane_Dictionary_Display = new ScrollPane();
        checkBox_stemmer = new CheckBox();
        grid = new GridPane();
        //</editor-fold>
            //<editor-fold desc="part_B">
            button_run = new Button();
            button_save_results = new Button();
            browse_button_query_file = new Button();
            text_field_query_file = new TextField();
            text_field_query = new TextField();
            check_box_entity_search = new CheckBox();
            check_box_semantic_search = new CheckBox();
            text_area_save_results = new TextField("enter path here");
            //</editor-fold>
        //</editor-fold>
        //<editor-fold desc="init Engine Objects">
        readfile = new ReadFile();
        parser = new Parse();
        stemmer = new Stemmer();
        indexer = new Indexer();
        searcher = new Searcher(indexer,parser);
        //</editor-fold>
    }
    public void initialize() {
        //<editor-fold desc="Part_A Set onAction Functions">
            //<editor-fold desc="Set button_Browse_Corpus_Stop_Word">
        button_Browse_Corpus_Stop_Word.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                text_Field_corpus_stop_word_path.setText(openAndChooseDir());
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_Browse_Dictionary_Posting">
        button_Browse_Dictionary_Posting.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                text_Field_Dictionary_Posting_Path.setText(openAndChooseDir());
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_Clear_Index">
        button_Clear_Index.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("clearing posting files...");
                if(indexer.clearPostingDirectory()) {
                    System.out.println("posting files cleared successfully");
                }else {
                    System.out.println("clearing posting files failed");
                }
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_Show_Dictionary">
        button_Show_Dictionary.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try{
                    indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
                    ArrayList<String> dictionaryList = indexer.dictionaryToString();
                    ArrayList<Text> texts = new ArrayList<>();
                    StringBuilder text =new StringBuilder();
                    for (String line : dictionaryList) {
                        texts.add(new Text(line));
                        text.append(line);
                    }
                    Stage stage = new Stage();
                    Scene scene;
                    TextArea textArea = new TextArea();
                    textArea.setText(text.toString());
                    //aboutPane.setContent(texts);
                    grid.add(textArea, 0,10);
                }catch(Exception e){System.out.println(e);}
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_Start">
        button_Start.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * method will build an index for the given corpus.
             * the indexing will include reading,parsing,posting and indexing.
             * resource heavy method.
             * @param event
             */
            @Override
            public void handle(ActionEvent event) {
                ArrayList<Double> times= new ArrayList<Double>();
                indexer.setWithStemming(checkBox_stemmer.isSelected());
                parser.stemmer.setEnable(checkBox_stemmer.isSelected());
                indexer.clear();
                indexer.clearDictionary();
                File FileInNewLoaction=null;
                if(checkBox_stemmer.isSelected()) {
                    File file = new File(text_Field_Dictionary_Posting_Path.getText() + "\\withStemming");
                    FileInNewLoaction=new File(text_Field_Dictionary_Posting_Path.getText() + "\\withStemming\\stop_words.txt");
                    if (file == null || !file.isDirectory())
                        file.mkdir();
                }
                else
                    FileInNewLoaction=new File(text_Field_Dictionary_Posting_Path.getText()+"\\stop_words.txt");
                indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
                indexer.clearPostingDirectory();
                String dirPath = text_Field_corpus_stop_word_path.getText();
                readfile.init(dirPath);
                String docText;
                File stopWords = new File(text_Field_Dictionary_Posting_Path.getText()+"\\stop_words.txt");
                if(stopWords==null) {
                    File originalStopWords = new File(dirPath + "\\stop_words.txt");
                    try {
                        copyFile(originalStopWords, FileInNewLoaction);
                    } catch (Exception e) {
                        System.out.println("could not copy stop words to posting directory");
                    }
                    stopWords = FileInNewLoaction;
                }
                parser.loadStopWords(stopWords.getPath());
                indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
                int counter=0;
                while((docText = readfile.nextDocument())!=null) {
                    DocumentObj doc = DocumentObj.createDocument(docText);
                    if (doc != null&&doc.getText()!=null&&!doc.getText().equals("")) {
                        doc = parser.ParseDoc(doc);
                        indexer.addToCollection(doc);
                        indexer.setDictionery();
                        indexer.stackRecords();
                    }
                    counter++;
                }
                indexer.createPosting();
                indexer.sortPosting();
                indexer.saveDictionary();
                indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
                indexer.loadDicitonary();
                System.out.println("done indexing");
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_Load_Dictionary">
        button_Load_Dictionary.setOnAction(event -> {
            indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
            indexer.loadDicitonary();
        });
        //</editor-fold>
            //<editor-fold desc="Set checkBox_stemmer">
        checkBox_stemmer.setOnAction(event -> {
            stemmer.setEnable(checkBox_stemmer.isSelected());
            indexer.setWithStemming(checkBox_stemmer.isSelected());
            if(checkBox_stemmer.isSelected())
                System.out.println("stemmer is enabled");
            else
                System.out.println("stemmer is disabled");
        });
        //</editor-fold>
        //</editor-fold>
        //<editor-fold desc="Part_B Set onAction Functions">
            //<editor-fold desc="Set button_run">
        button_run.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * method will read the query from the text field and from the given file path
             * and send them to a searcher for processing. will show results in TextArea
             * @param event
             */
            @Override
            public void handle(ActionEvent event) {
                textArea = new TextArea("running....");
                parser.loadStopWords(text_Field_corpus_stop_word_path.getText()+"\\stop_words.txt");
                Hashtable<Integer,String> queryInfo = new Hashtable<>();
                ArrayList<String> queries = new ArrayList<>();
                if (!text_field_query.getText().equals("")) {
                    queryInfo.put(1, text_field_query.getText());
                    queries.add(text_field_query.getText());
                }
                String path = text_field_query_file.getText();
                if(!path.equals("")) {
                    try {
                        Integer queryID = 1;
                        String line = "";
                        StringBuilder sb = new StringBuilder();
                        BufferedReader br = new BufferedReader(
                                new FileReader(
                                        new File(path)));
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("<num>"))
                                queryID = Integer.parseInt(line.replace("<num> Number: ", "").replace(" ", ""));
                            if (line.startsWith("<title>")) {
                                sb.append(line.replace("<title> ", "")+" ");
                            }
                            if(line.startsWith("<desc>")) {
                                while (!(line = br.readLine()).startsWith("<narr>")) {
                                    sb.append(line+" ");
                                }
                            }
                            if(line.startsWith("<narr>")) {
                                while (!(line = br.readLine()).startsWith("</top>")) {
                                    sb.append(line+" ");
                                }
                                queryInfo.put(queryID,sb.toString());
                                queries.add(sb.toString());
                                sb = new StringBuilder();
                            }
                        }
                        br.close();
                    } catch (Exception e) {
                        System.out.println("could not load query file");
                    }
                }
                ArrayList<String> output = new ArrayList<>();
                if(queryInfo.keySet().size()!=0) {

                    indexer.setPostingPath(text_Field_Dictionary_Posting_Path.getText());
                    indexer.loadDicitonary();
                    Hashtable<String, ArrayList<String>> results = searcher.processQueries(queries);
                    Set<Integer> queryIDs = queryInfo.keySet();
                    ArrayList<Integer> IDs = new ArrayList<>();
                    for (Integer id : queryIDs) {
                        IDs.add(id);
                    }
                    IDs.sort((o1, o2) -> o1.compareTo(o2));
                    for (Integer ID : IDs) {
                        String query = queryInfo.get(ID);
                        ArrayList<String> docs = results.get(query);
                        if (docs != null) {
                            for (String doc : docs) {
                                String docNo = doc.split(" ")[0];
                                String score = doc.split(" ")[1].split(",")[0];
                                String[] items = doc.replace(docNo + " ", "").replace(score + ",", "").split(",");
                                StringBuilder sb = new StringBuilder();
                                sb.append(ID + " 0 " + docNo + " 0 " + score + " LH ");
                                if (check_box_entity_search.selectedProperty().getValue()) {
                                    for (int i = 0; i < items.length; i++) {
                                        sb.append("," + items[i]);
                                    }
                                }
                                output.add(sb.toString());
                            }
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (String result:output) {
                    sb.append(result+"\n");
                }
                textArea = new TextArea();
                textArea.setEditable(false);
                String text = sb.toString();
                if(text.length()==0)
                    textArea.setText("no results");
                else
                    textArea.setText(text);
                grid.add(textArea, 0,10);
                //searcher.print();
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set button_save_results">
        button_save_results.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * writes text from the textArea to a file in the given path
             * @param event
             */
            @Override
            public void handle(ActionEvent event) {
                if(!textArea.getText().equals(""))
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(text_area_save_results.getText()+"\\results")));
                    bw.write(textArea.getText());
                    bw.close();
                }catch(Exception e){

                }
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set browse_button_query_file">
        browse_button_query_file.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                  text_field_query_file.setText(openAndChooseFile());
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set check_box_entity_search">
        check_box_entity_search.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(check_box_semantic_search.selectedProperty().getValue()) {
                    check_box_semantic_search.selectedProperty().setValue(false);
                    searcher.setDoSemantic(false);
                }
                searcher.setDoEntities(check_box_entity_search.selectedProperty().getValue());
            }
        });
        //</editor-fold>
            //<editor-fold desc="Set check_box_semantic_search">
        check_box_semantic_search.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(check_box_entity_search.selectedProperty().getValue()) {
                    check_box_entity_search.selectedProperty().setValue(false);
                    searcher.setDoEntities(false);
                }
                searcher.setDoSemantic(check_box_semantic_search.selectedProperty().getValue());

            }
        });
        //</editor-fold>
            //<editor-fold desc="Set browse_results_button">
        browse_results_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                text_area_save_results.setText(openAndChooseDir());
            }
        });
        //</editor-fold>
        //</editor-fold>
    }
    //<editor-fold desc="browse directory window">
    /**
     * helper function for opening a browse window for choosing a directory
     * @return
     */
        private String openAndChooseDir() {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("choose directory");
            File selectedDirectory = chooser.showDialog(Main.PrimaryStage);
            if(selectedDirectory==null)
                return "no path selected";
            return selectedDirectory.getPath();
        }
        //</editor-fold>
    //<editor-fold desc="browse file window">

    /**
     * helper function for opening a browse window for choosing a file
     * @return
     */
    private String openAndChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("choose file");
        File selectedFile = chooser.showOpenDialog(Main.PrimaryStage);
        if(selectedFile==null)
            return "no path selected";
        return selectedFile.getPath();
    }
    //</editor-fold>
    //<editor-fold desc="copy file">
    private void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    //</editor-fold>
}
